# Notes

- Google uses the existing direct popup flow, not a redirect/callback flow.
- Native login in this slice is only a development transport validation. It is
  not a production password-authentication implementation.
- The Google ID token is kept only in the Studio JavaScript module memory and
  is removed on sign-out. It is added to HTTP API requests through the common
  request client; no browser storage or refresh token is introduced.
- The native validation endpoint is unavailable outside `meshingress.security.mode=dev`.
- Verification: `npm run build`, `npm run lint` (one unrelated existing Fast
  Refresh warning), and focused server reactor tests passed.
