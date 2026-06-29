package domains.contest.table.contest

import cats.effect.IO
import cats.syntax.all.*
import database.utils.UserIdentitySql
import domains.contest.objects.{ContestId, ContestPenaltyMillis, ContestProblemAlias, ContestProblemSummary, ContestRank, ContestScore}
import domains.contest.objects.response.{ContestRanklistItem, ContestRanklistProblemResult}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.PageResponse

import java.sql.{Connection, ResultSet}
import java.util.UUID

/** 比赛榜单表访问对象，负责从提交记录计算榜单、逐题结果和评分快照。 */
object ContestRanklistTable:

  /** 评分所需的比赛参与者榜单记录，不包含逐题明细。 */
  final case class RatingSnapshotParticipantRecord(
    username: Username,
    rank: ContestRank,
    totalScore: ContestScore,
    penaltyMillis: ContestPenaltyMillis
  )

  /** 分页读取比赛榜单；canViewAllSubmissionDetails 控制是否显示他人提交详情入口。 */
  def listForContest(
    connection: Connection,
    contestId: ContestId,
    viewerUsername: Username,
    canViewAllSubmissionDetails: Boolean,
    page: Int,
    pageSize: Int
  ): IO[PageResponse[ContestRanklistItem]] =
    for
      totalItems <- countRows(connection, contestId)
      items <- readRows(connection, contestId, viewerUsername, canViewAllSubmissionDetails, page, pageSize)
    yield PageResponse(items = items, page = page, pageSize = pageSize, totalItems = totalItems)

  private val ratingSnapshotRowsSql: String =
    """
      |with per_problem_best as (
      |  select
      |    cr.username,
      |    cp.problem_id,
      |    best_submission.score,
      |    best_submission.submitted_at,
      |    c.start_at
      |  from contest_registrations cr
      |  join contests c on c.id = cr.contest_id
      |  left join contest_problems cp on cp.contest_id = c.id
      |  left join lateral (
      |    select
      |      coalesce(s.score, 0) as score,
      |      s.submitted_at
      |    from submissions s
      |    where s.problem_id = cp.problem_id
      |      and s.contest_id = c.id
      |      and s.submitter_username = cr.username
      |    order by coalesce(s.score, 0) desc, s.submitted_at asc, s.public_id asc
      |    limit 1
      |  ) best_submission on true
      |  where cr.contest_id = ?
      |),
      |user_scores as (
      |  select
      |    username,
      |    coalesce(sum(score), 0) as total_score,
      |    coalesce(sum(
      |      case
      |        when submitted_at is null then 0
      |        else floor(extract(epoch from (submitted_at - start_at)) * 1000)::bigint
      |      end
      |    ), 0) as penalty_millis
      |  from per_problem_best
      |  group by username
      |),
      |ranked_scores as (
      |  select
      |    rank() over (order by total_score desc, penalty_millis asc) as contest_rank,
      |    username,
      |    total_score,
      |    penalty_millis
      |  from user_scores
      |)
      |select contest_rank, username, total_score, penalty_millis
      |from ranked_scores
      |order by contest_rank asc, username asc
      |""".stripMargin

  /** 读取评分使用的完整参赛者排名快照，按排名和用户名稳定排序。 */
  def listRatingSnapshotParticipants(
    connection: Connection,
    contestId: ContestId
  ): IO[List[RatingSnapshotParticipantRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(ratingSnapshotRowsSql)
      try
        statement.setObject(1, contestId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readRatingSnapshotParticipant(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val countRowsSql: String =
    """
      |select count(*) as total_items
      |from contest_registrations cr
      |where cr.contest_id = ?
      |  and not exists (
      |    select 1
      |    from auth_accounts aa
      |    where aa.username = cr.username
      |      and aa.contest_manager = true
      |  )
      |  and not exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = cr.contest_id
      |      and cag.grant_role = 'manager'
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = cr.username
      |  )
      |  and not exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = cr.contest_id
      |      and cag.grant_role = 'manager'
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = cr.username
      |  )
      |""".stripMargin

  private def countRows(connection: Connection, contestId: ContestId): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(countRowsSql)
      try
        statement.setObject(1, contestId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getLong("total_items") else 0L
        finally resultSet.close()
      finally statement.close()
    }

  private val rankedRowsSql: String =
    s"""
      |with per_problem_best as (
      |  select
      |    cr.username,
      |    cp.problem_id,
      |    best_submission.score,
      |    best_submission.submitted_at,
      |    c.start_at
      |  from contest_registrations cr
      |  join contests c on c.id = cr.contest_id
      |  left join contest_problems cp on cp.contest_id = c.id
      |  left join lateral (
      |    select
      |      coalesce(s.score, 0) as score,
      |      s.submitted_at
      |    from submissions s
      |    where s.problem_id = cp.problem_id
      |      and s.contest_id = c.id
      |      and s.submitter_username = cr.username
      |    order by coalesce(s.score, 0) desc, s.submitted_at asc, s.public_id asc
      |    limit 1
      |  ) best_submission on true
      |  where cr.contest_id = ?
      |    and not exists (
      |      select 1
      |      from auth_accounts aa
      |      where aa.username = cr.username
      |        and aa.contest_manager = true
      |    )
      |    and not exists (
      |      select 1
      |      from contest_access_grants cag
      |      where cag.contest_id = c.id
      |        and cag.grant_role = 'manager'
      |        and cag.subject_kind = 'user'
      |        and cag.subject_key = cr.username
      |    )
      |    and not exists (
      |      select 1
      |      from contest_access_grants cag
      |      join user_groups ug on ug.slug = cag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where cag.contest_id = c.id
      |        and cag.grant_role = 'manager'
      |        and cag.subject_kind = 'user_group'
      |        and ugm.username = cr.username
      |    )
      |),
      |user_scores as (
      |  select
      |    username,
      |    coalesce(sum(score), 0) as total_score,
      |    coalesce(sum(
      |      case
      |        when submitted_at is null then 0
      |        else floor(extract(epoch from (submitted_at - start_at)) * 1000)::bigint
      |      end
      |    ), 0) as penalty_millis
      |  from per_problem_best
      |  group by username
      |),
      |ranked_scores as (
      |  select
      |    rank() over (order by total_score desc, penalty_millis asc, username asc) as contest_rank,
      |    username,
      |    total_score,
      |    penalty_millis
      |  from user_scores
      |)
      |select
      |  contest_rank,
      |  total_score,
      |  penalty_millis,
      |  rs.username as user_username,
      |  up.display_name as user_display_name
      |from ranked_scores rs
      |${UserIdentitySql.joinUserProfiles("rs.username", "up")}
      |order by contest_rank asc, rs.username asc
      |limit ? offset ?
      |""".stripMargin

  /** 读取榜单当前页，并根据查看者权限决定是否带出提交详情入口。 */
  private def readRows(
    connection: Connection,
    contestId: ContestId,
    viewerUsername: Username,
    canViewAllSubmissionDetails: Boolean,
    page: Int,
    pageSize: Int
  ): IO[List[ContestRanklistItem]] =
    for
      rows <- IO.blocking {
        val statement = connection.prepareStatement(rankedRowsSql)
        try
          statement.setObject(1, contestId.value)
          statement.setInt(2, pageSize)
          statement.setInt(3, (page - 1) * pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readRanklistItem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      items <- rows.traverse { item =>
        readProblemResults(connection, contestId, item.user.username, viewerUsername, canViewAllSubmissionDetails)
          .map(problemResults => item.copy(problemResults = problemResults))
      }
    yield items

  private val problemResultsSql: String =
    """
      |select
      |  cp.problem_id,
      |  p.slug as problem_slug,
      |  p.title as problem_title,
      |  cp.position,
      |  cp.alias,
      |  best_submission.public_id,
      |  best_submission.score,
      |  best_submission.submitted_at,
      |  case
      |    when best_submission.submitted_at is null then null
      |    else floor(extract(epoch from (best_submission.submitted_at - c.start_at)) * 1000)::bigint
      |  end as penalty_millis
      |from contest_problems cp
      |join contests c on c.id = cp.contest_id
      |join problems p on p.id = cp.problem_id
      |left join lateral (
      |  select
      |    s.public_id,
      |    coalesce(s.score, 0) as score,
      |    s.submitted_at
      |  from submissions s
      |  where s.problem_id = cp.problem_id
      |    and s.contest_id = cp.contest_id
      |    and s.submitter_username = ?
      |  order by coalesce(s.score, 0) desc, s.submitted_at asc, s.public_id asc
      |  limit 1
      |) best_submission on true
      |where cp.contest_id = ?
      |order by cp.position asc, cp.alias asc
      |""".stripMargin

  /** 读取单个参赛者的逐题最佳提交结果，并按查看权限控制 submissionId 是否可进入详情。 */
  private def readProblemResults(
    connection: Connection,
    contestId: ContestId,
    username: Username,
    viewerUsername: Username,
    canViewAllSubmissionDetails: Boolean
  ): IO[List[ContestRanklistProblemResult]] =
    IO.blocking {
      val statement = connection.prepareStatement(problemResultsSql)
      try
        statement.setString(1, username.value)
        statement.setObject(2, contestId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readProblemResult(resultSet, canViewAllSubmissionDetails || username == viewerUsername))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private def readRanklistItem(resultSet: ResultSet): ContestRanklistItem =
    ContestRanklistItem(
      rank = ContestRank(resultSet.getInt("contest_rank")),
      user = readUserIdentity(resultSet),
      totalScore = ContestScore(BigDecimal(resultSet.getBigDecimal("total_score"))),
      penaltyMillis = ContestPenaltyMillis(resultSet.getLong("penalty_millis")),
      problemResults = List.empty
    )

  private def readRatingSnapshotParticipant(resultSet: ResultSet): RatingSnapshotParticipantRecord =
    RatingSnapshotParticipantRecord(
      username = Username.canonical(resultSet.getString("username")),
      rank = ContestRank(resultSet.getInt("contest_rank")),
      totalScore = ContestScore(BigDecimal(resultSet.getBigDecimal("total_score"))),
      penaltyMillis = ContestPenaltyMillis(resultSet.getLong("penalty_millis"))
    )

  private def readProblemResult(resultSet: ResultSet, canViewDetail: Boolean): ContestRanklistProblemResult =
    val publicId = resultSet.getLong("public_id")
    val submissionId =
      if resultSet.wasNull() then None
      else Some(SubmissionId(publicId))
    val penaltyMillisValue = resultSet.getLong("penalty_millis")
    val penaltyMillis =
      if resultSet.wasNull() then None
      else Some(ContestPenaltyMillis(penaltyMillisValue))
    ContestRanklistProblemResult(
      problem = ContestProblemSummary(
        id = ProblemId(resultSet.getObject("problem_id", classOf[UUID])),
        slug = ProblemSlug(resultSet.getString("problem_slug")),
        title = ProblemTitle(resultSet.getString("problem_title")),
        position = resultSet.getInt("position"),
        alias = ContestProblemAlias(resultSet.getString("alias"))
      ),
      score = Option(resultSet.getBigDecimal("score")).map(value => ContestScore(BigDecimal(value))),
      penaltyMillis = penaltyMillis,
      submittedAt = Option(resultSet.getTimestamp("submitted_at")).map(_.toInstant),
      submissionId = submissionId,
      canViewDetail = submissionId.nonEmpty && canViewDetail
    )

  private def readUserIdentity(resultSet: ResultSet): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, "user")
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )
