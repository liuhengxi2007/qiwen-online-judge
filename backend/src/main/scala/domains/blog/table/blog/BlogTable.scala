package domains.blog.table.blog



import cats.effect.IO
import domains.user.model.Username
import domains.blog.application.output.{BlogCommentNotificationAncestor, BlogCommentNotificationContext}
import domains.blog.model.{BlogCommentContent, BlogCommentId, BlogContent, BlogId, BlogProblemReference, BlogTitle, BlogVisibility, BlogVote}
import domains.blog.application.output.{BlogCommentSummary, BlogDetail, BlogSummary}
import domains.blog.table.blog.BlogTableSupport.*
import domains.problem.model.ProblemSlug
import shared.model.{PageRequest, PageResponse}

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID
import database.utils.UserIdentitySql

object BlogTable:

  def initialize(connection: Connection): IO[Unit] =
    BlogTableSchema.initialize(connection)

  private val insertSQL: String =
    s"""
      |insert into blogs (id, public_id, author_username, title, content, visibility, created_at, updated_at)
      |values (?, nextval('blog_public_id_seq'), ?, ?, ?, ?, ?, ?)
      |returning public_id, title, content, visibility, ${UserIdentitySql.returningColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  def insert(
    connection: Connection,
    authorUsername: Username,
    title: BlogTitle,
    content: BlogContent,
    visibility: BlogVisibility
  ): IO[BlogSummary] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setString(2, authorUsername.value)
        statement.setString(3, title.value)
        statement.setString(4, content.value)
        statement.setString(5, encodeBlogVisibilityColumn(visibility))
        statement.setTimestamp(6, Timestamp.from(now))
        statement.setTimestamp(7, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            BlogSummary(
              id = BlogId(resultSet.getLong("public_id")),
              title = title,
              content = content,
              author = readUserIdentity(resultSet, "author"),
              visibility = visibility,
              relatedProblems = Nil,
              score = 0,
              viewerVote = None,
              createdAt = resultSet.getTimestamp("created_at").toInstant,
              updatedAt = resultSet.getTimestamp("updated_at").toInstant
            )
          else missingInsertResult("blog")
        finally resultSet.close()
      finally statement.close()
    }

  private val blogSelectColumns: String =
    s"""
      |b.public_id,
      |       b.title,
      |       b.content,
      |       b.visibility,
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
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.visibility = 'public' or b.author_username = ?
      |order by b.created_at desc, b.public_id desc
      |limit ? offset ?
      |""".stripMargin

  private val countListSQL: String =
    """
      |select count(*) as total_items
      |from blogs b
      |where b.visibility = 'public' or b.author_username = ?
      |""".stripMargin

  def listAll(connection: Connection, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, viewerUsername.value)
        statement.setInt(3, normalizedPageRequest.pageSize)
        statement.setInt(4, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListSQL, statement => statement.setString(1, viewerUsername.value))
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val listByAuthorSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.author_username = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by b.public_id asc
      |limit ? offset ?
      |""".stripMargin

  private val countListByAuthorSQL: String =
    """
      |select count(*) as total_items
      |from blogs b
      |where b.author_username = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |""".stripMargin

  def listByAuthor(connection: Connection, authorUsername: Username, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByAuthorSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, authorUsername.value)
        statement.setString(3, viewerUsername.value)
        statement.setInt(4, normalizedPageRequest.pageSize)
        statement.setInt(5, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListByAuthorSQL, statement =>
        statement.setString(1, authorUsername.value)
        statement.setString(2, viewerUsername.value)
      )
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val listByProblemSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where p.slug = ?
      |  and bpl.status = 'accepted'
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by b.created_at desc, b.public_id desc
      |limit ? offset ?
      |""".stripMargin

  private val countListByProblemSQL: String =
    """
      |select count(*) as total_items
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |where p.slug = ?
      |  and bpl.status = 'accepted'
      |  and (b.visibility = 'public' or b.author_username = ?)
      |""".stripMargin

  def listByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByProblemSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, problemSlug.value)
        statement.setString(3, viewerUsername.value)
        statement.setInt(4, normalizedPageRequest.pageSize)
        statement.setInt(5, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val summaries = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListByProblemSQL, statement =>
        statement.setString(1, problemSlug.value)
        statement.setString(2, viewerUsername.value)
      )
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  private val listPendingByProblemSQL: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
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
          enrichSummaries(connection)(summaries)
        finally resultSet.close()
      finally statement.close()
      }
      totalItems <- countBlogs(connection, countListPendingByProblemSQL, statement => statement.setString(1, problemSlug.value))
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

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
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSQL
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |""".stripMargin

  def findSummaryById(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[Option[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setLong(2, blogId.value)
        statement.setString(3, viewerUsername.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readBlogSummary(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(summary) => enrichSummary(connection, summary).map(Some(_))
      case None => IO.pure(None)
    }

  def findById(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[Option[BlogDetail]] =
    for
      summary <- findSummaryById(connection, blogId, viewerUsername)
      comments <- summary match
        case Some(_) => listComments(connection, blogId, viewerUsername)
        case None => IO.pure(Nil)
    yield summary.map(blog =>
      BlogDetail(
        id = blog.id,
        title = blog.title,
        content = blog.content,
        author = blog.author,
        visibility = blog.visibility,
        relatedProblems = blog.relatedProblems,
        score = blog.score,
        viewerVote = blog.viewerVote,
        comments = comments,
        createdAt = blog.createdAt,
        updatedAt = blog.updatedAt
      )
    )

  private val upsertVoteSQL: String =
    """
      |insert into blog_votes (blog_id, username, vote, created_at, updated_at)
      |select id, ?, ?, ?, ?
      |from blogs
      |where public_id = ?
      |  and (visibility = 'public' or author_username = ?)
      |on conflict (blog_id, username)
      |do update set vote = excluded.vote,
      |              updated_at = excluded.updated_at
      |""".stripMargin

  def vote(
    connection: Connection,
    blogId: BlogId,
    username: Username,
    vote: BlogVote
  ): IO[Option[BlogDetail]] =
    findCurrentVote(connection, blogId, username).flatMap {
      case Some(currentVote) if currentVote == vote =>
        deleteVote(connection, blogId, username).flatMap(_ => findById(connection, blogId, username))
      case _ =>
        IO.blocking {
          val now = Instant.now()
          val statement = connection.prepareStatement(upsertVoteSQL)
          try
            statement.setString(1, username.value)
            statement.setString(2, encodeBlogVoteColumn(vote))
            statement.setTimestamp(3, Timestamp.from(now))
            statement.setTimestamp(4, Timestamp.from(now))
            statement.setLong(5, blogId.value)
            statement.setString(6, username.value)
            val updatedRows = statement.executeUpdate()
            updatedRows > 0
          finally statement.close()
        }.flatMap {
          case false => IO.pure(None)
          case true => findById(connection, blogId, username)
        }
    }

  private val updateBlogSQL: String =
    """
      |update blogs
      |set title = ?,
      |    content = ?,
      |    visibility = ?,
      |    updated_at = ?
      |where public_id = ?
      |  and author_username = ?
      |""".stripMargin

  def update(
    connection: Connection,
    blogId: BlogId,
    actorUsername: Username,
    title: BlogTitle,
    content: BlogContent,
    visibility: BlogVisibility
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateBlogSQL)
      try
        statement.setString(1, title.value)
        statement.setString(2, content.value)
        statement.setString(3, encodeBlogVisibilityColumn(visibility))
        statement.setTimestamp(4, Timestamp.from(Instant.now()))
        statement.setLong(5, blogId.value)
        statement.setString(6, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
    }

  private val linkProblemSQL: String =
    """
      |insert into blog_problem_links (blog_id, problem_id, linked_by, linked_at, status)
      |select b.id, p.id, ?, ?, 'accepted'
      |from blogs b
      |join problems p on p.slug = ?
      |where b.public_id = ?
      |  and b.visibility = 'public'
      |on conflict (blog_id, problem_id)
      |do update set status = 'accepted',
      |              linked_by = excluded.linked_by,
      |              linked_at = excluded.linked_at
      |""".stripMargin

  def linkProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(linkProblemSQL)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setString(3, problemSlug.value)
        statement.setLong(4, blogId.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val submitProblemLinkSQL: String =
    """
      |insert into blog_problem_links (blog_id, problem_id, linked_by, linked_at, status)
      |select b.id, p.id, ?, ?, 'pending'
      |from blogs b
      |join problems p on p.slug = ?
      |where b.public_id = ?
      |  and b.visibility = 'public'
      |  and b.author_username = ?
      |on conflict (blog_id, problem_id) do nothing
      |""".stripMargin

  def submitProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(submitProblemLinkSQL)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setString(3, problemSlug.value)
        statement.setLong(4, blogId.value)
        statement.setString(5, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val acceptProblemLinkSQL: String =
    """
      |update blog_problem_links bpl
      |set status = 'accepted',
      |    linked_by = ?,
      |    linked_at = ?
      |from blogs b, problems p
      |where bpl.blog_id = b.id
      |  and bpl.problem_id = p.id
      |  and b.public_id = ?
      |  and p.slug = ?
      |  and bpl.status = 'pending'
      |""".stripMargin

  def acceptProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(acceptProblemLinkSQL)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setLong(3, blogId.value)
        statement.setString(4, problemSlug.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val deleteProblemLinkSQL: String =
    """
      |delete from blog_problem_links bpl
      |using blogs b, problems p
      |where bpl.blog_id = b.id
      |  and bpl.problem_id = p.id
      |  and b.public_id = ?
      |  and p.slug = ?
      |""".stripMargin

  def unlinkProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteProblemLinkSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, problemSlug.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val deleteBlogSQL: String =
    """
      |delete from blogs
      |where public_id = ?
      |  and author_username = ?
      |""".stripMargin

  def delete(connection: Connection, blogId: BlogId, actorUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteBlogSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val insertCommentSQL: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), id, null, ?, ?, ?, ?
      |from blogs
      |where public_id = ?
      |  and (visibility = 'public' or author_username = ?)
      |returning public_id
      |""".stripMargin

  private val insertReplySQL: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), b.id, parent_comment.id, ?, ?, ?, ?
      |from blogs b
      |join blog_comments parent_comment on parent_comment.blog_id = b.id and parent_comment.public_id = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |returning public_id
      |""".stripMargin

  def insertComment(
    connection: Connection,
    blogId: BlogId,
    parentCommentId: Option[BlogCommentId],
    authorUsername: Username,
    content: BlogCommentContent
  ): IO[Option[(BlogDetail, BlogCommentId)]] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(if parentCommentId.isDefined then insertReplySQL else insertCommentSQL)
      try
        statement.setObject(1, UUID.randomUUID())
        parentCommentId match
          case Some(commentId) =>
            statement.setString(2, authorUsername.value)
            statement.setString(3, content.value)
            statement.setTimestamp(4, Timestamp.from(now))
            statement.setTimestamp(5, Timestamp.from(now))
            statement.setLong(6, commentId.value)
            statement.setLong(7, blogId.value)
            statement.setString(8, authorUsername.value)
          case None =>
            statement.setString(2, authorUsername.value)
            statement.setString(3, content.value)
            statement.setTimestamp(4, Timestamp.from(now))
            statement.setTimestamp(5, Timestamp.from(now))
            statement.setLong(6, blogId.value)
            statement.setString(7, authorUsername.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(BlogCommentId(resultSet.getLong("public_id")))
          else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case None => IO.pure(None)
      case Some(createdCommentId) =>
        findById(connection, blogId, authorUsername).map(_.map(_ -> createdCommentId))
    }

  private val findCommentNotificationBlogContextSQL: String =
    """
      |select b.public_id as blog_public_id,
      |       b.title as blog_title,
      |       b.author_username as blog_author_username,
      |       c.content as trigger_comment_content
      |from blogs b
      |join blog_comments c on c.blog_id = b.id
      |where b.public_id = ?
      |  and c.public_id = ?
      |""".stripMargin

  private val listCommentNotificationAncestorsSQL: String =
    """
      |with recursive ancestor_chain as (
      |  select parent.public_id,
      |         parent.parent_comment_id,
      |         parent.author_username,
      |         1 as depth
      |  from blog_comments child
      |  join blogs b on b.id = child.blog_id
      |  join blog_comments parent on parent.id = child.parent_comment_id
      |  where b.public_id = ?
      |    and child.public_id = ?
      |  union all
      |  select parent.public_id,
      |         parent.parent_comment_id,
      |         parent.author_username,
      |         ancestor_chain.depth + 1
      |  from ancestor_chain
      |  join blog_comments parent on parent.id = ancestor_chain.parent_comment_id
      |)
      |select public_id, author_username
      |from ancestor_chain
      |order by depth asc
      |""".stripMargin

  def findCommentNotificationContext(
    connection: Connection,
    blogId: BlogId,
    triggerCommentId: BlogCommentId
  ): IO[Option[BlogCommentNotificationContext]] =
    for
      maybeBlogContext <- IO.blocking {
        val statement = connection.prepareStatement(findCommentNotificationBlogContextSQL)
        try
          statement.setLong(1, blogId.value)
          statement.setLong(2, triggerCommentId.value)
          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then
              Some(
                (
                  parseColumn("blogs.title", resultSet.getString("blog_title"), BlogTitle.parse),
                  Username.canonical(resultSet.getString("blog_author_username")),
                  resultSet.getString("trigger_comment_content")
                )
              )
            else None
          finally resultSet.close()
        finally statement.close()
      }
      ancestors <- maybeBlogContext match
        case None => IO.pure(Nil)
        case Some(_) =>
          IO.blocking {
            val statement = connection.prepareStatement(listCommentNotificationAncestorsSQL)
            try
              statement.setLong(1, blogId.value)
              statement.setLong(2, triggerCommentId.value)
              val resultSet = statement.executeQuery()
              try
                Iterator
                  .continually(resultSet.next())
                  .takeWhile(identity)
                  .map(_ =>
                    BlogCommentNotificationAncestor(
                      commentId = BlogCommentId(resultSet.getLong("public_id")),
                      authorUsername = Username.canonical(resultSet.getString("author_username"))
                    )
                  )
                  .toList
              finally resultSet.close()
            finally statement.close()
          }
    yield maybeBlogContext.map { case (blogTitle, blogAuthorUsername, triggerCommentContent) =>
      BlogCommentNotificationContext(
        blogId = blogId,
        blogTitle = blogTitle,
        blogAuthorUsername = blogAuthorUsername,
        triggerCommentId = triggerCommentId,
        triggerCommentContent = triggerCommentContent,
        ancestors = ancestors
      )
    }

  private val upsertCommentVoteSQL: String =
    """
      |insert into blog_comment_votes (comment_id, username, vote, created_at, updated_at)
      |select c.id, ?, ?, ?, ?
      |from blog_comments c
      |join blogs b on b.id = c.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |on conflict (comment_id, username)
      |do update set vote = excluded.vote,
      |              updated_at = excluded.updated_at
      |""".stripMargin

  def voteComment(
    connection: Connection,
    blogId: BlogId,
    commentId: BlogCommentId,
    username: Username,
    vote: BlogVote
  ): IO[Option[BlogDetail]] =
    findCurrentCommentVote(connection, blogId, commentId, username).flatMap {
      case Some(currentVote) if currentVote == vote =>
        deleteCommentVote(connection, blogId, commentId, username).flatMap(_ => findById(connection, blogId, username))
      case _ =>
        IO.blocking {
          val now = Instant.now()
          val statement = connection.prepareStatement(upsertCommentVoteSQL)
          try
            statement.setString(1, username.value)
            statement.setString(2, encodeBlogVoteColumn(vote))
            statement.setTimestamp(3, Timestamp.from(now))
            statement.setTimestamp(4, Timestamp.from(now))
            statement.setLong(5, blogId.value)
            statement.setString(6, username.value)
            statement.setLong(7, commentId.value)
            statement.executeUpdate() > 0
          finally statement.close()
        }.flatMap {
          case false => IO.pure(None)
          case true => findById(connection, blogId, username)
        }
    }

  private val updateCommentSQL: String =
    """
      |update blog_comments c
      |set content = ?,
      |    updated_at = ?
      |from blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin

  def updateComment(
    connection: Connection,
    blogId: BlogId,
    commentId: BlogCommentId,
    actorUsername: Username,
    content: BlogCommentContent
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateCommentSQL)
      try
        statement.setString(1, content.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setLong(3, blogId.value)
        statement.setString(4, actorUsername.value)
        statement.setLong(5, commentId.value)
        statement.setString(6, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
    }

  private val deleteCommentSQL: String =
    """
      |delete from blog_comments c
      |using blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin

  def deleteComment(
    connection: Connection,
    blogId: BlogId,
    commentId: BlogCommentId,
    actorUsername: Username
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteCommentSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, actorUsername.value)
        statement.setLong(3, commentId.value)
        statement.setString(4, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
    }

  private val listCommentsSQL: String =
    s"""
      |select c.public_id,
      |       pc.public_id as parent_id,
      |       c.content,
      |       ${UserIdentitySql.selectColumns("c.author_username", "author", "au")},
      |       coalesce(cvs.score, 0) as score,
      |       viewer_vote.vote as viewer_vote,
      |       c.created_at,
      |       c.updated_at
      |from blog_comments c
      |join blogs b on b.id = c.blog_id
      |left join blog_comments pc on pc.id = c.parent_comment_id
      |${UserIdentitySql.joinAuthUsers("c.author_username", "au")}
      |left join (
      |  select comment_id,
      |         sum(case when vote = 'up' then 1 when vote = 'down' then -1 else 0 end)::int as score
      |  from blog_comment_votes
      |  group by comment_id
      |) cvs on cvs.comment_id = c.id
      |left join blog_comment_votes viewer_vote on viewer_vote.comment_id = c.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by c.public_id asc
      |""".stripMargin

  private def listComments(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[List[BlogCommentSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listCommentsSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setLong(2, blogId.value)
        statement.setString(3, viewerUsername.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogCommentSummary(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private def enrichSummaries(connection: Connection)(summaries: List[BlogSummary]): List[BlogSummary] =
    summaries.map(summary => summary.copy(relatedProblems = listRelatedProblemsBlocking(connection, summary.id)))

  private def enrichSummary(connection: Connection, summary: BlogSummary): IO[BlogSummary] =
    IO.blocking(summary.copy(relatedProblems = listRelatedProblemsBlocking(connection, summary.id)))

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

  private val findViewerVoteSQL: String =
    """
      |select bv.vote
      |from blog_votes bv
      |join blogs b on b.id = bv.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and bv.username = ?
      |""".stripMargin

  private def findCurrentVote(connection: Connection, blogId: BlogId, username: Username): IO[Option[BlogVote]] =
    IO.blocking {
      val statement = connection.prepareStatement(findViewerVoteSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setString(3, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then decodeBlogVoteColumn(resultSet.getString("vote"))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteVoteSQL: String =
    """
      |delete from blog_votes
      |where blog_id = (select id from blogs where public_id = ? and (visibility = 'public' or author_username = ?))
      |  and username = ?
      |""".stripMargin

  private def deleteVote(connection: Connection, blogId: BlogId, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteVoteSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setString(3, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val findCommentVoteSQL: String =
    """
      |select bcv.vote
      |from blog_comment_votes bcv
      |join blog_comments c on c.id = bcv.comment_id
      |join blogs b on b.id = c.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and bcv.username = ?
      |""".stripMargin

  private def findCurrentCommentVote(connection: Connection, blogId: BlogId, commentId: BlogCommentId, username: Username): IO[Option[BlogVote]] =
    IO.blocking {
      val statement = connection.prepareStatement(findCommentVoteSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setLong(3, commentId.value)
        statement.setString(4, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then decodeBlogVoteColumn(resultSet.getString("vote"))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteCommentVoteSQL: String =
    """
      |delete from blog_comment_votes
      |where comment_id = (
      |  select c.id
      |  from blog_comments c
      |  join blogs b on b.id = c.blog_id
      |  where b.public_id = ?
      |    and (b.visibility = 'public' or b.author_username = ?)
      |    and c.public_id = ?
      |)
      |and username = ?
      |""".stripMargin

  private def deleteCommentVote(connection: Connection, blogId: BlogId, commentId: BlogCommentId, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteCommentVoteSQL)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setLong(3, commentId.value)
        statement.setString(4, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
