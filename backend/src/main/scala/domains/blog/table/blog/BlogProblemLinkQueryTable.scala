package domains.blog.table.blog

import cats.effect.IO
import cats.syntax.all.*
import database.utils.UserIdentitySql
import domains.blog.objects.{BlogId, BlogProblemReference}
import domains.blog.objects.response.BlogSummary
import domains.blog.table.blog.BlogTableSupport.*
import domains.blog.table.blog_access_grant.BlogAccessGrantTable
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 博客与题目关联查询表访问对象，负责按题目列出博客和补全博客关联题目。 */
object BlogProblemLinkQueryTable:

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

  private val listByProblemSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinUserProfiles("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where p.slug = ?
      |  and bpl.status = 'accepted'
      |  and ${blogVisibleToViewerPredicate("b")}
      |order by b.created_at desc, b.public_id desc
      |limit ? offset ?
      |""".stripMargin

  private val countListByProblemSQL: String =
    s"""
      |select count(*) as total_items
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |where p.slug = ?
      |  and bpl.status = 'accepted'
      |  and ${blogVisibleToViewerPredicate("b")}
      |""".stripMargin

  /** 分页读取题目 accepted 博客关联，并按当前用户共享可见性过滤。 */
  def listByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByProblemSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, problemSlug.value)
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
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListByProblemSQL, statement =>
        statement.setString(1, problemSlug.value)
        bindBlogVisibleToViewer(statement, 2, viewerUsername)
      )
      summariesWithPolicies <- summaries.traverse(enrichVisibilityPolicy(connection))
    yield PageResponse(summariesWithPolicies, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val listPendingByProblemSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinUserProfiles("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where p.slug = ?
      |  and bpl.status = 'pending'
      |order by bpl.linked_at asc, b.public_id asc
      |limit ? offset ?
      |""".stripMargin

  private val countListPendingByProblemSQL: String =
    """
      |select count(*) as total_items
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |where p.slug = ?
      |  and bpl.status = 'pending'
      |""".stripMargin

  /** 分页读取题目 pending 博客关联，调用方负责先校验审核权限。 */
  def listPendingByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listPendingByProblemSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, problemSlug.value)
        statement.setInt(3, normalizedPageRequest.pageSize)
        statement.setInt(4, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          /** 注意：enrichSummaries 内部同步读取关联题目；这里位于 IO.blocking 中，避免在计算线程直接阻塞。 */
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListPendingByProblemSQL, statement => statement.setString(1, problemSlug.value))
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

  /** 为博客摘要同步补充 accepted 关联题目；调用方应确保外层已经在阻塞上下文中。 */
  def enrichSummaries(connection: Connection)(summaries: List[BlogSummary]): List[BlogSummary] =
    summaries.map(summary => summary.copy(relatedProblems = listRelatedProblemsBlocking(connection, summary.id)))

  /** 为单个博客摘要补充 accepted 关联题目，并显式包裹在 IO.blocking 中。 */
  def enrichSummary(connection: Connection, summary: BlogSummary): IO[BlogSummary] =
    IO.blocking(summary.copy(relatedProblems = listRelatedProblemsBlocking(connection, summary.id)))

  private def enrichVisibilityPolicy(connection: Connection)(summary: BlogSummary): IO[BlogSummary] =
    BlogAccessGrantTable.findInternalId(connection, summary.id).flatMap {
      case Some(internalBlogId) =>
        BlogAccessGrantTable
          .listForBlog(connection, internalBlogId)
          .map(grants => summary.copy(visibilityPolicy = summary.visibilityPolicy.copy(viewerGrants = grants)))
      case None =>
        IO.pure(summary)
    }

  private val listRelatedProblemsSQL: String =
    """
      |select p.slug, p.title
      |from blog_problem_links bpl
      |join blogs b on b.id = bpl.blog_id
      |join problems p on p.id = bpl.problem_id
      |where b.public_id = ?
      |  and bpl.status = 'accepted'
      |order by bpl.linked_at desc, p.slug asc
      |""".stripMargin

  private def listRelatedProblemsBlocking(connection: Connection, blogId: BlogId): List[BlogProblemReference] =
    val statement = connection.prepareStatement(listRelatedProblemsSQL)
    try
      statement.setLong(1, blogId.value)
      val resultSet = statement.executeQuery()
      try
        Iterator
          .continually(resultSet.next())
          .takeWhile(identity)
          .map(_ => readBlogProblemReference(resultSet))
          .toList
      finally resultSet.close()
    finally statement.close()
