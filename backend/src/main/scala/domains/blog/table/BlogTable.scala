package domains.blog.table



import cats.effect.IO
import domains.user.model.{DisplayName, Username}
import domains.user.model.UserIdentity
import domains.blog.application.{BlogCommentNotificationAncestor, BlogCommentNotificationContext}
import domains.blog.model.{BlogCommentContent, BlogCommentId, BlogContent, BlogId, BlogProblemReference, BlogTitle, BlogVisibility, BlogVote}
import domains.blog.application.output.{BlogCommentSummary, BlogDetail, BlogSummary}
import domains.blog.table.BlogTableSql.*
import domains.blog.table.utils.BlogTableSupport.*
import domains.problem.model.ProblemSlug
import shared.model.{PageRequest, PageResponse}

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object BlogTable:

  def initialize(connection: Connection): IO[Unit] =
    BlogTableSchema.initialize(connection)

  def insert(
    connection: Connection,
    authorUsername: Username,
    title: BlogTitle,
    content: BlogContent,
    visibility: BlogVisibility
  ): IO[BlogSummary] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setString(2, authorUsername.value)
        statement.setString(3, title.value)
        statement.setString(4, content.value)
        statement.setString(5, BlogVisibility.toDatabase(visibility))
        statement.setTimestamp(6, Timestamp.from(now))
        statement.setTimestamp(7, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            BlogSummary(
              id = BlogId(resultSet.getLong("public_id")),
              title = title,
              content = content,
              author = UserIdentity(
                authorUsername,
                DisplayName(resultSet.getString("author_display_name"))
              ),
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

  def listAll(connection: Connection, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listSql)
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
      totalItems <- countBlogs(connection, countListSql, statement => statement.setString(1, viewerUsername.value))
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  def listByAuthor(connection: Connection, authorUsername: Username, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByAuthorSql)
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
      totalItems <- countBlogs(connection, countListByAuthorSql, statement =>
        statement.setString(1, authorUsername.value)
        statement.setString(2, viewerUsername.value)
      )
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  def listByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listByProblemSql)
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
      totalItems <- countBlogs(connection, countListByProblemSql, statement =>
        statement.setString(1, problemSlug.value)
        statement.setString(2, viewerUsername.value)
      )
    yield PageResponse(summaries, normalizedPageRequest.page, normalizedPageRequest.pageSize, totalItems)

  def listPendingByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username, pageRequest: PageRequest): IO[PageResponse[BlogSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      summaries <- IO.blocking {
      val statement = connection.prepareStatement(listPendingByProblemSql)
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
      totalItems <- countBlogs(connection, countListPendingByProblemSql, statement => statement.setString(1, problemSlug.value))
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

  def contributionByAuthor(connection: Connection, authorUsername: Username): IO[BigDecimal] =
    IO.blocking {
      val statement = connection.prepareStatement(contributionByAuthorSql)
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

  def findSummaryById(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[Option[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSql)
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
          val statement = connection.prepareStatement(upsertVoteSql)
          try
            statement.setString(1, username.value)
            statement.setString(2, BlogVote.toDatabase(vote))
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

  def update(
    connection: Connection,
    blogId: BlogId,
    actorUsername: Username,
    title: BlogTitle,
    content: BlogContent,
    visibility: BlogVisibility
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateBlogSql)
      try
        statement.setString(1, title.value)
        statement.setString(2, content.value)
        statement.setString(3, BlogVisibility.toDatabase(visibility))
        statement.setTimestamp(4, Timestamp.from(Instant.now()))
        statement.setLong(5, blogId.value)
        statement.setString(6, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
    }

  def linkProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(linkProblemSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setString(3, problemSlug.value)
        statement.setLong(4, blogId.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def submitProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(submitProblemLinkSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setString(3, problemSlug.value)
        statement.setLong(4, blogId.value)
        statement.setString(5, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def acceptProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId,
    actorUsername: Username
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(acceptProblemLinkSql)
      try
        statement.setString(1, actorUsername.value)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        statement.setLong(3, blogId.value)
        statement.setString(4, problemSlug.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def unlinkProblem(
    connection: Connection,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteProblemLinkSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, problemSlug.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def delete(connection: Connection, blogId: BlogId, actorUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteBlogSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  def insertComment(
    connection: Connection,
    blogId: BlogId,
    parentCommentId: Option[BlogCommentId],
    authorUsername: Username,
    content: BlogCommentContent
  ): IO[Option[(BlogDetail, BlogCommentId)]] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(if parentCommentId.isDefined then insertReplySql else insertCommentSql)
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

  def findCommentNotificationContext(
    connection: Connection,
    blogId: BlogId,
    triggerCommentId: BlogCommentId
  ): IO[Option[BlogCommentNotificationContext]] =
    for
      maybeBlogContext <- IO.blocking {
        val statement = connection.prepareStatement(findCommentNotificationBlogContextSql)
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
            val statement = connection.prepareStatement(listCommentNotificationAncestorsSql)
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
          val statement = connection.prepareStatement(upsertCommentVoteSql)
          try
            statement.setString(1, username.value)
            statement.setString(2, BlogVote.toDatabase(vote))
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

  def updateComment(
    connection: Connection,
    blogId: BlogId,
    commentId: BlogCommentId,
    actorUsername: Username,
    content: BlogCommentContent
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateCommentSql)
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

  def deleteComment(
    connection: Connection,
    blogId: BlogId,
    commentId: BlogCommentId,
    actorUsername: Username
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteCommentSql)
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

  private def listComments(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[List[BlogCommentSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listCommentsSql)
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

  private def listRelatedProblemsBlocking(connection: Connection, blogId: BlogId): List[BlogProblemReference] =
    val statement = connection.prepareStatement(listRelatedProblemsSql)
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

  private def findCurrentVote(connection: Connection, blogId: BlogId, username: Username): IO[Option[BlogVote]] =
    IO.blocking {
      val statement = connection.prepareStatement(findViewerVoteSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setString(3, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then BlogVote.fromDatabase(resultSet.getString("vote"))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def deleteVote(connection: Connection, blogId: BlogId, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteVoteSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setString(3, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def findCurrentCommentVote(connection: Connection, blogId: BlogId, commentId: BlogCommentId, username: Username): IO[Option[BlogVote]] =
    IO.blocking {
      val statement = connection.prepareStatement(findCommentVoteSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setLong(3, commentId.value)
        statement.setString(4, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then BlogVote.fromDatabase(resultSet.getString("vote"))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def deleteCommentVote(connection: Connection, blogId: BlogId, commentId: BlogCommentId, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteCommentVoteSql)
      try
        statement.setLong(1, blogId.value)
        statement.setString(2, username.value)
        statement.setLong(3, commentId.value)
        statement.setString(4, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
