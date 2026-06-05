package domains.submission.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest

import java.nio.charset.StandardCharsets

trait SubmissionProgramStorage:
  def writeSource(sourceKey: String, sourceCode: SubmissionSourceCode): IO[Unit]
  def readSource(sourceKey: String): IO[Option[SubmissionSourceCode]]
  def deleteSource(sourceKey: String): IO[Boolean]

  final def readDefaultSource(manifest: SubmissionProgramManifest): IO[Either[String, SubmissionSourceCode]] =
    manifest.defaultProgram match
      case Left(message) => IO.pure(Left(message))
      case Right(program) =>
        readSource(program.sourceKey).map {
          case Some(sourceCode) => Right(sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }

  final def readSources(manifest: SubmissionProgramManifest): IO[Either[String, Map[String, SubmissionSourceCode]]] =
    manifest.programs.toList
      .traverse { case (role, program) =>
        readSource(program.sourceKey).map {
          case Some(sourceCode) => Right(role -> sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }
      }
      .map(_.sequence.map(_.toMap))

  final def deleteManifest(manifest: SubmissionProgramManifest): IO[Unit] =
    manifest.programs.values.toList.traverse_(program => deleteSource(program.sourceKey).void)

object SubmissionProgramStorage:
  def sourceCodeFromBytes(bytes: Array[Byte]): Either[String, SubmissionSourceCode] =
    SubmissionSourceCode.parse(String(bytes, StandardCharsets.UTF_8))
