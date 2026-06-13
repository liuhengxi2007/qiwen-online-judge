package domains.blog.table.blog

import cats.effect.IO

import java.sql.Connection

/** 博客 domain 表初始化入口，委托 BlogTableSchema 创建和迁移表结构。 */
object BlogTable:

  /** 初始化博客、关联、投票和评论相关表结构。 */
  def initialize(connection: Connection): IO[Unit] =
    BlogTableSchema.initialize(connection)
