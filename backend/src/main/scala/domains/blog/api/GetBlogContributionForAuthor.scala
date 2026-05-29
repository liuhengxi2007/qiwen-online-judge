package domains.blog.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.blog.objects.response.BlogContributionResponse
import domains.blog.table.blog.BlogPostQueryTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object GetBlogContributionForAuthor extends InternalOnlyApi[Username, BlogContributionResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/blogs/contribution-for-author")

  override def plan(connection: Connection, username: Username): IO[BlogContributionResponse] =
    BlogPostQueryTable
      .contributionByAuthor(connection, username)
      .map(contribution => BlogContributionResponse(contribution.toInt))
