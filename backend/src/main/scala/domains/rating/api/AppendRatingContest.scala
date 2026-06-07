package domains.rating.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.contest.api.GetContestRatingSnapshot
import domains.contest.objects.ContestSlug
import domains.rating.objects.internal.{RatingContestSnapshot, RatingContestSnapshotParticipant}
import domains.rating.objects.request.AppendRatingContestRequest
import domains.rating.objects.response.RatingManageState
import domains.rating.table.rating.RatingTable
import domains.rating.table.rating.RatingTable.AppendContestRecord
import domains.rating.utils.RatingCalculator
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

object AppendRatingContest extends SiteManagerApi[AppendRatingContestRequest, RatingManageState]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/ratings/manage/contests")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[RatingManageState] = summon[Encoder[RatingManageState]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[AppendRatingContestRequest] =
    val _ = pathParams
    request.as[AppendRatingContestRequest]

  override def plan(
    connection: Connection,
    actor: SiteManagerUser,
    request: AppendRatingContestRequest
  ): IO[RatingManageState] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(ContestSlug.parse(request.contestSlug.value))
      _ <- HttpApiError.ensure(
        RatingCalculator.isValidM(request.m),
        HttpApiError.badRequest(ApiMessages.ratingMOutOfRange)
      )
      snapshot <- GetContestRatingSnapshot.plan(connection, contestSlug)
      now = Instant.now()
      _ <- HttpApiError.ensure(!snapshot.endAt.isAfter(now), HttpApiError.badRequest(ApiMessages.ratingContestNotEnded))
      _ <- HttpApiError.ensure(snapshot.participants.size >= 2, HttpApiError.badRequest(ApiMessages.ratingContestTooFewParticipants))
      lastContestEndAt <- RatingTable.findLastContestEndAt(connection)
      overlapWarning = lastContestEndAt.exists(_.isAfter(snapshot.startAt))
      _ <- RatingTable.appendContest(
        connection,
        AppendContestRecord(
          contestSlug = snapshot.slug,
          contestTitle = snapshot.title,
          contestStartAt = snapshot.startAt,
          contestEndAt = snapshot.endAt,
          m = request.m,
          participantCount = snapshot.participants.size,
          overlapWarning = overlapWarning,
          snapshot = RatingContestSnapshot(
            participants = snapshot.participants.map(participant =>
              RatingContestSnapshotParticipant(
                username = participant.username,
                rank = participant.rank.value,
                totalScore = participant.totalScore.value,
                penaltyMillis = participant.penaltyMillis.value
              )
            )
          ),
          appendedBy = actor.username,
          appendedAt = now
        )
      )
      contests <- RatingTable.listManageContests(connection)
    yield RatingManageState(contests)
