package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.codec.BlogHttpCodecs.given
import cats.effect.IO
import domains.blog.application.BlogCommands
import domains.blog.application.input.VoteBlogCommentRequest
import domains.blog.model.{BlogCommentId, BlogId}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object VoteBlogComment:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "vote" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            context.handlers.executeDecoded[VoteBlogCommentRequest, BlogHttpPlans.VoteBlogCommentInput, BlogCommands.VoteBlogCommentResult](
              request,
              context.plans.voteBlogComment
            )(voteRequest => BlogHttpPlans.VoteBlogCommentInput(blogId, commentId, voteRequest))
    }
