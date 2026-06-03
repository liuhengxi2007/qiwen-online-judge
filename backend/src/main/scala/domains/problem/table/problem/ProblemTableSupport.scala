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

object ProblemTableSupport:

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseColumn[A](columnName: String, rawValue: Int, parse: Int => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseColumn[A](columnName: String, rawValue: Option[String], parse: Option[String] => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def encodeBaseAccessColumn(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"

  def decodeBaseAccessColumn(value: String): Option[BaseAccess] =
    BaseAccess.parse(value).toOption

  private def userIdentityFromRow(row: UserIdentityRow): UserIdentity =
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix).map(userIdentityFromRow)

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

  def readProblemSuggestion(resultSet: ResultSet): ProblemSuggestion =
    ProblemSuggestion(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse)
    )

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
    val afterVisibleUnfinishedContest = bindVisibleContestAccessQuery(statement, actor, afterManagerProblemAccess)
    val afterNormalProblemAccess = bindNormalProblemAccessQuery(statement, actor, afterVisibleUnfinishedContest)
    bindVisibleContestAccessQuery(statement, actor, afterNormalProblemAccess)

  private def bindManagerProblemAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    statement.setBoolean(startIndex, actor.siteManager || actor.problemManager)
    statement.setString(startIndex + 1, actor.username.value)
    statement.setString(startIndex + 2, actor.username.value)
    startIndex + 3

  private def bindNormalProblemAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    val globalProblemOverride = actor.siteManager || actor.problemManager
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

  private def bindString(statement: PreparedStatement, index: Int, value: String): Int =
    statement.setString(index, value)
    index + 1

  private def bindVisibleContestAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    statement.setBoolean(startIndex, actor.siteManager || actor.contestManager)
    statement.setString(startIndex + 1, actor.username.value)
    statement.setString(startIndex + 2, actor.username.value)
    statement.setString(startIndex + 3, actor.username.value)
    startIndex + 4

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

  def encodeOtherUserSubmissionAccessColumn(value: OtherUserSubmissionAccess): String =
    value match
      case OtherUserSubmissionAccess.None => "none"
      case OtherUserSubmissionAccess.Summary => "summary"
      case OtherUserSubmissionAccess.Detail => "detail"

  def decodeOtherUserSubmissionAccessColumn(value: String): Option[OtherUserSubmissionAccess] =
    OtherUserSubmissionAccess.parse(value).toOption

  def bindContainingProblemSetAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.siteManager || actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)
