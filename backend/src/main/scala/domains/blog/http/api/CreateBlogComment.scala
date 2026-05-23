package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.codec.BlogHttpCodecs.given
import cats.effect.IO
import domains.user.model.Username
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId}
import domains.blog.application.input.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.model.ProblemSlug
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateBlogComment:

  def routes(context: BlogHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            context.handlers.executeDecoded[CreateBlogCommentRequest, BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult](
              request,
              context.plans.createBlogComment
            )(commentRequest => BlogHttpPlans.CreateBlogCommentInput(blogId, None, commentRequest))
    }