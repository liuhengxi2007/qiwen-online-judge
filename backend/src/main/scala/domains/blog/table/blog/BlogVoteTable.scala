package domains.blog.table.blog

import cats.effect.IO
import domains.blog.model.{BlogId, BlogVote}
import domains.blog.model.response.BlogDetail
import domains.blog.table.blog.BlogTableSupport.*
import domains.user.model.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant

object BlogVoteTable:

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
        deleteVote(connection, blogId, username).flatMap(_ => BlogPostQueryTable.findById(connection, blogId, username))
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
          case true => BlogPostQueryTable.findById(connection, blogId, username)
        }
    }

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
