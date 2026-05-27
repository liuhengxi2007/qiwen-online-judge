package domains.blog.table.blog

import cats.effect.IO
import domains.blog.objects.{BlogCommentId, BlogId, BlogVote}
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant

object BlogCommentVoteTable:

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
        deleteCommentVote(connection, blogId, commentId, username).flatMap(_ => BlogPostQueryTable.findById(connection, blogId, username))
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
          case true => BlogPostQueryTable.findById(connection, blogId, username)
        }
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
