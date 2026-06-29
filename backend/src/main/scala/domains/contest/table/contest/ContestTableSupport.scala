package domains.contest.table.contest

import cats.effect.IO
import domains.contest.objects.*
import domains.contest.objects.response.{ContestRegistrant, ContestRegistrationStatus, ContestSummary}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import database.utils.{UserIdentityRow, UserIdentitySql}
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.sql.ResultSet

/** 比赛表读写辅助对象，集中处理数据库列到领域对象的转换。 */
object ContestTableSupport:

  /** 注意：按领域解析函数读取必填列；这里抛异常表示数据库已有非法值，不是可恢复的用户输入错误。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
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

  /** 从当前 ResultSet 行读取比赛基础字段，赛题和授权由调用方后续补齐。 */
  def readContestBase(resultSet: ResultSet): Contest =
    Contest(
      id = ContestId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("contests.slug", resultSet.getString("slug"), ContestSlug.parse),
      title = parseColumn("contests.title", resultSet.getString("title"), ContestTitle.parse),
      description = parseColumn("contests.description", resultSet.getString("description"), ContestDescription.parse),
      startAt = resultSet.getTimestamp("start_at").toInstant,
      endAt = resultSet.getTimestamp("end_at").toInstant,
      problems = Nil,
      accessPolicy = ResourceAccessPolicy(
        parseOptionalColumn("contests.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn),
        Nil,
        Nil
      ),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从报名查询行读取报名用户身份。 */
  def readContestRegistrant(resultSet: ResultSet): ContestRegistrant =
    ContestRegistrant(
      /** 注意：报名列表 SQL 使用内连接读取用户身份；缺失身份表示查询/数据不变量被破坏。 */
      user = readOptionalUserIdentity(resultSet, "user").getOrElse(
        throw IllegalStateException("Contest registration row is missing user identity")
      )
    )

  /** 从列表查询行读取比赛摘要基础字段，授权和报名状态由调用方补齐。 */
  def readContestSummaryBase(resultSet: ResultSet): ContestSummary =
    ContestSummary(
      id = ContestId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("contests.slug", resultSet.getString("slug"), ContestSlug.parse),
      title = parseColumn("contests.title", resultSet.getString("title"), ContestTitle.parse),
      description = parseColumn("contests.description", resultSet.getString("description"), ContestDescription.parse),
      startAt = resultSet.getTimestamp("start_at").toInstant,
      endAt = resultSet.getTimestamp("end_at").toInstant,
      accessPolicy = ResourceAccessPolicy(
        parseOptionalColumn("contests.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn),
        Nil,
        Nil
      ),
      registrationStatus = ContestRegistrationStatus.notRegistered,
      canViewDetail = false,
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 使用传入 SQL 读取比赛题目摘要列表，保持题目位置顺序。 */
  def listProblemsForContest(connection: java.sql.Connection, contestId: ContestId, listProblemsForContestSql: String): IO[List[ContestProblemSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProblemsForContestSql)
      try
        statement.setObject(1, contestId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map { _ =>
              ContestProblemSummary(
                id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
                slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
                title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
                position = resultSet.getInt("position"),
                alias = parseColumn("contest_problems.alias", resultSet.getString("alias"), ContestProblemAlias.parse)
              )
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }
