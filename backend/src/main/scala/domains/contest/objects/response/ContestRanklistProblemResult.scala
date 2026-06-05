package domains.contest.objects.response

import domains.contest.objects.{ContestPenaltyMillis, ContestProblemSummary, ContestScore}
import domains.submission.objects.SubmissionId
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant

final case class ContestRanklistProblemResult(
  problem: ContestProblemSummary,
  score: Option[ContestScore],
  penaltyMillis: Option[ContestPenaltyMillis],
  submittedAt: Option[Instant],
  submissionId: Option[SubmissionId],
  canViewDetail: Boolean
)

object ContestRanklistProblemResult:
  given Encoder[ContestRanklistProblemResult] = deriveEncoder[ContestRanklistProblemResult]
  given Decoder[ContestRanklistProblemResult] = deriveDecoder[ContestRanklistProblemResult]
