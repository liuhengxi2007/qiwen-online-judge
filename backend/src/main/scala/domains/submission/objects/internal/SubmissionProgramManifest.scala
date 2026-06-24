package domains.submission.objects.internal

import domains.submission.objects.{SubmissionLanguage, SubmissionSourceCode}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/** 提交程序持久化 manifest，写入 submissions.program_manifest，供 SubmissionProgramStorage、JudgeTaskBuilder 和 hack 判题读取源码。 */
final case class SubmissionProgramManifest(
  defaultProgramKey: String,
  programs: Map[String, SubmissionProgramManifest.Program]
):
  /** 读取默认程序配置；manifest 异常时返回错误信息。 */
  def defaultProgram: Either[String, SubmissionProgramManifest.Program] =
    programs.get(defaultProgramKey).toRight(s"Default submission program was not found: $defaultProgramKey.")

  /** 按角色读取程序配置；缺失角色返回错误信息。 */
  def program(key: String): Either[String, SubmissionProgramManifest.Program] =
    programs.get(key).toRight(s"Submission program was not found: $key.")

/** 提交程序 manifest 构造与 JSON 编解码工具；CreateSubmission、backfill 和 hack 物化路径都通过这里生成一致格式。 */
object SubmissionProgramManifest:
  final val DefaultProgramKey: String = "main"
  private val CodeProgramKeyPattern = "^[A-Za-z0-9_-]+$".r
  private val TextProgramKeyPattern = "^[A-Za-z0-9_-]+\\.txt$".r

  /** 单个提交程序在对象存储中的元信息，由 SubmissionProgramStorage 按 sourceKey 读写源码。 */
  final case class Program(
    language: SubmissionLanguage,
    sourceKey: String,
    sizeBytes: Long,
    sha256: String
  )

  /** Program 的 JSON 编解码器。 */
  object Program:
    given Encoder[Program] = deriveEncoder[Program]
    given Decoder[Program] = deriveDecoder[Program]

  given Encoder[SubmissionProgramManifest] = deriveEncoder[SubmissionProgramManifest]
  given Decoder[SubmissionProgramManifest] = deriveDecoder[SubmissionProgramManifest]

  /** 生成源码对象 key；调用方需使用提交 UUID 避免不同提交间冲突。 */
  def defaultSourceKey(submissionUuid: UUID, programKey: String = DefaultProgramKey): String =
    s"submissions/${submissionUuid.toString}/programs/$programKey/source"

  /** 构造单 main 程序 manifest；主要用于旧数据迁移和兼容路径。 */
  def singleDefault(
    submissionUuid: UUID,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  ): SubmissionProgramManifest =
    unsafeFromPrograms(submissionUuid, Map(DefaultProgramKey -> (language -> sourceCode)))

  /** 从角色到语言/源码的映射构造 manifest；校验角色命名、文本角色后缀和重复角色。 */
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

  /** 构造 manifest，非法输入直接抛异常；只应在输入已由同一规则校验后使用。 */
  def unsafeFromPrograms(
    submissionUuid: UUID,
    rawPrograms: Map[String, (SubmissionLanguage, SubmissionSourceCode)]
  ): SubmissionProgramManifest =
    fromPrograms(submissionUuid, rawPrograms).fold(message => throw IllegalArgumentException(message), identity)

  /** 将源码编码为 UTF-8 字节，用于对象存储和 sha256 计算。 */
  def sourceBytes(sourceCode: SubmissionSourceCode): Array[Byte] =
    sourceCode.value.getBytes(StandardCharsets.UTF_8)

  /** 判断角色名是否为文本答案角色。 */
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
