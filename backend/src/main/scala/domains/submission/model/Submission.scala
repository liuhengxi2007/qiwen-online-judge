package domains.submission.model

import domains.auth.model.Username
import domains.problem.model.{ProblemId, ProblemSlug}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class SubmissionId(value: Long)

object SubmissionId:
  def parse(raw: String): Either[String, SubmissionId] =
    Try(raw.trim.toLong)
      .toEither
      .left
      .map(_ => "Submission id is invalid.")
      .flatMap { value =>
        if value < 1 then Left("Submission id is invalid.")
        else Right(SubmissionId(value))
      }

  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

enum SubmissionLanguage:
  case Cpp17
  case Python3

object SubmissionLanguage:
  def parse(value: String): Either[String, SubmissionLanguage] =
    value.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case _ => Left("Submission language must be one of: cpp17, python3.")

  def fromDatabase(value: String): Option[SubmissionLanguage] =
    value match
      case "cpp17" => Some(SubmissionLanguage.Cpp17)
      case "python3" => Some(SubmissionLanguage.Python3)
      case _ => None

  def toDatabase(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap(parse)

enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

object SubmissionStatus:
  def parse(value: String): Either[String, SubmissionStatus] =
    value.trim match
      case "queued" => Right(SubmissionStatus.Queued)
      case "running" => Right(SubmissionStatus.Running)
      case "completed" => Right(SubmissionStatus.Completed)
      case "failed" => Right(SubmissionStatus.Failed)
      case _ => Left("Submission status must be one of: queued, running, completed, failed.")

  def fromDatabase(value: String): Option[SubmissionStatus] =
    value match
      case "queued" => Some(SubmissionStatus.Queued)
      case "running" => Some(SubmissionStatus.Running)
      case "completed" => Some(SubmissionStatus.Completed)
      case "failed" => Some(SubmissionStatus.Failed)
      case _ => None

  def toDatabase(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(parse)

enum SubmissionVerdict:
  case Accepted
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError

object SubmissionVerdict:
  def parse(value: String): Either[String, SubmissionVerdict] =
    value.trim match
      case "accepted" => Right(SubmissionVerdict.Accepted)
      case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
      case "compile_error" => Right(SubmissionVerdict.CompileError)
      case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
      case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
      case "system_error" => Right(SubmissionVerdict.SystemError)
      case _ =>
        Left(
          "Submission verdict must be one of: accepted, wrong_answer, compile_error, runtime_error, time_limit_exceeded, system_error."
        )

  def fromDatabase(value: String): Option[SubmissionVerdict] =
    value match
      case "accepted" => Some(SubmissionVerdict.Accepted)
      case "wrong_answer" => Some(SubmissionVerdict.WrongAnswer)
      case "compile_error" => Some(SubmissionVerdict.CompileError)
      case "runtime_error" => Some(SubmissionVerdict.RuntimeError)
      case "time_limit_exceeded" => Some(SubmissionVerdict.TimeLimitExceeded)
      case "system_error" => Some(SubmissionVerdict.SystemError)
      case _ => None

  def toDatabase(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap(parse)

final case class SubmissionSourceCode(value: String)

object SubmissionSourceCode:
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Source code is required.")
    else if raw.length > 200000 then Left("Source code must be at most 200000 characters.")
    else Right(SubmissionSourceCode(raw))

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)

final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode
)

object CreateSubmissionRequest:
  given Encoder[CreateSubmissionRequest] = deriveEncoder[CreateSubmissionRequest]
  given Decoder[CreateSubmissionRequest] = deriveDecoder[CreateSubmissionRequest]

final case class SubmissionSummary(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  submitterUsername: Username,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
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
