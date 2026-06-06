package domains.hack.objects.response

import domains.hack.objects.{HackId, HackStatus}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class HackDetail(
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
  input: String,
  strategyProviderSource: Option[String],
  answer: Option[String],
  oldScore: BigDecimal,
  newScore: Option[BigDecimal],
  validatorMessage: Option[String],
  standardMessage: Option[String],
  targetMessage: Option[String],
  createdAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object HackDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[HackDetail] = deriveEncoder[HackDetail]
  given Decoder[HackDetail] = deriveDecoder[HackDetail]
