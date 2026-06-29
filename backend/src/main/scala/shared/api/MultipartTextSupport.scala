package shared.api

import cats.effect.IO
import org.http4s.Request
import org.http4s.multipart.{Multipart, Part}
import org.typelevel.ci.CIString

import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.util.Locale

/** Multipart 文本字段读取工具，负责字段数量校验、大小限制和严格 UTF-8 解码。 */
object MultipartTextSupport:

  /** 判断请求是否声明为 multipart，兼容 http4s Content-Type 和原始 header。 */
  def isMultipart(request: Request[IO]): Boolean =
    request.contentType.exists(_.mediaType.mainType == "multipart") ||
    request.headers.headers.exists { header =>
      header.name == CIString("Content-Type") &&
      header.value.toLowerCase(Locale.ROOT).startsWith("multipart/")
    }

  /** 返回 multipart 中指定字段名的所有 part，不读取 body。 */
  def partsNamed(multipart: Multipart[IO], fieldName: String): List[Part[IO]] =
    multipart.parts.filter(_.name.contains(fieldName)).toList

  /** 要求指定字段恰好出现一次，否则返回 400。 */
  def requireSinglePart(multipart: Multipart[IO], fieldName: String): IO[Part[IO]] =
    partsNamed(multipart, fieldName) match
      case part :: Nil => IO.pure(part)
      case Nil => HttpApiError.raise(HttpApiError.badRequest(s"Multipart field '$fieldName' is required."))
      case _ => HttpApiError.raise(HttpApiError.badRequest(s"Multipart field '$fieldName' must be provided exactly once."))

  /** 读取指定字段并按严格 UTF-8 解码，超过 maxBytes 或编码非法时返回 400。 */
  def requireUtf8Text(multipart: Multipart[IO], fieldName: String, maxBytes: Long): IO[String] =
    requireSinglePart(multipart, fieldName).flatMap(decodeUtf8Text(_, fieldName, maxBytes))

  /** 对单个 multipart part 做有界读取和严格文本解码。 */
  def decodeUtf8Text(part: Part[IO], fieldName: String, maxBytes: Long): IO[String] =
    part.body.take(maxBytes + 1).compile.to(Array).flatMap { bytes =>
      if bytes.length > maxBytes then
        HttpApiError.raise(HttpApiError.badRequest(s"Multipart field '$fieldName' must be at most $maxBytes bytes."))
      else
        HttpApiError.fromEitherBadRequest(strictUtf8(bytes).left.map(_ => s"Multipart field '$fieldName' must be valid UTF-8."))
    }

  private def strictUtf8(bytes: Array[Byte]): Either[String, String] =
    val decoder = StandardCharsets.UTF_8
      .newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
    try Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    catch case _: CharacterCodingException => Left("invalid UTF-8")
