package domains.problem.table.problem



import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.request.ProblemSearchQuery
import domains.problem.objects.{OtherUserSubmissionAccess, ProblemData, ProblemId, ProblemSlug, ProblemStatementText, ProblemTitle}
import domains.problem.objects.response.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}
import database.utils.LikePatternSql
import database.utils.{UserIdentityRow, UserIdentitySql}

import java.sql.{PreparedStatement, ResultSet}

/** 题目表读写辅助；集中处理列解析、访问策略列编解码、ResultSet 映射和 SQL 参数绑定。 */
object ProblemTableSupport:

  /** 解析必填字符串列；数据库中非法领域值视为不可恢复的数据错误。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 解析必填整数列；用于数据库枚举或数值领域类型回读。 */
  def parseColumn[A](columnName: String, rawValue: Int, parse: Int => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 解析可空字符串列；解析函数负责定义 None 的业务含义。 */
  def parseColumn[A](columnName: String, rawValue: Option[String], parse: Option[String] => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 解析本应有值的可选领域列；None 表示数据库中存在非法旧值。 */
  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  /** 处理 insert returning 没有返回行的异常分支。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  /** 将基础访问策略编码为 problems.base_access 列值。 */
  def encodeBaseAccessColumn(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"

  /** 从 problems.base_access 列值解析基础访问策略。 */
  def decodeBaseAccessColumn(value: String): Option[BaseAccess] =
    BaseAccess.parse(value).toOption

  private def userIdentityFromRow(row: UserIdentityRow): UserIdentity =
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix).map(userIdentityFromRow)

  /** 从当前 ResultSet 行读取题目摘要基础字段；访问策略 grants 由查询层另行补齐。 */
  def readProblemSummaryBase(resultSet: ResultSet): ProblemSummary =
    ProblemSummary(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
      data = parseColumn("problems.data_name", Option(resultSet.getString("data_name")), ProblemData.parse),
      ready = resultSet.getBoolean("ready"),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      otherUserSubmissionAccess =
        parseOptionalColumn("problems.other_user_submission_access", resultSet.getString("other_user_submission_access"), decodeOtherUserSubmissionAccessColumn),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从当前 ResultSet 行读取题目搜索建议。 */
  def readProblemSuggestion(resultSet: ResultSet): ProblemSuggestion =
    ProblemSuggestion(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse)
    )

  /** 从当前 ResultSet 行读取题目详情基础字段；canManage 默认 false，授权 grants 由查询层补齐。 */
  def readProblemDetailBase(resultSet: ResultSet): ProblemDetail =
    ProblemDetail(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
      statement = parseColumn("problems.statement_text", resultSet.getString("statement_text"), ProblemStatementText.parse),
      data = parseColumn("problems.data_name", Option(resultSet.getString("data_name")), ProblemData.parse),
      ready = resultSet.getBoolean("ready"),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      otherUserSubmissionAccess =
        parseOptionalColumn("problems.other_user_submission_access", resultSet.getString("other_user_submission_access"), decodeOtherUserSubmissionAccessColumn),
      author = readOptionalUserIdentity(resultSet, "author"),
      canManage = false,
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def bindAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    val afterManagerProblemAccess = bindManagerProblemAccessQuery(statement, actor, startIndex)
    bindNormalProblemAccessQuery(statement, actor, afterManagerProblemAccess)

  private def bindManagerProblemAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    statement.setBoolean(startIndex, actor.problemManager)
    statement.setString(startIndex + 1, actor.username.value)
    statement.setString(startIndex + 2, actor.username.value)
    startIndex + 3

  private def bindNormalProblemAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    val globalProblemOverride = actor.problemManager
    statement.setBoolean(startIndex, globalProblemOverride)
    statement.setString(startIndex + 1, actor.username.value)
    statement.setString(startIndex + 2, actor.username.value)
    statement.setBoolean(startIndex + 3, globalProblemOverride)
    statement.setString(startIndex + 4, actor.username.value)
    statement.setString(startIndex + 5, actor.username.value)
    startIndex + 6

  private def bindSearchQuery(
    statement: PreparedStatement,
    query: Option[ProblemSearchQuery],
    startIndex: Int
  ): Int =
    val likeQuery = query.map(searchQuery => LikePatternSql.fromRaw(searchQuery.value))
    statement.setBoolean(startIndex, query.nonEmpty)
    statement.setString(startIndex + 1, likeQuery.map(_.containsPattern).getOrElse(""))
    statement.setString(startIndex + 2, likeQuery.map(_.containsPattern).getOrElse(""))
    startIndex + 3

  /** 绑定题目列表查询中的访问控制、搜索和分页参数。 */
  def bindListQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    query: Option[ProblemSearchQuery],
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    val nextIndex = bindAccessQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, query, startIndex = nextIndex)
    pageSize.foreach(statement.setInt(afterSearchIndex, _))
    offset.foreach(statement.setInt(afterSearchIndex + 1, _))

  /** 绑定可见题目建议查询参数，包含访问控制和排序匹配条件。 */
  def bindSuggestionQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    query: ProblemSearchQuery
  ): Unit =
    val nextIndex = bindAccessQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, Some(query), startIndex = nextIndex)
    val searchPattern = LikePatternSql.fromRaw(query.value)
    statement.setString(afterSearchIndex, searchPattern.raw)
    statement.setString(afterSearchIndex + 1, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 2, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 3, searchPattern.containsPattern)

  /** 绑定可管理题目建议查询参数，使用管理权限访问谓词。 */
  def bindManageableSuggestionQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    query: ProblemSearchQuery
  ): Unit =
    val nextIndex = bindManagerProblemAccessQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, Some(query), startIndex = nextIndex)
    val searchPattern = LikePatternSql.fromRaw(query.value)
    statement.setString(afterSearchIndex, searchPattern.raw)
    statement.setString(afterSearchIndex + 1, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 2, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 3, searchPattern.containsPattern)

  /** 将他人提交可见级别编码为数据库列值。 */
  def encodeOtherUserSubmissionAccessColumn(value: OtherUserSubmissionAccess): String =
    value match
      case OtherUserSubmissionAccess.None => "none"
      case OtherUserSubmissionAccess.Summary => "summary"
      case OtherUserSubmissionAccess.Detail => "detail"

  /** 从 other_user_submission_access 列值解析他人提交可见级别。 */
  def decodeOtherUserSubmissionAccessColumn(value: String): Option[OtherUserSubmissionAccess] =
    OtherUserSubmissionAccess.parse(value).toOption

  /** 绑定判断题目所属题单是否对调用者可见的查询参数。 */
  def bindContainingProblemSetAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)
