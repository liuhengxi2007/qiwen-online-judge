package domains.problem.table

import domains.auth.model.AuthUser
import domains.auth.table.UserIdentityTableSupport.readUserIdentity
import domains.problem.model.{OthersSubmissionAccess, ProblemData, ProblemDetail, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemSuggestion, ProblemSummary, ProblemTimeLimitMs, ProblemTitle}
import domains.shared.access.{BaseAccess, ResourceAccessPolicy, ResourceId, ResourceAccessTableSupport}
import domains.shared.access.ResourceAccessTableSupport.{parseColumn, parseOptionalColumn, policyFrom, sanitizePolicy, toLegacyVisibility, missingInsertResult}

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

  def bindVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthUser,
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    statement.setBoolean(1, actor.siteManager || actor.problemManager)
    statement.setString(2, actor.username.value)
    statement.setString(3, actor.username.value)
    statement.setBoolean(4, actor.siteManager || actor.problemManager)
    statement.setString(5, actor.username.value)
    statement.setString(6, actor.username.value)
    pageSize.foreach(statement.setInt(10, _))
    offset.foreach(statement.setInt(11, _))

  def bindListQuery(
    statement: PreparedStatement,
    actor: AuthUser,
    query: Option[String],
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    bindVisibilityQuery(statement, actor, pageSize, offset)
    val normalizedQuery = query.map(_.trim).filter(_.nonEmpty)
    val likeQuery = normalizedQuery.map(value => s"%$value%").getOrElse("")
    statement.setBoolean(7, normalizedQuery.nonEmpty)
    statement.setString(8, likeQuery)
    statement.setString(9, likeQuery)

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
