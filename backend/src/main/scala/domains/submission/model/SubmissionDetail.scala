package domains.submission.model

import domains.auth.model.Username
import domains.problem.model.{ProblemId, ProblemSlug}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class SubmissionDetail(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  submitterUsername: Username,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  sourceCode: SubmissionSourceCode,
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[SubmissionDetail] = deriveEncoder[SubmissionDetail]
  given Decoder[SubmissionDetail] = deriveDecoder[SubmissionDetail]
