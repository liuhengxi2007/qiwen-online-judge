# Frontend Page Shared Consumers

This inventory records runtime consumers for files kept under
`src/pages/components`, `src/pages/hooks`, `src/pages/objects`,
`src/pages/routing`, and `src/pages/stores`.

Tests are not counted as consumers. A page consumer means the file is reachable
from runtime code under `src/pages/<PageName>`, including through another shared
page component or hook.

## Page Sets

- `P41`: BlogDetailPage, BlogPage, ContestDetailPage, ContestManagePage, ContestPage, ContestRanklistPage, ContestRegistrantPage, ContestSubmissionPage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, CreateUserGroupPage, DashboardPage, ForbiddenPage, HackDetailPage, HackPage, LoginPage, MessageConversationPage, MessageInboxPage, NotificationPage, ProblemBlogPage, ProblemDataPage, ProblemDetailPage, ProblemPage, ProblemSetDetailPage, ProblemSetPage, ProblemSubmissionPage, ProblemSubmitPage, RanklistPage, RatingManagePage, RegisterPage, SiteManagePage, SubmissionDetailPage, SubmissionHackPage, SubmissionPage, UserBlogPage, UserGroupDetailPage, UserGroupPage, UserProfilePage, UserSettingsPage.
- `P40`: `P41` except ForbiddenPage.
- `P38`: BlogDetailPage, BlogPage, ContestDetailPage, ContestManagePage, ContestPage, ContestRanklistPage, ContestRegistrantPage, ContestSubmissionPage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, CreateUserGroupPage, DashboardPage, HackDetailPage, HackPage, MessageConversationPage, MessageInboxPage, NotificationPage, ProblemBlogPage, ProblemDataPage, ProblemDetailPage, ProblemPage, ProblemSetDetailPage, ProblemSetPage, ProblemSubmissionPage, ProblemSubmitPage, RanklistPage, RatingManagePage, SiteManagePage, SubmissionDetailPage, SubmissionHackPage, SubmissionPage, UserBlogPage, UserGroupDetailPage, UserGroupPage, UserProfilePage, UserSettingsPage.
- `P37`: `P38` except DashboardPage.
- `P22`: BlogDetailPage, BlogPage, ContestDetailPage, ContestPage, ContestRanklistPage, ContestRegistrantPage, ContestSubmissionPage, HackDetailPage, HackPage, ProblemBlogPage, ProblemDetailPage, ProblemPage, ProblemSetDetailPage, ProblemSetPage, ProblemSubmissionPage, RanklistPage, RatingManagePage, SubmissionDetailPage, SubmissionHackPage, SubmissionPage, UserBlogPage, UserGroupDetailPage.
- `P16-datetime`: BlogDetailPage, BlogPage, ContestDetailPage, ContestPage, ContestSubmissionPage, HackDetailPage, HackPage, MessageConversationPage, MessageInboxPage, NotificationPage, ProblemBlogPage, ProblemSubmissionPage, SiteManagePage, SubmissionDetailPage, SubmissionPage, UserBlogPage.
- `P16-pagination`: BlogPage, ContestPage, ContestRanklistPage, ContestRegistrantPage, ContestSubmissionPage, HackPage, MessageInboxPage, NotificationPage, ProblemBlogPage, ProblemPage, ProblemSetPage, ProblemSubmissionPage, SiteManagePage, SubmissionPage, UserBlogPage, UserGroupPage.
- `P15-pagination-state`: BlogPage, ContestPage, ContestRanklistPage, ContestRegistrantPage, ContestSubmissionPage, MessageInboxPage, NotificationPage, ProblemBlogPage, ProblemPage, ProblemSetPage, ProblemSubmissionPage, SiteManagePage, SubmissionPage, UserBlogPage, UserGroupPage.
- `P15-problem-title`: BlogDetailPage, BlogPage, ContestDetailPage, ContestSubmissionPage, ProblemBlogPage, ProblemDataPage, ProblemDetailPage, ProblemPage, ProblemSetDetailPage, ProblemSubmissionPage, ProblemSubmitPage, SubmissionDetailPage, SubmissionPage, UserBlogPage, UserProfilePage.
- `P15-access`: BlogDetailPage, BlogPage, ContestDetailPage, ContestManagePage, ContestPage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, ProblemBlogPage, ProblemDetailPage, ProblemPage, ProblemSetDetailPage, ProblemSetPage, UserBlogPage.

## Shared Files

