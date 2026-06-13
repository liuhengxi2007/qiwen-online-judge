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
  private val maxAvatarBytes: Int = 2 * 1024 * 1024

  /** 校验头像并生成目标对象 key；失败返回业务错误，不写入存储。 */
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
    /** FIXME-CN: 头像类型只信任 multipart Content-Type，没有校验 PNG/JPEG 文件头，伪造类型可绕过格式判断。 */
    contentType.map(_.trim.toLowerCase).filter(_.nonEmpty) match
      case Some("image/png") => Right("image/png")
      case Some("image/jpeg") => Right("image/jpeg")
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
