package domains.user.utils

import domains.user.objects.Username

import java.util.UUID

/** 已通过校验的头像上传内容，包含对象 key、字节和规范 content type。 */
final case class PreparedUserAvatar(
  objectKey: String,
  bytes: Array[Byte],
  contentType: String
)

/** 用户头像上传校验工具，负责大小、content type 和对象 key 生成。 */
object UserAvatarUploadValidation:
  val maxAvatarBytes: Int = 2 * 1024 * 1024

  /** 校验头像并生成目标对象 key；失败返回业务错误，不写入存储。 */
  def prepare(username: Username, bytes: Array[Byte], contentType: Option[String], uploadId: UUID): Either[String, PreparedUserAvatar] =
    for
      _ <- validateSize(bytes)
      validContentType <- validateContentType(contentType, bytes)
    yield PreparedUserAvatar(
      objectKey = s"avatars/users/${username.value}/${uploadId.toString}.${extensionFor(validContentType)}",
      bytes = bytes,
      contentType = validContentType
    )

  private def validateContentType(contentType: Option[String], bytes: Array[Byte]): Either[String, String] =
    contentType.map(_.trim.toLowerCase).filter(_.nonEmpty) match
      case Some("image/png") if hasPngHeader(bytes) => Right("image/png")
      case Some("image/jpeg") if hasJpegHeader(bytes) => Right("image/jpeg")
      case Some("image/png" | "image/jpeg") => Left("Avatar file content does not match the declared image type.")
      case Some(_) => Left("Avatar must be a PNG or JPEG image.")
      case None => Left("Avatar upload must include a supported image content type.")

  private def validateSize(bytes: Array[Byte]): Either[String, Unit] =
    if bytes.isEmpty then Left("Avatar file is required.")
    else if bytes.length > maxAvatarBytes then Left("Avatar file must be at most 2 MB.")
    else Right(())

  private def extensionFor(contentType: String): String =
    contentType match
      case "image/png" => "png"
      case "image/jpeg" => "jpg"
      case _ => "bin"

  private def hasPngHeader(bytes: Array[Byte]): Boolean =
    bytes.length >= 8 &&
      bytes(0) == 0x89.toByte &&
      bytes(1) == 0x50.toByte &&
      bytes(2) == 0x4e.toByte &&
      bytes(3) == 0x47.toByte &&
      bytes(4) == 0x0d.toByte &&
      bytes(5) == 0x0a.toByte &&
      bytes(6) == 0x1a.toByte &&
      bytes(7) == 0x0a.toByte

  private def hasJpegHeader(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 &&
      bytes(0) == 0xff.toByte &&
      bytes(1) == 0xd8.toByte &&
      bytes(2) == 0xff.toByte
