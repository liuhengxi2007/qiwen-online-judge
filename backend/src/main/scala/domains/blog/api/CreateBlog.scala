package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.blog.objects.{BlogContent, BlogTitle}
import domains.blog.objects.request.CreateBlogRequest
import domains.blog.objects.response.BlogSummary
import domains.blog.table.blog.BlogPostMutationTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 创建博客的认证 API，写入当前用户的博客并返回摘要。 */
object CreateBlog extends AuthenticatedApi[CreateBlogRequest, BlogSummary]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[BlogSummary] = summon[Encoder[BlogSummary]]

  /** 读取创建博客请求体，路径参数不参与该入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateBlogRequest] =
    val _ = pathParams
    request.as[CreateBlogRequest]

  /** 校验标题和正文长度后以当前用户为作者创建博客。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateBlogRequest): IO[BlogSummary] =
    for
      title <- HttpApiError.fromEitherBadRequest(BlogTitle.parse(request.title.value))
      content <- HttpApiError.fromEitherBadRequest(BlogContent.parse(request.content.value))
      validRequest = request.copy(title = title, content = content)
      blog <- BlogPostMutationTable.insert(connection, actor.username, validRequest.title, validRequest.content, validRequest.visibility)
    yield blog
