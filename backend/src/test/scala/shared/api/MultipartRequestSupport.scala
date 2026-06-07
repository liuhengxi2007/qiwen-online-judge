package shared.api

import cats.effect.IO
import fs2.{Chunk, Stream}
import org.http4s.EntityEncoder
import org.http4s.multipart.{Boundary, Multipart, MultipartEncoder, Part}
import org.http4s.{Header, Method, Request, Uri}
import org.typelevel.ci.CIString

import java.nio.charset.StandardCharsets

object MultipartRequestSupport:

  private given EntityEncoder[IO, Multipart[IO]] = MultipartEncoder[IO]

  final case class PartData(
    name: String,
    bytes: Array[Byte],
    filename: Option[String] = None
  )

  def textPart(name: String, value: String): PartData =
    PartData(name, value.getBytes(StandardCharsets.UTF_8))

  def filePart(name: String, filename: String, bytes: Array[Byte]): PartData =
    PartData(name, bytes, Some(filename))

  def request(parts: List[PartData], uri: String = "/api/test"): Request[IO] =
    val boundary = Boundary("codex-test-boundary")
    val multipart = Multipart[IO](
      parts.map { part =>
        part.filename match
          case Some(filename) => Part.fileData[IO](part.name, filename, Stream.chunk(Chunk.array(part.bytes)).covary[IO])
          case None => Part.formData[IO](part.name, new String(part.bytes, StandardCharsets.UTF_8))
      }.toVector,
      boundary
    )

    Request[IO](method = Method.POST, uri = Uri.unsafeFromString(uri))
      .withEntity(multipart)
      .putHeaders(Header.Raw(CIString("Content-Type"), s"multipart/form-data; boundary=${boundary.value}"))
