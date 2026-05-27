package domains.blog.table.blog

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.blog.objects.{BlogContent, BlogId, BlogTitle, BlogVisibility}
import domains.blog.objects.response.BlogSummary
import domains.blog.table.blog.BlogTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object BlogPostMutationTable:

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
  ): IO[Option[domains.blog.objects.response.BlogDetail]] =
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
      case true => BlogPostQueryTable.findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
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
