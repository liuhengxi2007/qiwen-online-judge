package domains.notification.objects



import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

sealed trait NotificationPayload

object NotificationPayload:
  val BlogReplyKind = "blog_reply"

  given Encoder[NotificationPayload] = Encoder.instance {
    case payload: NotificationPayload.BlogReply =>
      Json.obj(
        "kind" -> Json.fromString(NotificationPayload.BlogReplyKind),
        "blogId" -> payload.blogId.asJson,
        "blogTitle" -> payload.blogTitle.asJson,
        "triggerCommentId" -> payload.triggerCommentId.asJson,
        "recipientCommentId" -> payload.recipientCommentId.asJson,
        "contentPreview" -> Json.fromString(payload.contentPreview)
      )
  }

  given Decoder[NotificationPayload] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case NotificationPayload.BlogReplyKind => decodeBlogReply(cursor)
      case other => Left(DecodingFailure(s"Unsupported notification payload kind: $other", cursor.history))
    }
  }

  final case class BlogReply(
    blogId: BlogId,
    blogTitle: BlogTitle,
    triggerCommentId: BlogCommentId,
    recipientCommentId: Option[BlogCommentId],
    contentPreview: String
  ) extends NotificationPayload

  private def decodeBlogReply(cursor: HCursor): Decoder.Result[NotificationPayload] =
    for
      blogId <- cursor.downField("blogId").as[BlogId]
      blogTitle <- cursor.downField("blogTitle").as[BlogTitle]
      triggerCommentId <- cursor.downField("triggerCommentId").as[BlogCommentId]
      recipientCommentId <- cursor.downField("recipientCommentId").as[Option[BlogCommentId]]
      contentPreview <- cursor.downField("contentPreview").as[String]
    yield NotificationPayload.BlogReply(
      blogId = blogId,
      blogTitle = blogTitle,
      triggerCommentId = triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = contentPreview
    )
