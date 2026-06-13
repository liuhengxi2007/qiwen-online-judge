package domains.blog.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.blog.objects.response.BlogContributionResponse
import domains.blog.table.blog.BlogPostQueryTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部博客贡献值查询 API，供用户画像或排名聚合使用。 */
object GetBlogContributionForAuthor extends InternalOnlyApi[Username, BlogContributionResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/blogs/contribution-for-author")

  /** 按作者统计博客与评论投票贡献，并四舍五入为整数响应。 */
  override def plan(connection: Connection, username: Username): IO[BlogContributionResponse] =
    BlogPostQueryTable
      .contributionByAuthor(connection, username)
      .map(contribution => BlogContributionResponse(contribution.toInt))
