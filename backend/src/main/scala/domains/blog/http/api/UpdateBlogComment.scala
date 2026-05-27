package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.codec.BlogHttpCodecs.given
import domains.blog.http.mapper.BlogHttpRequestMappers
import cats.effect.IO
import domains.blog.application.BlogCommands
import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.UpdateBlogCommentRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateBlogComment:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "update" =>
        (BlogHttpRequestMappers.blogId(rawBlogId), BlogHttpRequestMappers.blogCommentId(rawCommentId)) match
          case (Left(message), _) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            context.handlers.executeDecoded[UpdateBlogCommentRequest, BlogHttpPlans.UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult](
              request,
              context.plans.updateBlogComment
            )(updateRequest => BlogHttpPlans.UpdateBlogCommentInput(blogId, commentId, updateRequest))
    }
