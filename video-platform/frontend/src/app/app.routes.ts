import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './shared/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./videos/video-list/video-list.component').then((m) => m.VideoListComponent),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'upload',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./videos/video-upload/video-upload.component').then((m) => m.VideoUploadComponent),
  },
  {
    path: 'my-videos',
    canActivate: [authGuard],
    loadComponent: () => import('./videos/my-videos/my-videos.component').then((m) => m.MyVideosComponent),
  },
  {
    path: 'videos/:id',
    loadComponent: () =>
      import('./videos/video-detail/video-detail.component').then((m) => m.VideoDetailComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
