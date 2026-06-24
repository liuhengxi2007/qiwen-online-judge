package domains.blog.table.blog

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogId, BlogTitle}
import domains.blog.objects.response.{BlogCommentSummary, BlogDetail}
import domains.blog.table.blog.BlogTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

/** 博客评论表访问对象，负责评论/回复写入、更新删除、列表读取和通知上下文查询。 */
object BlogCommentTable:

  private val insertCommentSQL: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), b.id, null, ?, ?, ?, ?
      |from blogs b
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |returning public_id
      |""".stripMargin

  private val insertReplySQL: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), b.id, parent_comment.id, ?, ?, ?, ?
      |from blogs b
      |join blog_comments parent_comment on parent_comment.blog_id = b.id and parent_comment.public_id = ?
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |returning public_id
      |""".stripMargin

  /** 在当前用户可见博客下创建评论/回复，返回更新后的博客详情和新评论公开 id。 */
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
            bindBlogVisibleToViewer(statement, 8, authorUsername)
          case None =>
            statement.setString(2, authorUsername.value)
            statement.setString(3, content.value)
            statement.setTimestamp(4, Timestamp.from(now))
            statement.setTimestamp(5, Timestamp.from(now))
            statement.setLong(6, blogId.value)
            bindBlogVisibleToViewer(statement, 7, authorUsername)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(BlogCommentId(resultSet.getLong("public_id")))
          else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case None => IO.pure(None)
      case Some(createdCommentId) =>
        BlogPostQueryTable.findById(connection, blogId, authorUsername).map(_.map(_ -> createdCommentId))
    }

  private val findCommentNotificationBlogContextSQL: String =
    """
      |select b.title as blog_title,
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

  /** 读取创建评论后的通知上下文，包含博客作者、触发评论内容和祖先评论作者链。 */
  def findCommentNotificationContext(
    connection: Connection,
    blogId: BlogId,
    triggerCommentId: BlogCommentId
  ): IO[Option[(BlogTitle, Username, String, List[(BlogCommentId, Username)])]] =
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
                    BlogCommentId(resultSet.getLong("public_id")) ->
                      Username.canonical(resultSet.getString("author_username"))
                  )
                  .toList
              finally resultSet.close()
            finally statement.close()
          }
    yield maybeBlogContext.map { case (blogTitle, blogAuthorUsername, triggerCommentContent) =>
      (blogTitle, blogAuthorUsername, triggerCommentContent, ancestors)
    }

  private val updateCommentSQL: String =
    s"""
      |update blog_comments c
      |set content = ?,
      |    updated_at = ?
      |from blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin

  /** 仅允许评论作者在可见博客下更新评论内容，成功后返回更新后的博客详情。 */
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
        val nextIndex = bindBlogVisibleToViewer(statement, 4, actorUsername)
        statement.setLong(nextIndex, commentId.value)
        statement.setString(nextIndex + 1, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => BlogPostQueryTable.findById(connection, blogId, actorUsername)
      case false => IO.pure(None)
    }

  private val deleteCommentSQL: String =
    s"""
      |delete from blog_comments c
      |using blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin

  /** 仅允许评论作者在可见博客下删除评论，成功后返回更新后的博客详情。 */
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
        val nextIndex = bindBlogVisibleToViewer(statement, 2, actorUsername)
        statement.setLong(nextIndex, commentId.value)
        statement.setString(nextIndex + 1, actorUsername.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }.flatMap {
      case true => BlogPostQueryTable.findById(connection, blogId, actorUsername)
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
      |${UserIdentitySql.joinUserProfiles("c.author_username", "au")}
      |left join (
      |  select comment_id,
      |         sum(case when vote = 'up' then 1 when vote = 'down' then -1 else 0 end)::int as score
      |  from blog_comment_votes
      |  group by comment_id
      |) cvs on cvs.comment_id = c.id
      |left join blog_comment_votes viewer_vote on viewer_vote.comment_id = c.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and ${blogVisibleToViewerPredicate("b")}
      |order by c.public_id asc
      |""".stripMargin

  /** 读取博客评论列表，按博客可见性过滤并补充当前用户对每条评论的投票。 */
  def listComments(connection: Connection, blogId: BlogId, viewerUsername: Username): IO[List[BlogCommentSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listCommentsSQL)
      try
        statement.setString(1, viewerUsername.value)
        statement.setLong(2, blogId.value)
        bindBlogVisibleToViewer(statement, 3, viewerUsername)
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
