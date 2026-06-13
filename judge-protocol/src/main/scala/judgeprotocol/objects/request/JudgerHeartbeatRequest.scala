package judgeprotocol.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** judger 心跳请求体；身份来自 URL path，因此请求体为空对象。 */
final case class JudgerHeartbeatRequest()

/** 负责空心跳请求体的协议编解码。 */
object JudgerHeartbeatRequest:
  given Encoder[JudgerHeartbeatRequest] = deriveEncoder[JudgerHeartbeatRequest]
  given Decoder[JudgerHeartbeatRequest] = deriveDecoder[JudgerHeartbeatRequest]
