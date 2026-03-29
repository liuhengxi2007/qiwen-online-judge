Page name: Sign-in page
Path: `/login`
File: `src/pages/LoginPage.tsx`
Purpose:
- Provide the primary authentication entry for Qiwen Online Judge administrators.
- Accept username and password and connect to the live backend session API.
Main structure:
- Product introduction block
- Demo credential summary
- Sign-in form
- Status feedback area for success handoff, session expiry, sign-out, and request failures
Interaction flow:
- User submits valid credentials and is redirected to `/`
- Invalid credentials stay on the page and render an inline error
- Signed-out and session-expired flows re-enter this page with an explicit notice

Page name: Dashboard page
Path: `/`
File: `src/pages/DashboardPage.tsx`
Purpose:
- Act as the route hub after sign-in.
- Show the current account, available management routes, and route-level notices.
Main structure:
- Account summary header
- Global route notice area
- Current session summary card
- Navigation cards for user settings and site management
- Flow coverage card describing the available closed loops
Interaction flow:
- Session refresh success keeps the user on the dashboard
- Session refresh failure or authentication expiry returns the user to `/login?notice=session-expired`
- Permission denial from deeper routes returns to `/` with a reason-specific notice

Page name: Site management page
Path: `/site-manage`
File: `src/pages/SiteManagePage.tsx`
Purpose:
- Allow site managers to review user records and update permission flags.
Main structure:
- Header with return and sign-out actions
- Route status area for authorization, load failure, and permission update results
- User management table or empty-state panel
Interaction flow:
- Authorized site managers load the full user list
- Empty data renders an explicit empty-state panel instead of a blank table
- Unauthorized access redirects to `/?notice=site-manage-denied`
- Authentication expiry redirects to `/login?notice=session-expired`
- Permission update success renders an inline confirmation message

Page name: User settings page
Path: `/user/:username/settings`
File: `src/pages/UserSettingsPage.tsx`
Purpose:
- Let the signed-in user update profile data and password on a username-scoped route.
Main structure:
- Header with return and sign-out actions
- Route notice area for canonical route correction and auth failures
- Profile settings form
- Permission summary card
Interaction flow:
- Missing or mismatched route usernames are corrected to the canonical signed-in route with a notice
- Save success stays on the page and renders a success message
- Wrong current password or backend failures stay on the page and render an inline error
- Authentication expiry redirects to `/login?notice=session-expired`

Closed-loop checks:
- Every protected page now has an explicit return path to the dashboard.
- Auth expiry returns to the login page with a concrete reason.
- Permission denial returns to the dashboard with a concrete reason.
- Empty user-list results are handled explicitly on the site management page.
