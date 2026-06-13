package domains.blog.table.blog

import cats.effect.IO
import domains.blog.objects.BlogId
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}
import java.time.Instant

/** 博客与题目关联写操作表访问对象，负责直接关联、提交审核、接受和解除关联。 */
object BlogProblemLinkMutationTable:

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

  /** 管理员将公开博客直接关联到题目，已存在关联时置为 accepted 并刷新操作者和时间。 */
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

  /** 博客作者提交公开博客到题目待审队列；已有关联时不改变状态并返回 false。 */
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

  /** 管理员接受 pending 关联，将其置为 accepted 并记录审核操作者。 */
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

  /** 删除博客与题目的关联，不区分 pending 或 accepted。 */
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
