package domains.notification.http.codec

import domains.blog.http.codec.BlogModelHttpCodecs.given
import domains.notification.model.*
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

object NotificationModelHttpCodecs:
  given Encoder[NotificationId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[NotificationId] = Decoder.decodeString.emap(NotificationId.parse)

  given Encoder[NotificationKind] = Encoder.encodeString.contramap(NotificationKind.toDatabase)
  given Decoder[NotificationKind] = Decoder.decodeString.emap(NotificationKind.parse)

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

  private def decodeBlogReply(cursor: HCursor): Decoder.Result[NotificationPayload] =
    for
      blogId <- cursor.downField("blogId").as[domains.blog.model.BlogId]
      blogTitle <- cursor.downField("blogTitle").as[domains.blog.model.BlogTitle]
      triggerCommentId <- cursor.downField("triggerCommentId").as[domains.blog.model.BlogCommentId]
      recipientCommentId <- cursor.downField("recipientCommentId").as[Option[domains.blog.model.BlogCommentId]]
      contentPreview <- cursor.downField("contentPreview").as[String]
    yield NotificationPayload.BlogReply(
      blogId = blogId,
      blogTitle = blogTitle,
      triggerCommentId = triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = contentPreview
    )
