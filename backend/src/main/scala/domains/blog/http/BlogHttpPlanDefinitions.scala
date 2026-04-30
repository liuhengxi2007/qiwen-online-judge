package domains.blog.http

import domains.notification.application.NotificationEventHub
import domains.shared.http.AuthenticatedHttpPlanRegistry

object BlogHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  def plans(notificationEventHub: NotificationEventHub): Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      Plain(BlogHttpPlans.ListBlogs, BlogHttpResponses.mapListResult),
      Plain(BlogHttpPlans.ListProblemBlogs, BlogHttpResponses.mapListResult),
      Plain(BlogHttpPlans.ListPendingProblemBlogs, BlogHttpResponses.mapListResult),
      WithTransaction(BlogHttpPlans.CreateBlog, BlogHttpResponses.mapCreateResult),
      Plain(BlogHttpPlans.GetBlog, BlogHttpResponses.mapGetResult),
      WithTransaction(BlogHttpPlans.VoteBlog, BlogHttpResponses.mapVoteResult),
      WithTransaction(BlogHttpPlans.UpdateBlog, BlogHttpResponses.mapUpdateResult),
      WithTransaction(BlogHttpPlans.DeleteBlog, BlogHttpResponses.mapDeleteResult),
      WithTransaction(BlogHttpPlans.SubmitBlogToProblem, BlogHttpResponses.mapSubmitBlogToProblemResult),
      WithTransaction(BlogHttpPlans.LinkBlogToProblem, BlogHttpResponses.mapLinkBlogToProblemResult),
      WithTransaction(BlogHttpPlans.AcceptBlogProblemSubmission, BlogHttpResponses.mapAcceptBlogProblemSubmissionResult),
      WithTransaction(BlogHttpPlans.UnlinkBlogFromProblem, BlogHttpResponses.mapUnlinkBlogFromProblemResult),
      WithTransaction(new BlogHttpPlans.CreateBlogCommentPlan(notificationEventHub), BlogHttpResponses.mapCreateCommentResult),
      WithTransaction(BlogHttpPlans.VoteBlogComment, BlogHttpResponses.mapVoteCommentResult),
      WithTransaction(BlogHttpPlans.UpdateBlogComment, BlogHttpResponses.mapUpdateCommentResult),
      WithTransaction(BlogHttpPlans.DeleteBlogComment, BlogHttpResponses.mapDeleteCommentResult)
    ).map(plan => plan.name -> plan).toMap
