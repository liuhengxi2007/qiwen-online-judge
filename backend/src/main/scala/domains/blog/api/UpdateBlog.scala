package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser


import domains.blog.objects.{BlogContent, BlogId, BlogTitle}
import domains.blog.objects.request.UpdateBlogRequest
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogPostMutationTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateBlog extends AuthenticatedApi[UpdateBlogInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[UpdateBlogInput] =
    for
      blogId <- HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))
      body <- request.as[UpdateBlogRequest]
    yield UpdateBlogInput(blogId, body)

  override def plan(connection: Connection, actor: AuthUser, input: UpdateBlogInput): IO[BlogDetail] =
    for
      title <- HttpApiError.fromEitherBadRequest(BlogTitle.parse(input.request.title.value))
      content <- HttpApiError.fromEitherBadRequest(BlogContent.parse(input.request.content.value))
      validRequest = input.request.copy(title = title, content = content)
      maybeBlog <- BlogPostMutationTable.update(
        connection,
        input.blogId,
        actor.username,
        validRequest.title,
        validRequest.content,
        validRequest.visibility
      )
      blog <- maybeBlog match
        case Some(blog) => IO.pure(blog)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogNotFound))
    yield blog
