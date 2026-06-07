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

object GetContestRatingSnapshot extends InternalOnlyApi[ContestSlug, GetContestRatingSnapshot.Output]:

  final case class Output(
    slug: ContestSlug,
    title: ContestTitle,
    startAt: Instant,
    endAt: Instant,
    participants: List[Participant]
  )

  final case class Participant(
    username: Username,
    rank: ContestRank,
    totalScore: ContestScore,
    penaltyMillis: ContestPenaltyMillis
  )

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/internal/contests/:contestSlug/rating-snapshot")

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
