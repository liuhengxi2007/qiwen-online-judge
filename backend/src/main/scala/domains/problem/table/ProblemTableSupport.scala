package domains.problem.table

import domains.auth.model.{AuthUser, Username}
import domains.problem.model.{OthersSubmissionAccess, ProblemData, ProblemDetail, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemSummary, ProblemTimeLimitMs, ProblemTitle}
import domains.shared.access.{AccessSubject, BaseAccess, ResourceAccessGrant, ResourceAccessPolicy, ResourceId}

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
      creatorUsername = Username.canonical(resultSet.getString("creator_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
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
      creatorUsername = Username.canonical(resultSet.getString("creator_username")),
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
    pageSize.foreach(statement.setInt(7, _))
    offset.foreach(statement.setInt(8, _))

  def bindContainingProblemSetVisibilityQuery(
    statement: PreparedStatement,
    actor: AuthUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.siteManager || actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)

  def policyFrom(
    baseAccess: BaseAccess,
    viewerGrants: List[ResourceAccessGrant],
    managerGrants: List[ResourceAccessGrant]
  ): ResourceAccessPolicy =
    ResourceAccessPolicy(baseAccess = baseAccess, viewerGrants = viewerGrants.map(_.subject), managerGrants = managerGrants.map(_.subject))

  def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject))),
      managerGrants = policy.managerGrants.distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
    )

  def toResourceId(problemId: ProblemId): ResourceId =
    ResourceId(problemId.value)

  def toLegacyVisibility(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Public => "public"
      case BaseAccess.OwnerOnly => "private"

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
