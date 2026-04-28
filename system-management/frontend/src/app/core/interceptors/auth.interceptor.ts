import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);

  // The access_token is an HttpOnly cookie — the browser attaches it automatically.
  // withCredentials: true tells the browser to include cookies on cross-origin requests.
  const outgoing = req.clone({ withCredentials: true });

  return next(outgoing).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/')) {
        if (!isRefreshing) {
          isRefreshing = true;
          return authService.refresh().pipe(
            switchMap(() => {
              isRefreshing = false;
              // The refresh response set a new access_token cookie via Set-Cookie.
              // The browser will include it automatically on the retried request.
              return next(req.clone({ withCredentials: true }));
            }),
            catchError(refreshError => {
              isRefreshing = false;
              authService.clearAndRedirect();
              return throwError(() => refreshError);
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
