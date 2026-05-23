import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/categories', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'categories',
        loadComponent: () => import('./features/categories/category-list/category-list.component').then(m => m.CategoryListComponent)
      },
      {
        path: 'categories/:id/documents',
        loadComponent: () => import('./features/documents/document-list/document-list.component').then(m => m.DocumentListComponent)
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/users/user-list.component').then(m => m.UserListComponent)
      },
      {
        path: 'groups',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/groups/group-list/group-list.component').then(m => m.GroupListComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/categories' }
];
