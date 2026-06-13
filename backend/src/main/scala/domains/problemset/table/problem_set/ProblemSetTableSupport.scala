package domains.problemset.table.problem_set



import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.problemset.objects.{ProblemSet, ProblemSetDescription, ProblemSetId, ProblemSetProblemSummary, ProblemSetSlug, ProblemSetTitle}
import domains.problemset.objects.response.ProblemSetSummary
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}
import database.utils.{UserIdentityRow, UserIdentitySql}

import java.sql.{PreparedStatement, ResultSet}

/** 题单表读写辅助对象，集中处理数据库列到领域对象的转换和列表访问绑定。 */
object ProblemSetTableSupport:

  /** 注意：按领域解析函数读取必填列；这里抛异常表示数据库已有非法值，不是可恢复的用户输入错误。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 注意：按可选原始值解析必填业务列；解析失败表示数据库状态异常，不走 HTTP 业务错误。 */
  def parseColumn[A](columnName: String, rawValue: Option[String], parse: Option[String] => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 注意：按 Option 解析函数读取必填列；解析失败表示数据库状态异常，不走 HTTP 业务错误。 */
  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  /** 注意：INSERT RETURNING 没有返回行时抛出内部数据异常，因为正常数据库语义下该分支不可达。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  /** 将访问策略基础可见性编码为数据库列值。 */
  def encodeBaseAccessColumn(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"

  /** 将数据库基础可见性列值解码为领域枚举。 */
  def decodeBaseAccessColumn(value: String): Option[BaseAccess] =
    BaseAccess.parse(value).toOption

  private def userIdentityFromRow(row: UserIdentityRow): UserIdentity =
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix).map(userIdentityFromRow)

  /** 从列表查询行读取题单摘要基础字段，授权由调用方补齐。 */
  def readProblemSetSummaryBase(resultSet: ResultSet): ProblemSetSummary =
    ProblemSetSummary(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从详情查询行读取题单基础字段，题目和授权由调用方补齐。 */
  def readProblemSetDetailBase(resultSet: ResultSet): ProblemSet =
    ProblemSet(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      problems = Nil,
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 使用传入 SQL 读取题单题目摘要列表，保持题单位置顺序。 */
  def listProblemsForSet(connection: java.sql.Connection, problemSetId: ProblemSetId, listProblemsForSetSql: String): IO[List[ProblemSetProblemSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProblemsForSetSql)
      try
        statement.setObject(1, problemSetId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map { _ =>
              ProblemSetProblemSummary(
                id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
                slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
                title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
                position = resultSet.getInt("position")
              )
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  /** 绑定题单列表访问查询参数，包括全局题目管理覆盖和分页参数。 */
  def bindAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    statement.setBoolean(1, actor.problemManager)
    statement.setString(2, actor.username.value)
    statement.setString(3, actor.username.value)
    pageSize.foreach(statement.setInt(4, _))
    offset.foreach(statement.setInt(5, _))
