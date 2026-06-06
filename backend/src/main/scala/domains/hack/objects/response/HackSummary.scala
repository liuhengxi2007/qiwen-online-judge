package domains.hack.objects.response

import domains.hack.objects.{HackId, HackStatus}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class HackSummary(
  id: HackId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  targetSubmissionId: SubmissionId,
  targetSubmitter: UserIdentity,
  author: UserIdentity,
  subtaskIndex: Int,
  subtaskLabel: Option[String],
  status: HackStatus,
  oldScore: BigDecimal,
  newScore: Option[BigDecimal],
  createdAt: Instant,
  finishedAt: Option[Instant]
)

object HackSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[HackSummary] = deriveEncoder[HackSummary]
  given Decoder[HackSummary] = deriveDecoder[HackSummary]
