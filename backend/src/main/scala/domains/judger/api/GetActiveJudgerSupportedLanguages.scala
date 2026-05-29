package domains.judger.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.judger.table.judger.JudgerTable
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class ActiveJudgerSupportedLanguagesInput(
  judgerId: JudgerId,
  heartbeatTimeoutMs: Long
)

object GetActiveJudgerSupportedLanguages extends InternalOnlyApi[ActiveJudgerSupportedLanguagesInput, Option[List[SubmissionLanguage]]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/judgers/active-supported-languages")

  def input(judgerId: JudgerId, heartbeatTimeoutMs: Long): ActiveJudgerSupportedLanguagesInput =
    ActiveJudgerSupportedLanguagesInput(judgerId = judgerId, heartbeatTimeoutMs = heartbeatTimeoutMs)

  override def plan(connection: Connection, input: ActiveJudgerSupportedLanguagesInput): IO[Option[List[SubmissionLanguage]]] =
    JudgerTable.findActiveSupportedLanguages(connection, input.judgerId, input.heartbeatTimeoutMs)
