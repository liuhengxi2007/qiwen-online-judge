package domains.blog.http

import domains.notification.application.NotificationEventHub
import domains.shared.http.AuthenticatedHttpPlanRegistry

object BlogHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listBlogs: Plain[BlogHttpPlans.ListBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    listProblemBlogs: Plain[BlogHttpPlans.ProblemBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    listPendingProblemBlogs: Plain[BlogHttpPlans.ProblemBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    createBlog: WithTransaction[domains.blog.model.CreateBlogRequest, domains.blog.application.BlogCommands.CreateBlogResult],
    getBlog: Plain[domains.blog.model.BlogId, domains.blog.application.BlogCommands.GetBlogResult],
    voteBlog: WithTransaction[BlogHttpPlans.VoteBlogInput, domains.blog.application.BlogCommands.VoteBlogResult],
    updateBlog: WithTransaction[BlogHttpPlans.UpdateBlogInput, domains.blog.application.BlogCommands.UpdateBlogResult],
    deleteBlog: WithTransaction[domains.blog.model.BlogId, domains.blog.application.BlogCommands.DeleteBlogResult],
    submitBlogToProblem: WithTransaction[BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.SubmitBlogToProblemResult],
    linkBlogToProblem: WithTransaction[BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.LinkBlogToProblemResult],
    acceptBlogProblemSubmission: WithTransaction[BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.AcceptBlogProblemSubmissionResult],
    unlinkBlogFromProblem: WithTransaction[BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.UnlinkBlogFromProblemResult],
    createBlogComment: WithTransaction[BlogHttpPlans.CreateBlogCommentInput, domains.blog.application.BlogCommands.CreateBlogCommentResult],
    voteBlogComment: WithTransaction[BlogHttpPlans.VoteBlogCommentInput, domains.blog.application.BlogCommands.VoteBlogCommentResult],
    updateBlogComment: WithTransaction[BlogHttpPlans.UpdateBlogCommentInput, domains.blog.application.BlogCommands.UpdateBlogCommentResult],
    deleteBlogComment: WithTransaction[BlogHttpPlans.DeleteBlogCommentInput, domains.blog.application.BlogCommands.DeleteBlogCommentResult]
  )

  def plans(notificationEventHub: NotificationEventHub): RegisteredPlans =
    RegisteredPlans(
      listBlogs = Plain(BlogHttpPlans.ListBlogs, BlogHttpResponses.mapListResult),
      listProblemBlogs = Plain(BlogHttpPlans.ListProblemBlogs, BlogHttpResponses.mapListResult),
      listPendingProblemBlogs = Plain(BlogHttpPlans.ListPendingProblemBlogs, BlogHttpResponses.mapListResult),
      createBlog = WithTransaction(BlogHttpPlans.CreateBlog, BlogHttpResponses.mapCreateResult),
      getBlog = Plain(BlogHttpPlans.GetBlog, BlogHttpResponses.mapGetResult),
      voteBlog = WithTransaction(BlogHttpPlans.VoteBlog, BlogHttpResponses.mapVoteResult),
      updateBlog = WithTransaction(BlogHttpPlans.UpdateBlog, BlogHttpResponses.mapUpdateResult),
      deleteBlog = WithTransaction(BlogHttpPlans.DeleteBlog, BlogHttpResponses.mapDeleteResult),
      submitBlogToProblem = WithTransaction(BlogHttpPlans.SubmitBlogToProblem, BlogHttpResponses.mapSubmitBlogToProblemResult),
      linkBlogToProblem = WithTransaction(BlogHttpPlans.LinkBlogToProblem, BlogHttpResponses.mapLinkBlogToProblemResult),
      acceptBlogProblemSubmission = WithTransaction(BlogHttpPlans.AcceptBlogProblemSubmission, BlogHttpResponses.mapAcceptBlogProblemSubmissionResult),
      unlinkBlogFromProblem = WithTransaction(BlogHttpPlans.UnlinkBlogFromProblem, BlogHttpResponses.mapUnlinkBlogFromProblemResult),
      createBlogComment = WithTransaction(new BlogHttpPlans.CreateBlogCommentPlan(notificationEventHub), BlogHttpResponses.mapCreateCommentResult),
      voteBlogComment = WithTransaction(BlogHttpPlans.VoteBlogComment, BlogHttpResponses.mapVoteCommentResult),
      updateBlogComment = WithTransaction(BlogHttpPlans.UpdateBlogComment, BlogHttpResponses.mapUpdateCommentResult),
      deleteBlogComment = WithTransaction(BlogHttpPlans.DeleteBlogComment, BlogHttpResponses.mapDeleteCommentResult)
    )
