package judger.config

import judgeprotocol.objects.{JudgerId, RegisterJudgerResponse}

final case class RegisteredJudger(
  judgerId: JudgerId,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

object RegisteredJudger:
  def fromResponse(response: RegisterJudgerResponse): RegisteredJudger =
    RegisteredJudger(
      judgerId = response.judgerId,
      heartbeatIntervalMs = response.heartbeatIntervalMs,
      heartbeatTimeoutMs = response.heartbeatTimeoutMs
    )
