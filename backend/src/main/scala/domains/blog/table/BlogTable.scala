package domains.blog.table

import cats.effect.IO
import domains.auth.model.{DisplayName, UserDisplayMode, UserIdentity, UserLocale, UserPreferences, Username}
import domains.blog.model.{BlogCommentContent, BlogCommentId, BlogCommentSummary, BlogContent, BlogDetail, BlogId, BlogSummary, BlogTitle, BlogType, BlogVisibility, BlogVote}
import domains.blog.table.BlogTableSql.*
import domains.blog.table.BlogTableSupport.*
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitleDisplayMode}

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
    visibility: BlogVisibility,
    blogType: BlogType,
    problemId: Option[ProblemId]
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
        statement.setString(6, BlogType.toDatabase(blogType))
        problemId match
          case Some(value) => statement.setObject(7, value.value)
          case None => statement.setNull(7, java.sql.Types.OTHER)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setTimestamp(9, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            BlogSummary(
              id = BlogId(resultSet.getLong("public_id")),
              title = title,
              content = content,
              author = UserIdentity(
                authorUsername,
                DisplayName(resultSet.getString("author_display_name")),
                UserPreferences(
                  displayMode =
                    UserDisplayMode
                      .fromDatabase(resultSet.getString("author_display_mode"))
                      .getOrElse(throw new IllegalStateException("Invalid author_display_mode.")),
                  locale =
                    UserLocale
                      .fromDatabase(resultSet.getString("author_locale"))
                      .getOrElse(throw new IllegalStateException("Invalid author_locale.")),
                  problemTitleDisplayMode =
                    ProblemTitleDisplayMode
                      .fromDatabase(resultSet.getString("author_problem_title_display_mode"))
                      .getOrElse(throw new IllegalStateException("Invalid author_problem_title_display_mode."))
                )
              ),
              visibility = visibility,
              blogType = blogType,
              problemSlug = None,
              problemTitle = None,
              score = 0,
              viewerVote = None,
              createdAt = resultSet.getTimestamp("created_at").toInstant,
              updatedAt = resultSet.getTimestamp("updated_at").toInstant
            )
          else missingInsertResult("blog")
        finally resultSet.close()
      finally statement.close()
    }

  def listAll(connection: Connection, viewerUsername: Username): IO[List[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, viewerUsername.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def listByAuthor(connection: Connection, authorUsername: Username, viewerUsername: Username): IO[List[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByAuthorSql)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, authorUsername.value)
        statement.setString(3, viewerUsername.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def listByProblem(connection: Connection, problemSlug: ProblemSlug, viewerUsername: Username): IO[List[BlogSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listByProblemSql)
      try
        statement.setString(1, viewerUsername.value)
        statement.setString(2, problemSlug.value)
        statement.setString(3, viewerUsername.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readBlogSummary(resultSet))
            .toList
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
        blogType = blog.blogType,
        problemSlug = blog.problemSlug,
        problemTitle = blog.problemTitle,
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
    visibility: BlogVisibility,
    blogType: BlogType,
    problemId: Option[ProblemId]
  ): IO[Option[BlogDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateBlogSql)
      try
        statement.setString(1, title.value)
        statement.setString(2, content.value)
        statement.setString(3, BlogVisibility.toDatabase(visibility))
        statement.setString(4, BlogType.toDatabase(blogType))
        problemId match
          case Some(value) => statement.setObject(5, value.value)
          case None => statement.setNull(5, java.sql.Types.OTHER)
        statement.setTimestamp(6, Timestamp.from(Instant.now()))
        statement.setLong(7, blogId.value)
        statement.setString(8, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
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
  ): IO[Option[BlogDetail]] =
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
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case false => IO.pure(None)
      case true => findById(connection, blogId, authorUsername)
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
