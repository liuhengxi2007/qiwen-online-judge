package domains.problem.table.utils



import database.utils.ResourceAccessTableSupport.{parseColumn, parseOptionalColumn}
import domains.auth.model.AuthUser
import domains.auth.table.utils.UserIdentityTableSupport.readUserIdentity
import domains.problem.model.{OthersSubmissionAccess, ProblemData, ProblemId, ProblemSearchQuery, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}
import domains.problem.http.response.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.shared.access.{BaseAccess, ResourceAccessPolicy, ResourceId}
import domains.shared.sql.LikePatternSql

import java.sql.{PreparedStatement, ResultSet}

object ProblemTableSupport:

  val hasVisibleContainingProblemSetSql: String =
    """
      |select 1
      |from problem_set_problems psp
      |join problem_sets ps on ps.id = psp.problem_set_id
      |where psp.problem_id = ?
      |  and (
      |    ? = true
      |    or ps.base_access = 'public'
      |    or exists (
      |      select 1
      |      from resource_access_grants rag
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user'
      |        and rag.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from resource_access_grants rag
      |      join user_groups ug on ug.slug = rag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user_group'
      |        and ugm.username = ?
      |    )
      |  )
      |limit 1
      |""".stripMargin

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
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), BaseAccess.fromDatabase), Nil, Nil),
      othersSubmissionAccess =
        parseOptionalColumn("problems.others_submission_access", resultSet.getString("others_submission_access"), OthersSubmissionAccess.fromDatabase),
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
        ResourceAccessPolicy(parseOptionalColumn("problems.base_access", resultSet.getString("base_access"), BaseAccess.fromDatabase), Nil, Nil),
      othersSubmissionAccess =
        parseOptionalColumn("problems.others_submission_access", resultSet.getString("others_submission_access"), OthersSubmissionAccess.fromDatabase),
      creator = readUserIdentity(resultSet, "creator"),
      canManage = false,
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def bindVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthUser,
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
    actor: AuthUser,
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
    actor: AuthUser,
    query: ProblemSearchQuery
  ): Unit =
    val nextIndex = bindVisibilityQuery(statement, actor, startIndex = 1)
    val afterSearchIndex = bindSearchQuery(statement, Some(query), startIndex = nextIndex)
    val searchPattern = LikePatternSql.fromRaw(query.value)
    statement.setString(afterSearchIndex, searchPattern.raw)
    statement.setString(afterSearchIndex + 1, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 2, searchPattern.prefixPattern)
    statement.setString(afterSearchIndex + 3, searchPattern.containsPattern)

  def bindContainingProblemSetVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.siteManager || actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)

  def toResourceId(problemId: ProblemId): ResourceId =
    ResourceId(problemId.value)
