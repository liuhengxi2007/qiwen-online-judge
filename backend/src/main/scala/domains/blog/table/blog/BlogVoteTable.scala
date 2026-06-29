package domains.blog.table.blog

import cats.effect.IO
import domains.blog.objects.{BlogId, BlogVote}
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant

/** 博客投票表访问对象，负责写入、切换和撤销当前用户的博客投票。 */
object BlogVoteTable:

  private val upsertVoteSQL: String =
    s"""
      |insert into blog_votes (blog_id, username, vote, created_at, updated_at)
      |select b.id, ?, ?, ?, ?
      |from blogs b
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |on conflict (blog_id, username)
      |do update set vote = excluded.vote,
      |              updated_at = excluded.updated_at
      |""".stripMargin

  /** 对可见博客投票；重复同向投票会撤销，成功后返回更新后的博客详情。 */
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
            bindBlogVisibleToViewer(statement, 6, username)
            val updatedRows = statement.executeUpdate()
            updatedRows > 0
          finally statement.close()
        }.flatMap {
          case false => IO.pure(None)
          case true => BlogPostQueryTable.findById(connection, blogId, username)
        }
    }

  private val findViewerVoteSQL: String =
    s"""
      |select bv.vote
      |from blog_votes bv
      |join blogs b on b.id = bv.blog_id
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |  and bv.username = ?
      |""".stripMargin

  private def findCurrentVote(connection: Connection, blogId: BlogId, username: Username): IO[Option[BlogVote]] =
    IO.blocking {
      val statement = connection.prepareStatement(findViewerVoteSQL)
      try
        statement.setLong(1, blogId.value)
        val nextIndex = bindBlogVisibleToViewer(statement, 2, username)
        statement.setString(nextIndex, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then decodeBlogVoteColumn(resultSet.getString("vote"))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteVoteSQL: String =
    s"""
      |delete from blog_votes
      |where blog_id = (
      |  select b.id
      |  from blogs b
      |  where b.public_id = ?
      |    and ${blogVisibleToViewerPredicate("b")}
      |)
      |  and username = ?
      |""".stripMargin

  private def deleteVote(connection: Connection, blogId: BlogId, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteVoteSQL)
      try
        statement.setLong(1, blogId.value)
        val nextIndex = bindBlogVisibleToViewer(statement, 2, username)
        statement.setString(nextIndex, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
