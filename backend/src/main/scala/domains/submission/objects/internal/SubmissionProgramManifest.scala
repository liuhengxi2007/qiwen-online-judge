package domains.submission.objects.internal

import domains.submission.objects.{SubmissionLanguage, SubmissionSourceCode}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

final case class SubmissionProgramManifest(
  defaultProgramKey: String,
  programs: Map[String, SubmissionProgramManifest.Program]
):
  def defaultProgram: Either[String, SubmissionProgramManifest.Program] =
    programs.get(defaultProgramKey).toRight(s"Default submission program was not found: $defaultProgramKey.")

object SubmissionProgramManifest:
  final val DefaultProgramKey: String = "main"

  final case class Program(
    language: SubmissionLanguage,
    sourceKey: String,
    sizeBytes: Long,
    sha256: String
  )

  object Program:
    given Encoder[Program] = deriveEncoder[Program]
    given Decoder[Program] = deriveDecoder[Program]

  given Encoder[SubmissionProgramManifest] = deriveEncoder[SubmissionProgramManifest]
  given Decoder[SubmissionProgramManifest] = deriveDecoder[SubmissionProgramManifest]

  def defaultSourceKey(submissionUuid: UUID, programKey: String = DefaultProgramKey): String =
    s"submissions/${submissionUuid.toString}/programs/$programKey/source"

  def singleDefault(
    submissionUuid: UUID,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  ): SubmissionProgramManifest =
    val bytes = sourceBytes(sourceCode)
    val program = Program(
      language = language,
      sourceKey = defaultSourceKey(submissionUuid),
      sizeBytes = bytes.length.toLong,
      sha256 = sha256Hex(bytes)
    )
    SubmissionProgramManifest(
      defaultProgramKey = DefaultProgramKey,
      programs = Map(DefaultProgramKey -> program)
    )

  def sourceBytes(sourceCode: SubmissionSourceCode): Array[Byte] =
    sourceCode.value.getBytes(StandardCharsets.UTF_8)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
