package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.blog.objects.BlogId
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogPostQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 获取博客详情的认证 API，只返回公开博客或当前用户自己的私有博客。 */
object GetBlog extends AuthenticatedApi[BlogId, BlogDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/blogs/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  /** 从路径解析博客 id，非法 id 转为 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))

  /** 按可见性加载博客详情，不可见或不存在时返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, blogId: BlogId): IO[BlogDetail] =
    BlogPostQueryTable.findById(connection, blogId, actor.username).flatMap {
      case Some(blog) => IO.pure(blog)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogNotFound))
    }
