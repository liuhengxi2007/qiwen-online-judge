package domains.blog.http

import domains.blog.http.mapper.BlogHttpResponseMappers



import domains.notification.application.NotificationEventHub
import shared.http.AuthenticatedHttpPlanRegistry

object BlogHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  final case class RegisteredPlans(
    listBlogs: Plain[domains.auth.model.AuthUser, BlogHttpPlans.ListBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    listProblemBlogs: Plain[domains.auth.model.AuthUser, BlogHttpPlans.ProblemBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    listPendingProblemBlogs: Plain[domains.auth.model.AuthUser, BlogHttpPlans.ProblemBlogsInput, domains.blog.application.BlogCommands.ListBlogsResult],
    createBlog: WithTransaction[domains.auth.model.AuthUser, domains.blog.model.request.CreateBlogRequest, domains.blog.application.BlogCommands.CreateBlogResult],
    getBlog: Plain[domains.auth.model.AuthUser, domains.blog.model.BlogId, domains.blog.application.BlogCommands.GetBlogResult],
    voteBlog: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.VoteBlogInput, domains.blog.application.BlogCommands.VoteBlogResult],
    updateBlog: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.UpdateBlogInput, domains.blog.application.BlogCommands.UpdateBlogResult],
    deleteBlog: WithTransaction[domains.auth.model.AuthUser, domains.blog.model.BlogId, domains.blog.application.BlogCommands.DeleteBlogResult],
    submitBlogToProblem: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.SubmitBlogToProblemResult],
    linkBlogToProblem: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.LinkBlogToProblemResult],
    acceptBlogProblemSubmission: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.AcceptBlogProblemSubmissionResult],
    unlinkBlogFromProblem: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.BlogProblemLinkInput, domains.blog.application.BlogCommands.UnlinkBlogFromProblemResult],
    createBlogComment: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.CreateBlogCommentInput, domains.blog.application.BlogCommands.CreateBlogCommentResult],
    voteBlogComment: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.VoteBlogCommentInput, domains.blog.application.BlogCommands.VoteBlogCommentResult],
    updateBlogComment: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.UpdateBlogCommentInput, domains.blog.application.BlogCommands.UpdateBlogCommentResult],
    deleteBlogComment: WithTransaction[domains.auth.model.AuthUser, BlogHttpPlans.DeleteBlogCommentInput, domains.blog.application.BlogCommands.DeleteBlogCommentResult]
  )

  def plans(notificationEventHub: NotificationEventHub): RegisteredPlans =
    RegisteredPlans(
      listBlogs = Plain(BlogHttpPlans.ListBlogs, BlogHttpResponseMappers.mapListResult),
      listProblemBlogs = Plain(BlogHttpPlans.ListProblemBlogs, BlogHttpResponseMappers.mapListResult),
      listPendingProblemBlogs = Plain(BlogHttpPlans.ListPendingProblemBlogs, BlogHttpResponseMappers.mapListResult),
      createBlog = WithTransaction(BlogHttpPlans.CreateBlog, BlogHttpResponseMappers.mapCreateResult),
      getBlog = Plain(BlogHttpPlans.GetBlog, BlogHttpResponseMappers.mapGetResult),
      voteBlog = WithTransaction(BlogHttpPlans.VoteBlog, BlogHttpResponseMappers.mapVoteResult),
      updateBlog = WithTransaction(BlogHttpPlans.UpdateBlog, BlogHttpResponseMappers.mapUpdateResult),
      deleteBlog = WithTransaction(BlogHttpPlans.DeleteBlog, BlogHttpResponseMappers.mapDeleteResult),
      submitBlogToProblem = WithTransaction(BlogHttpPlans.SubmitBlogToProblem, BlogHttpResponseMappers.mapSubmitBlogToProblemResult),
      linkBlogToProblem = WithTransaction(BlogHttpPlans.LinkBlogToProblem, BlogHttpResponseMappers.mapLinkBlogToProblemResult),
      acceptBlogProblemSubmission = WithTransaction(BlogHttpPlans.AcceptBlogProblemSubmission, BlogHttpResponseMappers.mapAcceptBlogProblemSubmissionResult),
      unlinkBlogFromProblem = WithTransaction(BlogHttpPlans.UnlinkBlogFromProblem, BlogHttpResponseMappers.mapUnlinkBlogFromProblemResult),
      createBlogComment = WithTransaction(new BlogHttpPlans.CreateBlogCommentPlan(notificationEventHub), BlogHttpResponseMappers.mapCreateCommentResult),
      voteBlogComment = WithTransaction(BlogHttpPlans.VoteBlogComment, BlogHttpResponseMappers.mapVoteCommentResult),
      updateBlogComment = WithTransaction(BlogHttpPlans.UpdateBlogComment, BlogHttpResponseMappers.mapUpdateCommentResult),
      deleteBlogComment = WithTransaction(BlogHttpPlans.DeleteBlogComment, BlogHttpResponseMappers.mapDeleteCommentResult)
    )
