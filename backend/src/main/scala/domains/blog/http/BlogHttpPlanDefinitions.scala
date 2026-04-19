package domains.blog.http

import domains.shared.http.AuthenticatedHttpPlanRegistry

object BlogHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listBlogs = Plain(BlogHttpPlans.ListBlogs, BlogHttpResponses.mapListResult)
  val listProblemBlogs = Plain(BlogHttpPlans.ListProblemBlogs, BlogHttpResponses.mapListResult)
  val listPendingProblemBlogs = Plain(BlogHttpPlans.ListPendingProblemBlogs, BlogHttpResponses.mapListResult)
  val createBlog = WithTransaction(BlogHttpPlans.CreateBlog, BlogHttpResponses.mapCreateResult)
  val getBlog = Plain(BlogHttpPlans.GetBlog, BlogHttpResponses.mapGetResult)
  val voteBlog = WithTransaction(BlogHttpPlans.VoteBlog, BlogHttpResponses.mapVoteResult)
  val updateBlog = WithTransaction(BlogHttpPlans.UpdateBlog, BlogHttpResponses.mapUpdateResult)
  val deleteBlog = WithTransaction(BlogHttpPlans.DeleteBlog, BlogHttpResponses.mapDeleteResult)
  val submitBlogToProblem = WithTransaction(BlogHttpPlans.SubmitBlogToProblem, BlogHttpResponses.mapSubmitBlogToProblemResult)
  val linkBlogToProblem = WithTransaction(BlogHttpPlans.LinkBlogToProblem, BlogHttpResponses.mapLinkBlogToProblemResult)
  val acceptBlogProblemSubmission = WithTransaction(BlogHttpPlans.AcceptBlogProblemSubmission, BlogHttpResponses.mapAcceptBlogProblemSubmissionResult)
  val unlinkBlogFromProblem = WithTransaction(BlogHttpPlans.UnlinkBlogFromProblem, BlogHttpResponses.mapUnlinkBlogFromProblemResult)
  val createBlogComment = WithTransaction(BlogHttpPlans.CreateBlogComment, BlogHttpResponses.mapCreateCommentResult)
  val voteBlogComment = WithTransaction(BlogHttpPlans.VoteBlogComment, BlogHttpResponses.mapVoteCommentResult)
  val updateBlogComment = WithTransaction(BlogHttpPlans.UpdateBlogComment, BlogHttpResponses.mapUpdateCommentResult)
  val deleteBlogComment = WithTransaction(BlogHttpPlans.DeleteBlogComment, BlogHttpResponses.mapDeleteCommentResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listBlogs,
      listProblemBlogs,
      listPendingProblemBlogs,
      createBlog,
      getBlog,
      voteBlog,
      updateBlog,
      deleteBlog,
      submitBlogToProblem,
      linkBlogToProblem,
      acceptBlogProblemSubmission,
      unlinkBlogFromProblem,
      createBlogComment,
      voteBlogComment,
      updateBlogComment,
      deleteBlogComment
    ).map(plan => plan.name -> plan).toMap
