package domains.contest.table.contest

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.contest.objects.{ContestId, ContestPenaltyMillis, ContestRank, ContestScore}
import domains.contest.objects.response.ContestRanklistItem
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.PageResponse

import java.sql.{Connection, ResultSet}

object ContestRanklistTable:

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
      |      and s.submitter_username = cr.username
      |      and s.submitted_at >= c.start_at
      |      and s.submitted_at <= c.end_at
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

  private val countRowsSql: String =
    """
      |select count(*) as total_items
      |from contest_registrations
      |where contest_id = ?
      |""".stripMargin

  def listForContest(connection: Connection, contestId: ContestId, page: Int, pageSize: Int): IO[PageResponse[ContestRanklistItem]] =
    for
      totalItems <- countRows(connection, contestId)
      items <- readRows(connection, contestId, page, pageSize)
    yield PageResponse(items = items, page = page, pageSize = pageSize, totalItems = totalItems)

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

  private def readRows(connection: Connection, contestId: ContestId, page: Int, pageSize: Int): IO[List[ContestRanklistItem]] =
    IO.blocking {
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

  private def readRanklistItem(resultSet: ResultSet): ContestRanklistItem =
    ContestRanklistItem(
      rank = ContestRank(resultSet.getInt("contest_rank")),
      user = readUserIdentity(resultSet),
      totalScore = ContestScore(BigDecimal(resultSet.getBigDecimal("total_score"))),
      penaltyMillis = ContestPenaltyMillis(resultSet.getLong("penalty_millis"))
    )

  private def readUserIdentity(resultSet: ResultSet): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, "user")
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )
