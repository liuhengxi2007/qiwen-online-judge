package domains.blog.table.blog_access_grant

import cats.effect.IO
import database.utils.AccessGrantSql.*
import domains.blog.objects.BlogId
import shared.objects.access.AccessSubject

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID

/** 博客查看授权表访问对象，负责 viewer-only 授权的读取、替换和清理。 */
object BlogAccessGrantTable:

  /** 初始化博客查看授权表结构和历史授权迁移。 */
  def initialize(connection: Connection): IO[Unit] =
    BlogAccessGrantTableSchema.initialize(connection)

  private val listForBlogSQL: String =
    """
      |select subject_kind, subject_key
      |from blog_access_grants
      |where blog_id = ?
      |order by subject_kind asc, subject_key asc
      |""".stripMargin

  /** 读取指定博客的 viewer 授权主体列表，按主体类型和 key 稳定排序。 */
  def listForBlog(connection: Connection, internalBlogId: UUID): IO[List[AccessSubject]] =
    IO.blocking {
      val statement = connection.prepareStatement(listForBlogSQL)
      try
        statement.setObject(1, internalBlogId)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readSubject(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val findInternalIdSQL: String =
    """
      |select id
      |from blogs
      |where public_id = ?
      |""".stripMargin

  /** 按公开博客 id 查找内部 uuid，用于授权替换。 */
  def findInternalId(connection: Connection, blogId: BlogId): IO[Option[UUID]] =
    IO.blocking {
      val statement = connection.prepareStatement(findInternalIdSQL)
      try
        statement.setLong(1, blogId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(resultSet.getObject("id", classOf[UUID])) else None
        finally resultSet.close()
      finally statement.close()
    }

  /** 用传入授权主体完整替换指定博客的 viewer 授权集合。 */
  def replaceForBlog(connection: Connection, internalBlogId: UUID, grants: List[AccessSubject]): IO[Unit] =
    for
      _ <- deleteAllForInternalBlog(connection, internalBlogId)
      _ <- insertGrants(connection, internalBlogId, grants)
    yield ()

  /** 删除某个博客的全部 viewer 授权记录，通常用于博客删除前清理。 */
  def deleteAllForBlog(connection: Connection, blogId: BlogId): IO[Unit] =
    findInternalId(connection, blogId).flatMap {
      case Some(internalBlogId) => deleteAllForInternalBlog(connection, internalBlogId)
      case None => IO.unit
    }

  private val deleteAllForInternalBlogSQL: String =
    """
      |delete from blog_access_grants
      |where blog_id = ?
      |""".stripMargin

  private def deleteAllForInternalBlog(connection: Connection, internalBlogId: UUID): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteAllForInternalBlogSQL)
      try
        statement.setObject(1, internalBlogId)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val insertGrantSQL: String =
    """
      |insert into blog_access_grants (blog_id, subject_kind, subject_key, created_at)
      |values (?, ?, ?, ?)
      |on conflict (blog_id, subject_kind, subject_key) do nothing
      |""".stripMargin

  private def insertGrants(connection: Connection, internalBlogId: UUID, grants: List[AccessSubject]): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertGrantSQL)
      try
        grants
          .distinctBy(subject => subjectIdentity(subject))
          .foreach { subject =>
            statement.setObject(1, internalBlogId)
            statement.setString(2, encodeSubjectKindColumn(subject))
            statement.setString(3, encodeSubjectKeyColumn(subject))
            statement.setTimestamp(4, Timestamp.from(now))
            statement.addBatch()
          }
        statement.executeBatch()
        ()
      finally statement.close()
    }

  private def readSubject(resultSet: ResultSet): AccessSubject =
    decodeSubjectColumns(resultSet.getString("subject_kind"), resultSet.getString("subject_key"), "blog access")
