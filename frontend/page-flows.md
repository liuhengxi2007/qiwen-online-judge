# Frontend Page Flows

This document tracks the main user-facing routes in the current `frontend/src/features/<domain>` structure.

## App Shell

Page name: Dashboard page
Path: `/`
File: `src/features/dashboard/pages/DashboardPage.tsx`
Purpose:
- Act as the signed-in route hub.
- Link to problems, problem sets, submissions, blogs, user groups, site management, and user profile flows.
Interaction flow:
- Authenticated users see the dashboard and global authenticated user bar.
- Guests are redirected to `/login`.

Page name: Authenticated user bar
Scope: protected routes
File: `src/shared/components/authenticated-user-bar.tsx`
Purpose:
- Keep the current display name and logout action visible on all authenticated pages.
- Link the current user to their profile page.

## Auth And Users

Page name: Sign-in page
Path: `/login`
File: `src/features/auth/pages/LoginPage.tsx`
Purpose:
- Accept username and password.
- Create a backend session and redirect to `/`.

Page name: Register page
Path: `/register`
File: `src/features/auth/pages/RegisterPage.tsx`
Purpose:
- Create a new user account.

Page name: User profile page
Path: `/user/:username`
File: `src/features/auth/pages/UserProfilePage.tsx`
Purpose:
- Show public user identity information.
- Link to that user's submissions and blogs.
- Show the settings button only when viewing the current user's own profile.

Page name: User settings page
Path: `/user/:username/settings`
File: `src/features/auth/pages/UserSettingsPage.tsx`
Purpose:
- Let the current user update profile fields and password.

Page name: Site management page
Path: `/site-manage`
File: `src/features/site-management/pages/SiteManagePage.tsx`
Purpose:
- Let site managers inspect users and update permission flags.

## Problems And Problem Sets

Page name: Problem list page
Path: `/problems`
File: `src/features/problem/pages/ProblemPage.tsx`
Purpose:
- List visible problems and entry points for problem creation.

Page name: Create problem page
Path: `/problems/new`
File: `src/features/problem/pages/CreateProblemPage.tsx`
Purpose:
- Create a problem with slug, title, statement, limits, access policy, and others-submission access settings.

Page name: Problem detail page
Path: `/problems/:slug`
File: `src/features/problem/pages/ProblemDetailPage.tsx`
Purpose:
- Show problem statement and metadata.
- Link to submit code, problem submissions, problem blogs, and data management.
- Let problem managers open edit and access-management panels.

Page name: Problem data page
Path: `/problems/:slug/data`
File: `src/features/problem/pages/ProblemDataPage.tsx`
Purpose:
- Let problem managers upload, list, download, delete, and clear problem data files.

Page name: Problem submit page
Path: `/problems/:slug/submit`
File: `src/features/problem/pages/ProblemSubmitPage.tsx`
Purpose:
- Let users with access submit source code for a problem.
- Redirect to the created submission detail page after success.

Page name: Problem set list page
Path: `/problem-sets`
File: `src/features/problemset/pages/ProblemSetPage.tsx`
Purpose:
- List visible problem sets.

Page name: Problem set detail page
Path: `/problem-sets/:slug`
File: `src/features/problemset/pages/ProblemSetDetailPage.tsx`
Purpose:
- Show problem set content and linked problems.
- Let authorized managers edit content, manage access, link problems, and remove linked problems.

## Submissions And Judging

Page name: Global submissions page
Path: `/submissions`
File: `src/features/submission/pages/SubmissionPage.tsx`
Purpose:
- List visible submissions with verdict, problem title, submitter display name, execution time, space, code length, and submission time.
- Support filtering, sorting, reverse order, and pagination.

Page name: User submissions page
Path: `/submission/:username`
File: `src/features/submission/pages/SubmissionPage.tsx`
Purpose:
- Show submissions for one user using the same display logic as the global list, without the username filter box.

Page name: Problem submissions page
Path: `/problems/:slug/submissions`
File: `src/features/submission/pages/SubmissionPage.tsx`
Purpose:
- Show submissions for one problem using the same display logic as the global list, without the problem filter box.

Page name: Submission detail page
Path: `/submissions/:submissionId`
File: `src/features/submission/pages/SubmissionDetailPage.tsx`
Purpose:
- Show one submission's metadata, verdict, runtime metrics, and source code.

## Blogs

Page name: Global blogs page
Path: `/blogs`
File: `src/features/blog/pages/BlogPage.tsx`
Purpose:
- List visible blogs by time.
- Show title, visibility, blog type, linked problem when present, author, score, and creation time.

Page name: Create blog page
Path: `/blogs/new`
File: `src/features/blog/pages/CreateBlogPage.tsx`
Purpose:
- Create a public/private blog.
- Choose `general` or `problem`; problem blogs require selecting a linked problem.

Page name: Blog detail page
Path: `/blogs/:blogId`
File: `src/features/blog/pages/BlogDetailPage.tsx`
Purpose:
- Show title, content, author, visibility, type, linked problem, score, comments, and replies.
- Let authors edit/delete their own blogs and comments.
- Let viewers like/dislike blogs and comments.

Page name: User blogs page
Path: `/blog/:username`
File: `src/features/blog/pages/BlogPage.tsx`
Purpose:
- Show visible blogs published by one user.

Page name: Problem blogs page
Path: `/problems/:slug/blogs`
File: `src/features/blog/pages/BlogPage.tsx`
Purpose:
- Show visible problem-type blogs linked to one problem.
- Keep those same blogs visible in global and user blog lists.

## User Groups

Page name: User group list page
Path: `/user-groups`
File: `src/features/usergroup/pages/UserGroupPage.tsx`
Purpose:
- List user groups visible to the current user.

Page name: Create user group page
Path: `/user-groups/new`
File: `src/features/usergroup/pages/CreateUserGroupPage.tsx`
Purpose:
- Create a user group.

Page name: User group detail page
Path: `/user-groups/:slug`
File: `src/features/usergroup/pages/UserGroupDetailPage.tsx`
Purpose:
- Show members and group metadata.
- Let authorized users manage members, roles, ownership, and deletion.
