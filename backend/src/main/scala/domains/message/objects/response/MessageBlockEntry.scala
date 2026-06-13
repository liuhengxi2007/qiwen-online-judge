package domains.message.objects.response


import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 私信屏蔽名单条目，包含被屏蔽用户身份和创建时间。 */
final case class MessageBlockEntry(
  user: UserIdentity,
  createdAt: Instant
)

/** 提供屏蔽名单条目 JSON codec，并显式处理 Instant 字符串格式。 */
object MessageBlockEntry:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[MessageBlockEntry] = deriveEncoder[MessageBlockEntry]
  given Decoder[MessageBlockEntry] = deriveDecoder[MessageBlockEntry]
