package domains.user.utils

import domains.user.objects.Username

import java.util.UUID

final case class PreparedUserAvatar(
  objectKey: String,
  bytes: Array[Byte],
  contentType: String
)

object UserAvatarUploadValidation:
  private val maxAvatarBytes: Int = 2 * 1024 * 1024

  def prepare(username: Username, bytes: Array[Byte], contentType: Option[String], uploadId: UUID): Either[String, PreparedUserAvatar] =
    for
      validContentType <- validateContentType(contentType)
      _ <- validateSize(bytes)
    yield PreparedUserAvatar(
      objectKey = s"avatars/users/${username.value}/${uploadId.toString}.${extensionFor(validContentType)}",
      bytes = bytes,
      contentType = validContentType
    )

  private def validateContentType(contentType: Option[String]): Either[String, String] =
    contentType.map(_.trim.toLowerCase).filter(_.nonEmpty) match
      case Some("image/png") => Right("image/png")
      case Some("image/jpeg") => Right("image/jpeg")
      case Some("image/webp") => Right("image/webp")
      case Some(_) => Left("Avatar must be a PNG, JPEG, or WebP image.")
      case None => Left("Avatar upload must include a supported image content type.")

  private def validateSize(bytes: Array[Byte]): Either[String, Unit] =
    if bytes.isEmpty then Left("Avatar file is required.")
    else if bytes.length > maxAvatarBytes then Left("Avatar file must be at most 2 MB.")
    else Right(())

  private def extensionFor(contentType: String): String =
    contentType match
      case "image/png" => "png"
      case "image/jpeg" => "jpg"
      case "image/webp" => "webp"
      case _ => "bin"
