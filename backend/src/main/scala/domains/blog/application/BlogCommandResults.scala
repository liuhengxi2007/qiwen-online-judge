package domains.blog.application

import domains.blog.model.{BlogDetail, BlogSummary}

object BlogCommandResults:

  enum CreateBlogResult:
    case ValidationFailed(message: String)
    case Created(blog: BlogSummary)

  enum ListBlogsResult:
    case Listed(blogs: List[BlogSummary])

  enum GetBlogResult:
    case NotFound
    case Found(blog: BlogDetail)

  enum VoteBlogResult:
    case NotFound
    case Voted(blog: BlogDetail)

  enum UpdateBlogResult:
    case ValidationFailed(message: String)
    case NotFound
    case Updated(blog: BlogDetail)

  enum DeleteBlogResult:
    case NotFound
    case Deleted

  enum SubmitBlogToProblemResult:
    case NotFound
    case Submitted

  enum LinkBlogToProblemResult:
    case Forbidden
    case NotFound
    case Linked

  enum AcceptBlogProblemSubmissionResult:
    case Forbidden
    case NotFound
    case Accepted

  enum UnlinkBlogFromProblemResult:
    case Forbidden
    case NotFound
    case Unlinked

  enum CreateBlogCommentResult:
    case ValidationFailed(message: String)
    case BlogNotFound
    case Created(blog: BlogDetail)

  enum VoteBlogCommentResult:
    case NotFound
    case Voted(blog: BlogDetail)

  enum UpdateBlogCommentResult:
    case NotFound
    case Updated(blog: BlogDetail)

  enum DeleteBlogCommentResult:
    case NotFound
    case Deleted(blog: BlogDetail)
