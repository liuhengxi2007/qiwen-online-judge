package domains.problem.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 设置题目数据 ready 状态的请求体；true 会触发 judge.yaml 完整校验，false 只下线数据。 */
final case class SetProblemDataReadyRequest(ready: Boolean)

/** SetProblemDataReadyRequest 的 JSON 编解码器。 */
object SetProblemDataReadyRequest:
  given Encoder[SetProblemDataReadyRequest] = deriveEncoder[SetProblemDataReadyRequest]
  given Decoder[SetProblemDataReadyRequest] = deriveDecoder[SetProblemDataReadyRequest]
