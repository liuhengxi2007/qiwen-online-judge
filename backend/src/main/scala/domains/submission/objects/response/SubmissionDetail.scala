package domains.submission.objects.response

import domains.submission.objects.*
import domains.submission.objects.internal.SubmissionDetailRecord

import domains.user.objects.UserIdentity
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.response.JudgeResult

import java.time.Instant
import scala.util.Try

final case class SubmissionDetail(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  problemTitle: ProblemTitle,
  source: SubmissionSource,
  canManage: Boolean,
  submitter: UserIdentity,
  language: SubmissionLanguage,
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  score: Option[BigDecimal],
  judgeResult: Option[JudgeResult],
  codeLength: Int,
  sourceCode: SubmissionSourceCode,
  programs: Map[String, SubmissionDetail.Program],
  submittedAt: Instant,
  startedAt: Option[Instant],
  finishedAt: Option[Instant]
)

object SubmissionDetail:
  final case class Program(
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  )

  object Program:
    given Encoder[Program] = deriveEncoder[Program]
    given Decoder[Program] = deriveDecoder[Program]

  def fromRecord(record: SubmissionDetailRecord, sourceCode: SubmissionSourceCode, canManage: Boolean): SubmissionDetail =
    fromRecord(record, Map(record.programManifest.defaultProgramKey -> sourceCode), canManage)

  def fromRecord(record: SubmissionDetailRecord, sourceCodes: Map[String, SubmissionSourceCode], canManage: Boolean = false): SubmissionDetail =
    val defaultSourceCode = sourceCodes.getOrElse(record.programManifest.defaultProgramKey, SubmissionSourceCode(""))
    SubmissionDetail(
      id = record.id,
      problemId = record.problemId,
      problemSlug = record.problemSlug,
      problemTitle = record.problemTitle,
      source = record.source,
      canManage = canManage,
      submitter = record.submitter,
      language = record.language,
      status = record.status,
      verdict = record.verdict,
      timeUsedMs = record.timeUsedMs,
      memoryUsedKb = record.memoryUsedKb,
      score = record.score,
      judgeResult = record.judgeResult,
      codeLength = record.codeLength,
      sourceCode = defaultSourceCode,
      programs = record.programManifest.programs.map { case (role, program) =>
        role -> Program(program.language, sourceCodes.getOrElse(role, SubmissionSourceCode("")))
      },
      submittedAt = record.submittedAt,
      startedAt = record.startedAt,
      finishedAt = record.finishedAt
    )

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[SubmissionDetail] = deriveEncoder[SubmissionDetail]
  given Decoder[SubmissionDetail] = deriveDecoder[SubmissionDetail]
