package domains.problem.table.problem



import database.utils.ResourceAccessTableSupport.{decodeBaseAccessColumn, parseColumn, parseOptionalColumn}
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.request.ProblemSearchQuery
import domains.problem.objects.{OthersSubmissionAccess, ProblemData, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}
import domains.problem.objects.response.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.access.{ResourceAccessPolicy, ResourceId}
import database.utils.LikePatternSql
import database.utils.UserIdentitySql

import java.sql.{PreparedStatement, ResultSet}

object ProblemTableSupport:

  private def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  def readProblemSummaryBase(resultSet: ResultSet): ProblemSummary =
    ProblemSummary(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
      data = parseColumn("problems.data_name", Option(resultSet.getString("data_name")), ProblemData.parse),
      ready = resultSet.getBoolean("ready"),
      timeLimitMs = parseColumn("problems.time_limit_ms", resultSet.getInt("time_limit_ms"), ProblemTimeLimitMs.parse),
      spaceLimitMb = parseColumn("problems.space_limit_mb", resultSet.getInt("space_limit_mb"), ProblemSpaceLimitMb.parse),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      othersSubmissionAccess =
        parseOptionalColumn("problems.others_submission_access", resultSet.getString("others_submission_access"), decodeOthersSubmissionAccessColumn),
      creator = readUserIdentity(resultSet, "creator"),
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
      timeLimitMs = parseColumn("problems.time_limit_ms", resultSet.getInt("time_limit_ms"), ProblemTimeLimitMs.parse),
      spaceLimitMb = parseColumn("problems.space_limit_mb", resultSet.getInt("space_limit_mb"), ProblemSpaceLimitMb.parse),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      othersSubmissionAccess =
        parseOptionalColumn("problems.others_submission_access", resultSet.getString("others_submission_access"), decodeOthersSubmissionAccessColumn),
      creator = readUserIdentity(resultSet, "creator"),
      canManage = false,
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def bindVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    startIndex: Int
  ): Int =
    statement.setBoolean(startIndex, actor.siteManager || actor.problemManager)
    statement.setString(startIndex + 1, actor.username.value)
    statement.setString(startIndex + 2, actor.username.value)
    statement.setBoolean(startIndex + 3, actor.siteManager || actor.problemManager)
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

  def bindListQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    query: Option[ProblemSearchQuery],
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    val nextIndex = bindVisibilityQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, query, startIndex = nextIndex)
    pageSize.foreach(statement.setInt(afterSearchIndex, _))
    offset.foreach(statement.setInt(afterSearchIndex + 1, _))

  def bindSuggestionQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    query: ProblemSearchQuery
  ): Unit =
    val nextIndex = bindVisibilityQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, Some(query), startIndex = nextIndex)
    val searchPattern = LikePatternSql.fromRaw(query.value)
    statement.setString(afterSearchIndex, searchPattern.raw)
    statement.setString(afterSearchIndex + 1, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 2, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 3, searchPattern.containsPattern)

  def encodeOthersSubmissionAccessColumn(value: OthersSubmissionAccess): String =
    value match
      case OthersSubmissionAccess.None => "none"
      case OthersSubmissionAccess.Summary => "summary"
      case OthersSubmissionAccess.Detail => "detail"

  def decodeOthersSubmissionAccessColumn(value: String): Option[OthersSubmissionAccess] =
    OthersSubmissionAccess.parse(value).toOption

  def bindContainingProblemSetVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.siteManager || actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)

  def toResourceId(problemId: ProblemId): ResourceId =
    ResourceId(problemId.value)
