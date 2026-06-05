package domains.contest.table.contest

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.ProblemId

import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.Instant

object ContestProblemVisibilityTable:

  private val visibleContestPredicate: String =
    """
      |(
      |  ? = true
      |  or c.base_access = 'public'
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_registrations cr
      |    where cr.contest_id = c.id
      |      and cr.username = ?
      |  )
      |)
      |""".stripMargin

  private val manageableContestPredicate: String =
    """
      |(
      |  ? = true
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = c.id
      |      and cag.grant_role = 'manager'
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = c.id
      |      and cag.grant_role = 'manager'
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |)
      |""".stripMargin

  private val hasVisibleUnfinishedContestHidingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and c.end_at >= now()
      |  and $visibleContestPredicate
      |  and not $manageableContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleUnfinishedContestHidingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    existsByProblem(connection, actor, problemId, hasVisibleUnfinishedContestHidingProblemSQL, includeManagePredicate = true)

  def hasVisibleUnstartedContestHidingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    hasVisibleUnfinishedContestHidingProblem(connection, actor, problemId)

  private val hasVisibleUnfinishedContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and c.end_at >= now()
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleUnfinishedContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    existsByProblem(connection, actor, problemId, hasVisibleUnfinishedContestContainingProblemSQL, includeManagePredicate = false)

  private val hasVisibleEndedContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and c.end_at < now()
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleEndedContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    existsByProblem(connection, actor, problemId, hasVisibleEndedContestContainingProblemSQL, includeManagePredicate = false)

  private val hasVisibleStartedContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and c.start_at <= now()
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleStartedContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    existsByProblem(connection, actor, problemId, hasVisibleStartedContestContainingProblemSQL, includeManagePredicate = false)

  private val hasManageableContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and $manageableContestPredicate
      |limit 1
      |""".stripMargin

  def hasManageableContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasManageableContestContainingProblemSQL)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        bindManageableContestAccess(statement, afterProblem, actor)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val hasActiveContestContainingSubmissionSQL: String =
    """
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and ? >= c.start_at
      |  and ? <= c.end_at
      |limit 1
      |""".stripMargin

  def hasActiveContestContainingSubmission(
    connection: Connection,
    problemId: ProblemId,
    submittedAt: Instant
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasActiveContestContainingSubmissionSQL)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        bindSubmittedAtRange(statement, afterProblem, submittedAt)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val hasVisibleActiveContestContainingSubmissionSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and ? >= c.start_at
      |  and ? <= c.end_at
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleActiveContestContainingSubmission(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId,
    submittedAt: Instant
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasVisibleActiveContestContainingSubmissionSQL)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        val afterSubmittedAt = bindSubmittedAtRange(statement, afterProblem, submittedAt)
        bindContestAccess(statement, afterSubmittedAt, actor)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val hasRegisteredContestContainingSubmissionSQL: String =
    """
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |join contest_registrations cr on cr.contest_id = c.id
      |where cp.problem_id = ?
      |  and ? >= c.start_at
      |  and ? <= c.end_at
      |  and cr.username = ?
      |  and cr.registered_at <= c.start_at
      |limit 1
      |""".stripMargin

  def hasRegisteredContestContainingSubmission(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId,
    submittedAt: Instant
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasRegisteredContestContainingSubmissionSQL)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        val afterSubmittedAt = bindSubmittedAtRange(statement, afterProblem, submittedAt)
        bindString(statement, afterSubmittedAt, actor.username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val hasVisibleActiveContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |where cp.problem_id = ?
      |  and c.start_at <= now()
      |  and c.end_at >= now()
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasVisibleActiveContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    existsByProblem(connection, actor, problemId, hasVisibleActiveContestContainingProblemSQL, includeManagePredicate = false)

  private val hasEligibleVisibleActiveContestContainingProblemSQL: String =
    s"""
      |select 1
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |join contest_registrations cr on cr.contest_id = c.id
      |where cp.problem_id = ?
      |  and c.start_at <= now()
      |  and c.end_at >= now()
      |  and cr.username = ?
      |  and cr.registered_at <= c.start_at
      |  and $visibleContestPredicate
      |limit 1
      |""".stripMargin

  def hasEligibleVisibleActiveContestContainingProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasEligibleVisibleActiveContestContainingProblemSQL)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        val afterRegistrant = bindString(statement, afterProblem, actor.username.value)
        bindContestAccess(statement, afterRegistrant, actor)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def existsByProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId,
    sql: String,
    includeManagePredicate: Boolean
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        val afterProblem = bindProblemId(statement, 1, problemId)
        val afterVisible = bindVisibleContestAccess(statement, afterProblem, actor)
        if includeManagePredicate then bindManageableContestAccess(statement, afterVisible, actor) else afterVisible
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private def bindProblemId(statement: PreparedStatement, index: Int, problemId: ProblemId): Int =
    statement.setObject(index, problemId.value)
    index + 1

  private def bindSubmittedAtRange(statement: PreparedStatement, startIndex: Int, submittedAt: Instant): Int =
    statement.setTimestamp(startIndex, Timestamp.from(submittedAt))
    statement.setTimestamp(startIndex + 1, Timestamp.from(submittedAt))
    startIndex + 2

  private def bindContestAccess(statement: PreparedStatement, startIndex: Int, actor: AuthenticatedUser): Int =
    bindVisibleContestAccess(statement, startIndex, actor)

  private def bindVisibleContestAccess(statement: PreparedStatement, startIndex: Int, actor: AuthenticatedUser): Int =
    val afterGlobalOverride = bindBoolean(statement, startIndex, actor.siteManager || actor.contestManager)
    val afterUserGrant = bindString(statement, afterGlobalOverride, actor.username.value)
    val afterGroupGrant = bindString(statement, afterUserGrant, actor.username.value)
    bindString(statement, afterGroupGrant, actor.username.value)

  private def bindManageableContestAccess(statement: PreparedStatement, startIndex: Int, actor: AuthenticatedUser): Int =
    val afterGlobalOverride = bindBoolean(statement, startIndex, actor.siteManager || actor.contestManager)
    val afterUserGrant = bindString(statement, afterGlobalOverride, actor.username.value)
    bindString(statement, afterUserGrant, actor.username.value)

  private def bindBoolean(statement: PreparedStatement, index: Int, value: Boolean): Int =
    statement.setBoolean(index, value)
    index + 1

  private def bindString(statement: PreparedStatement, index: Int, value: String): Int =
    statement.setString(index, value)
    index + 1
