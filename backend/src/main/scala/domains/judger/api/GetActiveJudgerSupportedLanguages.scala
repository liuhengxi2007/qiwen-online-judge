package domains.judger.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.judger.table.judger.JudgerTable
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部查询活动 judger 支持语言的输入；heartbeatTimeoutMs 决定租约有效窗口。 */
final case class ActiveJudgerSupportedLanguagesInput(
  judgerId: JudgerId,
  heartbeatTimeoutMs: Long
)

/** 内部 judger 活动性查询 API；judge claim 前用它确认 worker 注册且心跳未过期。 */
object GetActiveJudgerSupportedLanguages extends InternalOnlyApi[ActiveJudgerSupportedLanguagesInput, Option[List[SubmissionLanguage]]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judgers/active-supported-languages")

  /** 构造活动 judger 查询输入。 */
  def input(judgerId: JudgerId, heartbeatTimeoutMs: Long): ActiveJudgerSupportedLanguagesInput =
    ActiveJudgerSupportedLanguagesInput(judgerId = judgerId, heartbeatTimeoutMs = heartbeatTimeoutMs)

  /** 返回活动 judger 的支持语言；未注册或租约过期返回 None。 */
  override def plan(connection: Connection, input: ActiveJudgerSupportedLanguagesInput): IO[Option[List[SubmissionLanguage]]] =
    JudgerTable.findActiveSupportedLanguages(connection, input.judgerId, input.heartbeatTimeoutMs)
