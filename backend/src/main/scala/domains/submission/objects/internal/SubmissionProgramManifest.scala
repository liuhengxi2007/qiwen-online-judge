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

  def program(key: String): Either[String, SubmissionProgramManifest.Program] =
    programs.get(key).toRight(s"Submission program was not found: $key.")

object SubmissionProgramManifest:
  final val DefaultProgramKey: String = "main"
  private val CodeProgramKeyPattern = "^[A-Za-z0-9_-]+$".r
  private val TextProgramKeyPattern = "^[A-Za-z0-9_-]+\\.txt$".r

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
    unsafeFromPrograms(submissionUuid, Map(DefaultProgramKey -> (language -> sourceCode)))

  def fromPrograms(
    submissionUuid: UUID,
    rawPrograms: Map[String, (SubmissionLanguage, SubmissionSourceCode)]
  ): Either[String, SubmissionProgramManifest] =
    if rawPrograms.isEmpty then Left("At least one submission program is required.")
    else
      val normalized = rawPrograms.toList.map { case (key, program) => key.trim -> program }
      normalized.collectFirst {
        case (key, _) if key.isEmpty => "Submission program role must not be empty."
        case (key, _) if !isValidProgramKey(key) =>
          s"Submission program role must contain only ASCII letters, digits, '_' or '-', with an optional single '.txt' suffix for text answers: $key."
        case (key, (SubmissionLanguage.Text, _)) if !isTextProgramKey(key) =>
          s"Submission text role must end with .txt: $key."
        case (key, (language, _)) if language != SubmissionLanguage.Text && isTextProgramKey(key) =>
          s"Submission role ending with .txt must use text language: $key."
      } match
        case Some(message) => Left(message)
        case None =>
          val duplicate = normalized.groupBy(_._1).collectFirst { case (key, items) if items.size > 1 => key }
          duplicate match
            case Some(key) => Left(s"Submission program role is duplicated: $key.")
            case None =>
              val defaultProgramKey =
                if normalized.exists(_._1 == DefaultProgramKey) then DefaultProgramKey
                else normalized.map(_._1).sorted.head
              val programs = normalized.map { case (programKey, (language, sourceCode)) =>
                val bytes = sourceBytes(sourceCode)
                programKey -> Program(
                  language = language,
                  sourceKey = defaultSourceKey(submissionUuid, programKey),
                  sizeBytes = bytes.length.toLong,
                  sha256 = sha256Hex(bytes)
                )
              }.toMap
              Right(SubmissionProgramManifest(defaultProgramKey, programs))

  def unsafeFromPrograms(
    submissionUuid: UUID,
    rawPrograms: Map[String, (SubmissionLanguage, SubmissionSourceCode)]
  ): SubmissionProgramManifest =
    fromPrograms(submissionUuid, rawPrograms).fold(message => throw IllegalArgumentException(message), identity)

  def sourceBytes(sourceCode: SubmissionSourceCode): Array[Byte] =
    sourceCode.value.getBytes(StandardCharsets.UTF_8)

  def isTextProgramKey(programKey: String): Boolean =
    TextProgramKeyPattern.findFirstIn(programKey).contains(programKey)

  private def isValidProgramKey(programKey: String): Boolean =
    CodeProgramKeyPattern.findFirstIn(programKey).contains(programKey) ||
      TextProgramKeyPattern.findFirstIn(programKey).contains(programKey)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
