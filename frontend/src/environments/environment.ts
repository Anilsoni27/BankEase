// When running locally on Angular dev server (port 4200), target the backend at http://localhost:8080/api
// In production or when served directly from Spring Boot, use relative '/api' so it works on any domain/port/HTTPS automatically
const isDevServer = typeof window !== 'undefined' && window.location.port === '4200';

export const environment = {
  production: !isDevServer,
  apiUrl: isDevServer ? 'http://localhost:8080/api' : '/api'
};
