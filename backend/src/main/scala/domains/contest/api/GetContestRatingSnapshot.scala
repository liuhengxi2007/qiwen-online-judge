package domains.contest.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.contest.objects.{ContestPenaltyMillis, ContestRank, ContestScore, ContestSlug, ContestTitle}
import domains.contest.table.contest.{ContestRanklistTable, ContestTable}
import domains.user.objects.Username
import org.http4s.Method
import shared.api.{ApiMessages, ApiPath, HttpApiError}

import java.sql.Connection
import java.time.Instant

/** 面向评分 domain 的内部 API，导出比赛结束时间和榜单快照用于评分序列。 */
object GetContestRatingSnapshot extends InternalOnlyApi[ContestSlug, GetContestRatingSnapshot.Output]:

  /** 评分快照输出，包含比赛时间和全部可纳入评分的参与者排名数据。 */
  final case class Output(
    slug: ContestSlug,
    title: ContestTitle,
    startAt: Instant,
    endAt: Instant,
    participants: List[Participant]
  )

  /** 单个评分参与者快照，保留排名、总分和罚时供评分记录持久化。 */
  final case class Participant(
    username: Username,
    rank: ContestRank,
    totalScore: ContestScore,
    penaltyMillis: ContestPenaltyMillis
  )

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/internal/contests/:contestSlug/rating-snapshot")

  /** 根据比赛 slug 读取比赛与评分参与者；比赛不存在时返回 404。 */
  override def plan(connection: Connection, slug: ContestSlug): IO[Output] =
    for
      maybeContest <- ContestTable.findBySlug(connection, slug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      participants <- ContestRanklistTable.listRatingSnapshotParticipants(connection, contest.id)
    yield Output(
      slug = contest.slug,
      title = contest.title,
      startAt = contest.startAt,
      endAt = contest.endAt,
      participants = participants.map(participant =>
        Participant(
          username = participant.username,
          rank = participant.rank,
          totalScore = participant.totalScore,
          penaltyMillis = participant.penaltyMillis
        )
      )
    )
