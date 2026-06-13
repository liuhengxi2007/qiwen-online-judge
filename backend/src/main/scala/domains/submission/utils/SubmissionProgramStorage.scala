package domains.submission.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.SubmissionSourceCode
import domains.submission.objects.internal.SubmissionProgramManifest

import java.nio.charset.StandardCharsets

/** 提交程序源码存储抽象；sourceKey 由 manifest 生成，实际后端当前为 MinIO。 */
trait SubmissionProgramStorage:
  /** 写入单个源码对象。 */
  def writeSource(sourceKey: String, sourceCode: SubmissionSourceCode): IO[Unit]
  /** 读取单个源码对象；缺失返回 None。 */
  def readSource(sourceKey: String): IO[Option[SubmissionSourceCode]]
  /** 删除单个源码对象；返回对象是否存在。 */
  def deleteSource(sourceKey: String): IO[Boolean]

  /** 读取 manifest 的默认源码；manifest 异常或对象缺失以 Left 返回。 */
  final def readDefaultSource(manifest: SubmissionProgramManifest): IO[Either[String, SubmissionSourceCode]] =
    manifest.defaultProgram match
      case Left(message) => IO.pure(Left(message))
      case Right(program) =>
        readSource(program.sourceKey).map {
          case Some(sourceCode) => Right(sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }

  /** 读取 manifest 中所有角色源码；任一对象缺失会返回 Left。 */
  final def readSources(manifest: SubmissionProgramManifest): IO[Either[String, Map[String, SubmissionSourceCode]]] =
    manifest.programs.toList
      .traverse { case (role, program) =>
        readSource(program.sourceKey).map {
          case Some(sourceCode) => Right(role -> sourceCode)
          case None => Left(s"Submission source object was not found: ${program.sourceKey}.")
        }
      }
      .map(_.sequence.map(_.toMap))

  /** 删除 manifest 中所有源码对象；单个 deleteSource 的返回值会被忽略。 */
  final def deleteManifest(manifest: SubmissionProgramManifest): IO[Unit] =
    manifest.programs.values.toList.traverse_(program => deleteSource(program.sourceKey).void)

/** 提交程序存储的字节转换工具。 */
object SubmissionProgramStorage:
  /** 将 UTF-8 字节解析为源码领域类型，并复用源码大小/非空校验。 */
  def sourceCodeFromBytes(bytes: Array[Byte]): Either[String, SubmissionSourceCode] =
    SubmissionSourceCode.parse(String(bytes, StandardCharsets.UTF_8))
