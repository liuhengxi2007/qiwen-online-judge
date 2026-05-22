package domains.notification.model



import domains.blog.model.{BlogCommentId, BlogId, BlogTitle}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

sealed trait NotificationPayload

object NotificationPayload:
  final case class BlogReply(
    blogId: BlogId,
    blogTitle: BlogTitle,
    triggerCommentId: BlogCommentId,
    recipientCommentId: Option[BlogCommentId],
    contentPreview: String
  ) extends NotificationPayload

  given Encoder[NotificationPayload] = Encoder.instance {
    case payload: BlogReply =>
      Json.obj(
        "kind" -> Json.fromString("blog_reply"),
        "blogId" -> payload.blogId.asJson,
        "blogTitle" -> payload.blogTitle.asJson,
        "triggerCommentId" -> payload.triggerCommentId.asJson,
        "recipientCommentId" -> payload.recipientCommentId.asJson,
        "contentPreview" -> Json.fromString(payload.contentPreview)
      )
  }

  given Decoder[NotificationPayload] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "blog_reply" => decodeBlogReply(cursor)
      case other => Left(DecodingFailure(s"Unsupported notification payload kind: $other", cursor.history))
    }
  }

  private def decodeBlogReply(cursor: HCursor): Decoder.Result[NotificationPayload] =
    for
      blogId <- cursor.downField("blogId").as[BlogId]
      blogTitle <- cursor.downField("blogTitle").as[BlogTitle]
      triggerCommentId <- cursor.downField("triggerCommentId").as[BlogCommentId]
      recipientCommentId <- cursor.downField("recipientCommentId").as[Option[BlogCommentId]]
      contentPreview <- cursor.downField("contentPreview").as[String]
    yield BlogReply(
      blogId = blogId,
      blogTitle = blogTitle,
      triggerCommentId = triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = contentPreview
    )