| File | Page consumers |
| --- | --- |
| `src/pages/hooks/usePageTitle.ts` | `P41` |
| `src/pages/routing/NavigationIntent.ts` | `P40` |
| `src/pages/stores/auth/AuthSession.ts` | `P40` |
| `src/pages/stores/auth/AuthSessionStorage.ts` | `P40` |
| `src/pages/stores/auth/UseAuthStore.ts` | `P40`, plus `src/router.tsx` |
| `src/pages/components/AccountActions.tsx` | `P38` |
| `src/pages/components/UserAvatar.tsx` | `P38` |
| `src/pages/hooks/useSessionGuard.ts` | `P38` |
| `src/pages/objects/AppEntryCatalog.ts` | `P38` |
| `src/pages/objects/AppModuleTheme.ts` | `P38` |
| `src/pages/objects/UserDisplayLabel.ts` | `P38` |
| `src/pages/routing/RoutePolicy.ts` | `P38` |
| `src/pages/stores/message/UseMessageStore.ts` | `P38` |
| `src/pages/stores/notification/UseNotificationStore.ts` | `P38` |
| `src/pages/components/AncestorNavigationLinks.ts` | `P37` |
| `src/pages/components/AppSectionBar.tsx` | `P37` |
| `src/pages/components/BreadcrumbNavigation.tsx` | `P37` |
| `src/pages/components/PageShell.tsx` | `P37` |
| `src/pages/components/UserProfileLink.tsx` | `P22` |
| `src/pages/components/DateTimeText.tsx` | `P16-datetime` |
| `src/pages/components/PaginationControls.tsx` | `P16-pagination` |
| `src/pages/hooks/usePageSearchParamCorrection.ts` | `P15-pagination-state` |
| `src/pages/objects/Pagination.ts` | `P15-pagination-state` |
| `src/pages/hooks/useProblemTitleDisplay.ts` | `P15-problem-title` |
| `src/pages/objects/ProblemTitleDisplay.ts` | `P15-problem-title` |
| `src/pages/objects/ResourceAccessDisplay.ts` | `P15-access` |
| `src/pages/components/PageLoadingCard.tsx` | ContestDetailPage, ContestManagePage, HackDetailPage, ProblemDataPage, ProblemDetailPage, ProblemSetDetailPage, ProblemSubmitPage, SubmissionDetailPage, SubmissionHackPage, UserGroupDetailPage |
| `src/pages/components/MarkdownDocument.tsx` | BlogDetailPage, ContestDetailPage, ContestManagePage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, ProblemDetailPage, ProblemSetDetailPage |
| `src/pages/hooks/useBeforeUnloadPrompt.ts` | CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, CreateUserGroupPage, ProblemDetailPage, ProblemSetDetailPage, ProblemSubmitPage, UserGroupDetailPage |
| `src/pages/components/ResourceAccessEditor.tsx` | BlogDetailPage, ContestManagePage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, ProblemDetailPage, ProblemSetDetailPage |
| `src/pages/components/ResourceAccessEditorInput.ts` | BlogDetailPage, ContestManagePage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, ProblemDetailPage, ProblemSetDetailPage |
| `src/pages/objects/ScoreDisplay.ts` | ContestRanklistPage, ContestSubmissionPage, HackDetailPage, HackPage, ProblemSubmissionPage, SubmissionDetailPage, SubmissionHackPage, SubmissionPage |
| `src/pages/components/ConfirmActionDialog.tsx` | ContestManagePage, ProblemDataPage, ProblemDetailPage, ProblemSetDetailPage, SiteManagePage, SubmissionDetailPage, UserGroupDetailPage |
| `src/pages/components/MarkdownEditorTabs.tsx` | ContestManagePage, CreateBlogPage, CreateContestPage, CreateProblemPage, CreateProblemSetPage, ProblemDetailPage, ProblemSetDetailPage |
| `src/pages/objects/SubmissionDisplay.ts` | ContestSubmissionPage, HackDetailPage, HackPage, ProblemSubmissionPage, SubmissionDetailPage, SubmissionHackPage, SubmissionPage |
| `src/pages/objects/BlogDisplay.ts` | BlogDetailPage, BlogPage, ProblemBlogPage, UserBlogPage |
| `src/pages/components/submission/SubmissionFilterCard.tsx` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/components/submission/SubmissionListPageContent.tsx` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/components/submission/SubmissionSummaryList.tsx` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/hooks/submission/useSubmissionListQuery.ts` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/hooks/submission/useSubmissionPageModel.ts` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/hooks/submission/useSubmissionSuggestions.ts` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/objects/SubmissionListForm.ts` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/objects/SubmissionPageState.ts` | ContestSubmissionPage, ProblemSubmissionPage, SubmissionPage |
| `src/pages/components/HackCard.tsx` | HackDetailPage, HackPage, SubmissionHackPage |
| `src/pages/objects/HackDisplay.ts` | HackDetailPage, HackPage, SubmissionHackPage |
| `src/pages/hooks/useProblemDetailQuery.ts` | ProblemDataPage, ProblemDetailPage, ProblemSubmitPage |
| `src/pages/components/AuthPageShell.tsx` | LoginPage, RegisterPage |
| `src/pages/components/AuthTextField.tsx` | LoginPage, RegisterPage |
| `src/pages/components/HackMetric.tsx` | HackDetailPage, SubmissionHackPage |
| `src/pages/hooks/useMessageInboxRefresh.ts` | MessageConversationPage, MessageInboxPage |
| `src/pages/objects/BlogForm.ts` | BlogDetailPage, CreateBlogPage |
| `src/pages/routing/MessagePaths.ts` | MessageInboxPage, UserProfilePage |

## App Shell Runtime

These files are not page-private even when a single page also observes their
events. They are mounted or reached from `src/router.tsx` through
`AuthenticatedRoute`, so their runtime scope is every authenticated route in
`P38`.

| File | Runtime consumers |
| --- | --- |
| `src/pages/hooks/useRealtimeConnection.ts` | `src/router.tsx` authenticated route wrapper, affecting `P38` |
| `src/pages/hooks/useRealtimeLeader.ts` | `useRealtimeConnection.ts`, affecting `P38` |
| `src/pages/hooks/realtimeRefresh.ts` | `useRealtimeConnection.ts` for initial/recovery refresh across `P38`; also MessageConversationPage, MessageInboxPage, NotificationPage through page refresh hooks |
| `src/pages/hooks/messageRealtimeEvents.ts` | `useRealtimeConnection.ts` for SSE decode/dispatch across `P38`; MessageConversationPage listens to decoded message events |

## Page-Private Or Removed

- `useNow` is page-private and now lives at `src/pages/ContestPage/hooks/useNow.ts`.
- `useNotificationRefresh` is page-private and now lives at `src/pages/NotificationPage/hooks/useNotificationRefresh.ts`.
- `useMessageRealtimeConnection` was a compatibility wrapper and was removed.
- `useNotificationRealtimeConnection` was unused and was removed.
