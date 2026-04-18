package domains.blog.http

import domains.shared.http.AuthenticatedHttpPlanRegistry

object BlogHttpPlanDefinitions:

  import AuthenticatedHttpPlanRegistry.RegisteredPlan.{Plain, WithTransaction}

  val listBlogs = Plain(BlogHttpPlans.ListBlogs, BlogHttpResponses.mapListResult)
  val listProblemBlogs = Plain(BlogHttpPlans.ListProblemBlogs, BlogHttpResponses.mapListResult)
  val createBlog = WithTransaction(BlogHttpPlans.CreateBlog, BlogHttpResponses.mapCreateResult)
  val getBlog = Plain(BlogHttpPlans.GetBlog, BlogHttpResponses.mapGetResult)
  val voteBlog = WithTransaction(BlogHttpPlans.VoteBlog, BlogHttpResponses.mapVoteResult)
  val updateBlog = WithTransaction(BlogHttpPlans.UpdateBlog, BlogHttpResponses.mapUpdateResult)
  val deleteBlog = WithTransaction(BlogHttpPlans.DeleteBlog, BlogHttpResponses.mapDeleteResult)
  val createBlogComment = WithTransaction(BlogHttpPlans.CreateBlogComment, BlogHttpResponses.mapCreateCommentResult)
  val voteBlogComment = WithTransaction(BlogHttpPlans.VoteBlogComment, BlogHttpResponses.mapVoteCommentResult)
  val updateBlogComment = WithTransaction(BlogHttpPlans.UpdateBlogComment, BlogHttpResponses.mapUpdateCommentResult)
  val deleteBlogComment = WithTransaction(BlogHttpPlans.DeleteBlogComment, BlogHttpResponses.mapDeleteCommentResult)

  val plans: Map[String, AuthenticatedHttpPlanRegistry.RegisteredPlan] =
    List(
      listBlogs,
      listProblemBlogs,
      createBlog,
      getBlog,
      voteBlog,
      updateBlog,
      deleteBlog,
      createBlogComment,
      voteBlogComment,
      updateBlogComment,
      deleteBlogComment
    ).map(plan => plan.name -> plan).toMap
