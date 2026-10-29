import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Functional HTTP Interceptor in Angular.
 * Automatically attaches the Authorization header with Bearer token
 * to all outgoing API requests if a session token is present.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('bankease_token');
  if (token) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authReq);
  }
  return next(req);
};
