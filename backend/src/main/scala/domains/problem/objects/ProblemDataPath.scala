package domains.problem.objects

import io.circe.{Decoder, Encoder}


import shared.application.upload.StoredFilePath

/** 题目数据对象存储中的相对路径；路径必须通过 StoredFilePath 规则校验以避免越界。 */
final case class ProblemDataPath(value: String):
  /** 返回路径最后一段文件名，用于下载响应和旧接口兼容。 */
  def fileName: String =
    value.split('/').lastOption.getOrElse(value)

  /** 转换为共享上传模块使用的存储路径类型，保持同一个已校验相对路径。 */
  def toStoredFilePath: StoredFilePath =
    StoredFilePath(value)

/** 题目数据路径的 JSON 编解码与构造入口。 */
object ProblemDataPath:
  given Encoder[ProblemDataPath] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataPath] = Decoder.decodeString.emap(parse)

  /** 从外部字符串解析安全相对路径；非法路径返回错误信息。 */
  def parse(raw: String): Either[String, ProblemDataPath] =
    StoredFilePath.parse(raw).map(path => ProblemDataPath(path.value))

  /** 从已校验文件名构造同名根路径。 */
  def fromFilename(filename: ProblemDataFilename): ProblemDataPath =
    ProblemDataPath(filename.value)
