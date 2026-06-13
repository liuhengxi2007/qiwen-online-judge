package judger.objects

import judgeprotocol.objects.JudgerId
import judgeprotocol.objects.response.RegisterJudgerResponse

/** 当前进程从 backend 获得的 judger 租约和心跳参数。 */
final case class RegisteredJudger(
  judgerId: JudgerId,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

/** 将协议响应转换为 judger 运行时持有的租约状态。 */
object RegisteredJudger:
  /** 从注册响应构造本地状态；不重新校验时间参数。 */
  def fromResponse(response: RegisterJudgerResponse): RegisteredJudger =
    RegisteredJudger(
      judgerId = response.judgerId,
      heartbeatIntervalMs = response.heartbeatIntervalMs,
      heartbeatTimeoutMs = response.heartbeatTimeoutMs
    )
