package domains.submission.model

import domains.auth.model.UserIdentity
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class SubmissionSummary(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  codeLength: Int,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[SubmissionSummary] = deriveEncoder[SubmissionSummary]
  given Decoder[SubmissionSummary] = deriveDecoder[SubmissionSummary]
