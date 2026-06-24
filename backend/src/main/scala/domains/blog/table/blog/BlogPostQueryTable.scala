package domains.blog.table.blog

import cats.effect.IO
import cats.syntax.all.*
import database.utils.UserIdentitySql
import domains.blog.objects.BlogId
import domains.blog.objects.response.{BlogDetail, BlogSummary}
import domains.blog.table.blog.BlogTableSupport.*
import domains.blog.table.blog_access_grant.BlogAccessGrantTable
import domains.user.objects.Username
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 博客查询表访问对象，负责列表、作者贡献、详情和可见性过滤。 */
object BlogPostQueryTable:

  private val blogSelectColumns: String =
    s"""
      |b.public_id,
      |       b.title,
      |       b.content,
      |       b.base_access,
      |       ${UserIdentitySql.selectColumns("b.author_username", "author", "au")},
      |       coalesce(vs.score, 0) as score,
      |       viewer_vote.vote as viewer_vote,
      |       b.created_at,
      |       b.updated_at
      |""".stripMargin

  private val blogScoreJoinSQL: String =
    """
      |left join (
      |  select blog_id,
      |         sum(case when vote = 'up' then 1 when vote = 'down' then -1 else 0 end)::int as score
      |  from blog_votes
      |  group by blog_id
      |) vs on vs.blog_id = b.id
      |""".stripMargin

  private val listSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinUserProfiles("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where ${blogVisibleToViewerPredicate("b")}
      |order by b.created_at desc, b.public_id desc
      |limit ? offset ?
      |""".stripMargin

  private val countListSQL: String =
    s"""
      |select count(*) as total_items
      |from blogs b
      |where ${blogVisibleToViewerPredicate("b")}
      |""".stripMargin

  /** 分页读取当前用户可见的博客列表，包含公开博客、自己的博客和显式授权博客。 */
  def listAll(connection: Connection, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listSQL)
      try
        statement.setString(1, viewerUsername.value)
        val nextIndex = bindBlogVisibleToViewer(statement, 2, viewerUsername)
        statement.setInt(nextIndex, normalizedPageRequest.pageSize)
        statement.setInt(nextIndex + 1, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          /** 注意：enrichSummaries 内部同步读取关联题目；这里位于 IO.blocking 中，避免在计算线程直接阻塞。 */
          BlogProblemLinkQueryTable.enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      summariesWithPolicies <- summaries.traverse(enrichVisibilityPolicy(connection))
      totalItems <- countBlogs(connection, countListSQL, statement => bindBlogVisibleToViewer(statement, 1, viewerUsername))
    yield PageResponse(summariesWithPolicies, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val listByAuthorSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinUserProfiles("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.author_username = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |order by b.public_id asc
      |limit ? offset ?
      |""".stripMargin

  private val countListByAuthorSQL: String =
    s"""
      |select count(*) as total_items
      |from blogs b
      |where b.author_username = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |""".stripMargin

  /** 分页读取指定作者博客；按当前 viewer 的共享可见性过滤。 */
  def listByAuthor(connection: Connection, authorUsername: Username, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByAuthorSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, authorUsername.value)
        val nextIndex = bindBlogVisibleToViewer(statement, 3, viewerUsername)
        statement.setInt(nextIndex, normalizedPageRequest.pageSize)
        statement.setInt(nextIndex + 1, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          /** 注意：enrichSummaries 内部同步读取关联题目；这里位于 IO.blocking 中，避免在计算线程直接阻塞。 */
          BlogProblemLinkQueryTable.enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListByAuthorSQL, statement =>
        statement.setString(1, authorUsername.value)
        bindBlogVisibleToViewer(statement, 2, viewerUsername)
      )
      summariesWithPolicies <- summaries.traverse(enrichVisibilityPolicy(connection))
    yield PageResponse(summariesWithPolicies, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private def countBlogs(connection: Connection, sql: String, bind: java.sql.PreparedStatement => Unit): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        bind(statement)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getLong("total_items") else 0L
        finally resultSet.close()
      finally statement.close()
    }

  private val contributionByAuthorSQL: String =
    """
      |select coalesce(blog_scores.blog_score, 0)::numeric +
      |       coalesce(comment_scores.comment_score, 0)::numeric * 0.1 as contribution
      |from (select ?::varchar as username) target
      |left join (
      |  select b.author_username,
      |         sum(case when bv.vote = 'up' then 1 when bv.vote = 'down' then -1 else 0 end)::numeric as blog_score
      |  from blogs b
      |  left join blog_votes bv on bv.blog_id = b.id
      |  where lower(b.author_username) = lower(?)
      |  group by b.author_username
      |) blog_scores on blog_scores.author_username = target.username
      |left join (
      |  select c.author_username,
      |         sum(case when bcv.vote = 'up' then 1 when bcv.vote = 'down' then -1 else 0 end)::numeric as comment_score
      |  from blog_comments c
      |  left join blog_comment_votes bcv on bcv.comment_id = c.id
      |  where lower(c.author_username) = lower(?)
      |  group by c.author_username
      |) comment_scores on comment_scores.author_username = target.username
      |""".stripMargin

  /** 统计作者博客和评论投票贡献，评论分按 0.1 权重计入。 */
  def contributionByAuthor(connection: Connection, authorUsername: Username): IO[BigDecimal] =
    IO.blocking {
      val statement = connection.prepareStatement(contributionByAuthorSQL)
      try
        statement.setString(1, authorUsername.value)
        statement.setString(2, authorUsername.value)
        statement.setString(3, authorUsername.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then BigDecimal(resultSet.getBigDecimal("contribution")).setScale(0, BigDecimal.RoundingMode.HALF_UP)
          else BigDecimal(0)
        finally resultSet.close()
      finally statement.close()
    }

  private val findByIdSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinUserProfiles("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |""".stripMargin

  /** 按公开 id 查找当前用户可见的博客摘要，并补充 accepted 关联题目。 */
  def findSummaryById(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[Option[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setLong(2, blogId.value)
        bindBlogVisibleToViewer(statement, 3, viewerUsername)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readBlogSummary(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(summary) =>
        for
          withProblems <- BlogProblemLinkQueryTable.enrichSummary(connection, summary)
          withPolicy <- enrichVisibilityPolicy(connection)(withProblems)
        yield Some(withPolicy)
      case None => IO.pure(None)
    }

  /** 按公开 id 查找当前用户可见的博客详情，并在可见时读取评论列表。 */
  def findById(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[Option[BlogDetail]] =
    for
      summary <- findSummaryById(connection, blogId, viewerUsername)
      comments <- summary match
        case Some(_) => BlogCommentTable.listComments(connection, blogId, viewerUsername)
        case None => IO.pure(Nil)
    yield summary.map(blog =>
      BlogDetail(
        id = blog.id,
        title = blog.title,
        content = blog.content,
        author = blog.author,
        visibilityPolicy = blog.visibilityPolicy,
        relatedProblems = blog.relatedProblems,
        score = blog.score,
        viewerVote = blog.viewerVote,
        comments = comments,
        createdAt = blog.createdAt,
        updatedAt = blog.updatedAt
      )
    )

  private def enrichVisibilityPolicy(connection: Connection)(summary: BlogSummary): IO[BlogSummary] =
    BlogAccessGrantTable.findInternalId(connection, summary.id).flatMap {
      case Some(internalBlogId) =>
        BlogAccessGrantTable
          .listForBlog(connection, internalBlogId)
          .map(grants => summary.copy(visibilityPolicy = summary.visibilityPolicy.copy(viewerGrants = grants)))
      case None =>
        IO.pure(summary)
    }
