package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import domains.blog.model.{BlogCommentId, BlogId}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object DeleteBlogComment:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "delete" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            context.handlers.execute(request, BlogHttpPlans.DeleteBlogCommentInput(blogId, commentId), context.plans.deleteBlogComment)
    }