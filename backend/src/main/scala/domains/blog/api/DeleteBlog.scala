package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogPostMutationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 删除当前用户博客的认证 API，只允许博客作者删除自己的博客。 */
object DeleteBlog extends AuthenticatedApi[BlogId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析博客 id，删除入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))

  /** 按博客 id 和当前作者删除博客；不存在或非作者时返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, blogId: BlogId): IO[SuccessResponse] =
    for
      deleted <- BlogPostMutationTable.delete(connection, blogId, actor.username)
      _ <- HttpApiError.ensure(deleted, HttpApiError.notFound(ApiMessages.blogNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogDeleted)
