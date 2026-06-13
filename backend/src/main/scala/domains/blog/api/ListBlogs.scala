package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.response.BlogListResponse
import domains.blog.table.blog.BlogPostQueryTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 分页列出博客的认证 API，可按作者过滤，并隐藏非本人私有博客。 */
object ListBlogs extends AuthenticatedApi[ListBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/blogs")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogListResponse] = summon[Encoder[BlogListResponse]]

  /** 从查询参数解析可选作者用户名和分页信息。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ListBlogsInput] =
    val _ = pathParams
    IO.pure(
      ListBlogsInput(
        authorUsername = request.uri.query.params.get("username").map(Username.canonical),
        pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
      )
    )

  /** 根据是否带作者过滤选择列表查询，查询层按当前用户处理私有博客可见性。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: ListBlogsInput): IO[BlogListResponse] =
    input.authorUsername match
      case Some(username) =>
        BlogPostQueryTable.listByAuthor(connection, username, actor.username, input.pageRequest.normalized)
      case None =>
        BlogPostQueryTable.listAll(connection, actor.username, input.pageRequest.normalized)
