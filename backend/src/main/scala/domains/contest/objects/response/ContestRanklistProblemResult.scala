package domains.contest.objects.response

import domains.contest.objects.{ContestPenaltyMillis, ContestProblemSummary, ContestScore}
import domains.submission.objects.SubmissionId
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant

/** 比赛榜单逐题结果，submissionId 和 canViewDetail 控制前端能否进入提交详情。 */
final case class ContestRanklistProblemResult(
  problem: ContestProblemSummary,
  score: Option[ContestScore],
  penaltyMillis: Option[ContestPenaltyMillis],
  submittedAt: Option[Instant],
  submissionId: Option[SubmissionId],
  canViewDetail: Boolean
)

/** 提供比赛榜单逐题结果 JSON codec。 */
object ContestRanklistProblemResult:
  given Encoder[ContestRanklistProblemResult] = deriveEncoder[ContestRanklistProblemResult]
  given Decoder[ContestRanklistProblemResult] = deriveDecoder[ContestRanklistProblemResult]
