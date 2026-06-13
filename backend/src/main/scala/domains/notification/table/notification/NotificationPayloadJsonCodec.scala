package domains.notification.table.notification

import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}
import domains.notification.objects.NotificationPayload
import io.circe.parser.decode as decodeJson
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

/** 通知 payload 数据库存储 JSON codec，负责在 NotificationPayload 与 jsonb 字符串之间转换。 */
object NotificationPayloadJsonCodec:
  /** 将通知 payload 编码为紧凑 JSON 字符串。 */
  def encode(payload: NotificationPayload): String =
    payload.asJson.noSpaces

  /** 注意：从数据库 JSON 字符串解码通知 payload；非法数据视为数据库状态异常，因此抛出异常而不是返回业务错误。 */
  def decode(raw: String): NotificationPayload =
    decodeJson[NotificationPayload](raw).fold(
      error => throw IllegalStateException(s"Invalid notification payload: ${error.getMessage}"),
      identity
    )

  private given Encoder[NotificationPayload] = Encoder.instance {
    case payload: NotificationPayload.BlogReply =>
      Json.obj(
        "kind" -> Json.fromString(NotificationPayload.BlogReplyKind),
        "blogId" -> Json.fromLong(payload.blogId.value),
        "blogTitle" -> Json.fromString(payload.blogTitle.value),
        "triggerCommentId" -> Json.fromLong(payload.triggerCommentId.value),
        "recipientCommentId" -> payload.recipientCommentId.fold(Json.Null)(id => Json.fromLong(id.value)),
        "contentPreview" -> Json.fromString(payload.contentPreview)
      )
  }

  private given Decoder[NotificationPayload] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case NotificationPayload.BlogReplyKind => decodeBlogReply(cursor)
      case other => Left(DecodingFailure(s"Unsupported notification payload kind: $other", cursor.history))
    }
  }

  /** 解码博客回复 payload，并对 id 与标题执行领域级校验。 */
  private def decodeBlogReply(cursor: HCursor): Decoder.Result[NotificationPayload] =
    for
      blogId <- cursor.downField("blogId").as[Long].flatMap(decodeBlogId(_, cursor))
      blogTitle <- cursor.downField("blogTitle").as[String].flatMap(decodeBlogTitle(_, cursor))
      triggerCommentId <- cursor.downField("triggerCommentId").as[Long].flatMap(decodeBlogCommentId(_, cursor))
      recipientCommentId <- cursor.downField("recipientCommentId").as[Option[Long]].flatMap {
        case Some(value) => decodeBlogCommentId(value, cursor).map(Some(_))
        case None => Right(None)
      }
      contentPreview <- cursor.downField("contentPreview").as[String]
    yield NotificationPayload.BlogReply(
      blogId = blogId,
      blogTitle = blogTitle,
      triggerCommentId = triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = contentPreview
    )

  private def decodeBlogId(value: Long, cursor: HCursor): Decoder.Result[BlogId] =
    if value > 0 then Right(BlogId(value))
    else Left(DecodingFailure("Blog id must be a positive integer.", cursor.history))

  private def decodeBlogCommentId(value: Long, cursor: HCursor): Decoder.Result[BlogCommentId] =
    if value > 0 then Right(BlogCommentId(value))
    else Left(DecodingFailure("Blog comment id must be a positive integer.", cursor.history))

  private def decodeBlogTitle(value: String, cursor: HCursor): Decoder.Result[BlogTitle] =
    BlogTitle.parse(value).left.map(message => DecodingFailure(message, cursor.history))
