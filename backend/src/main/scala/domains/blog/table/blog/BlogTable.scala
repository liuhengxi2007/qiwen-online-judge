package domains.blog.table.blog

import cats.effect.IO
import domains.blog.table.blog_access_grant.BlogAccessGrantTable

import java.sql.Connection

/** 博客 domain 表初始化入口，创建博客主体表和 viewer 授权表。 */
object BlogTable:

  /** 初始化博客、关联、投票、评论和 viewer 授权相关表结构。 */
  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- BlogTableSchema.initialize(connection)
      _ <- BlogAccessGrantTable.initialize(connection)
    yield ()
