package domains.blog.http

import cats.effect.IO
import domains.blog.application.BlogCommands
import domains.shared.http.HttpResponseSupport.errorResponse
import domains.shared.model.SuccessResponse
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}

object BlogHttpResponses:

  def mapListResult(result: BlogCommands.ListBlogsResult): IO[Response[IO]] =
    result match
      case BlogCommands.ListBlogsResult.Listed(blogs) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(blogs.asJson))

  def mapCreateResult(result: BlogCommands.CreateBlogResult): IO[Response[IO]] =
    result match
      case BlogCommands.CreateBlogResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case BlogCommands.CreateBlogResult.Created(blog) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(blog.asJson))

  def mapGetResult(result: BlogCommands.GetBlogResult): IO[Response[IO]] =
    result match
      case BlogCommands.GetBlogResult.NotFound =>
        errorResponse(Status.NotFound, "Blog not found.")
      case BlogCommands.GetBlogResult.Found(blog) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))

  def mapVoteResult(result: BlogCommands.VoteBlogResult): IO[Response[IO]] =
    result match
      case BlogCommands.VoteBlogResult.NotFound =>
        errorResponse(Status.NotFound, "Blog not found.")
      case BlogCommands.VoteBlogResult.Voted(blog) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))

  def mapUpdateResult(result: BlogCommands.UpdateBlogResult): IO[Response[IO]] =
    result match
      case BlogCommands.UpdateBlogResult.ValidationFailed(message) => errorResponse(Status.BadRequest, message)
      case BlogCommands.UpdateBlogResult.NotFound => errorResponse(Status.NotFound, "Blog not found.")
      case BlogCommands.UpdateBlogResult.Updated(blog) => IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))

  def mapDeleteResult(result: BlogCommands.DeleteBlogResult): IO[Response[IO]] =
    result match
      case BlogCommands.DeleteBlogResult.NotFound => errorResponse(Status.NotFound, "Blog not found.")
      case BlogCommands.DeleteBlogResult.Deleted => IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Blog deleted.").asJson))

  def mapSubmitBlogToProblemResult(result: BlogCommands.SubmitBlogToProblemResult): IO[Response[IO]] =
    result match
      case BlogCommands.SubmitBlogToProblemResult.NotFound => errorResponse(Status.NotFound, "Problem or owned public blog not found.")
      case BlogCommands.SubmitBlogToProblemResult.Submitted => IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Blog submitted to problem.").asJson))

  def mapLinkBlogToProblemResult(result: BlogCommands.LinkBlogToProblemResult): IO[Response[IO]] =
    result match
      case BlogCommands.LinkBlogToProblemResult.Forbidden => errorResponse(Status.Forbidden, "You cannot manage this problem's blog links.")
      case BlogCommands.LinkBlogToProblemResult.NotFound => errorResponse(Status.NotFound, "Problem or public blog not found.")
      case BlogCommands.LinkBlogToProblemResult.Linked => IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Blog linked to problem.").asJson))

  def mapAcceptBlogProblemSubmissionResult(result: BlogCommands.AcceptBlogProblemSubmissionResult): IO[Response[IO]] =
    result match
      case BlogCommands.AcceptBlogProblemSubmissionResult.Forbidden => errorResponse(Status.Forbidden, "You cannot manage this problem's blog links.")
      case BlogCommands.AcceptBlogProblemSubmissionResult.NotFound => errorResponse(Status.NotFound, "Pending problem blog submission not found.")
      case BlogCommands.AcceptBlogProblemSubmissionResult.Accepted => IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Problem blog submission accepted.").asJson))

  def mapUnlinkBlogFromProblemResult(result: BlogCommands.UnlinkBlogFromProblemResult): IO[Response[IO]] =
    result match
      case BlogCommands.UnlinkBlogFromProblemResult.Forbidden => errorResponse(Status.Forbidden, "You cannot manage this problem's blog links.")
      case BlogCommands.UnlinkBlogFromProblemResult.NotFound => errorResponse(Status.NotFound, "Problem blog link not found.")
      case BlogCommands.UnlinkBlogFromProblemResult.Unlinked => IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("Blog unlinked from problem.").asJson))

  def mapCreateCommentResult(result: BlogCommands.CreateBlogCommentResult): IO[Response[IO]] =
    result match
      case BlogCommands.CreateBlogCommentResult.ValidationFailed(message) =>
        errorResponse(Status.BadRequest, message)
      case BlogCommands.CreateBlogCommentResult.BlogNotFound =>
        errorResponse(Status.NotFound, "Blog not found.")
      case BlogCommands.CreateBlogCommentResult.Created(blog) =>
        IO.pure(Response[IO](status = Status.Created).withEntity(blog.asJson))

  def mapVoteCommentResult(result: BlogCommands.VoteBlogCommentResult): IO[Response[IO]] =
    result match
      case BlogCommands.VoteBlogCommentResult.NotFound =>
        errorResponse(Status.NotFound, "Blog comment not found.")
      case BlogCommands.VoteBlogCommentResult.Voted(blog) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))

  def mapUpdateCommentResult(result: BlogCommands.UpdateBlogCommentResult): IO[Response[IO]] =
    result match
      case BlogCommands.UpdateBlogCommentResult.NotFound => errorResponse(Status.NotFound, "Blog comment not found.")
      case BlogCommands.UpdateBlogCommentResult.Updated(blog) => IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))

  def mapDeleteCommentResult(result: BlogCommands.DeleteBlogCommentResult): IO[Response[IO]] =
    result match
      case BlogCommands.DeleteBlogCommentResult.NotFound => errorResponse(Status.NotFound, "Blog comment not found.")
      case BlogCommands.DeleteBlogCommentResult.Deleted(blog) => IO.pure(Response[IO](status = Status.Ok).withEntity(blog.asJson))
