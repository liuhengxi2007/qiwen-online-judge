package domains.submission.utils

import domains.submission.objects.{SubmissionStatus, SubmissionVerdict}
import domains.submission.objects.internal.SubmissionJudgeState
import domains.submission.objects.internal.SubmissionDetailRecord
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeResult, JudgeResultSummary}

import java.time.Instant

/** 提交判题生命周期规则；集中校验 queued/running/terminal 状态转换和协议结果一致性。 */
object SubmissionJudgeRules:

  /** 将 queued 状态推进为 running；非 queued 状态返回错误，避免重复领取。 */
  def beginJudging(state: SubmissionJudgeState, startedAt: Instant): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Queued =>
        Right(
          state.copy(
            status = SubmissionStatus.Running,
            judgeResult = None,
            startedAt = Some(startedAt),
            finishedAt = None
          )
        )
      case _ =>
        Left(s"Only queued submissions can start judging, but found ${statusName(state.status)}.")

  /** 将 running 状态推进为 completed/failed；校验终态结果、系统错误 reason 和状态匹配。 */
  def completeJudging(
    state: SubmissionJudgeState,
    status: SubmissionStatus,
    judgeResult: Option[JudgeResult],
    finishedAt: Instant
  ): Either[String, SubmissionJudgeState] =
    state.status match
      case SubmissionStatus.Running =>
        status match
          case SubmissionStatus.Completed | SubmissionStatus.Failed =>
            for
              judgeResult <- judgeResult.toRight("Terminal judge updates must include judgeResult.")
              _ <- validateReasonMatchesVerdict(judgeResult)
              _ <- validateStatusMatchesResult(status, judgeResult)
            yield
              state.copy(
                status = status,
                judgeResult = Some(judgeResult),
                finishedAt = Some(finishedAt)
              )
          case _ =>
            Left("Judging may only finish with completed or failed status.")
      case _ =>
        Left(s"Only running submissions can finish judging, but found ${statusName(state.status)}.")

  /** 从数据库详情记录抽取判题状态快照。 */
  def fromSubmissionRecord(submission: SubmissionDetailRecord): SubmissionJudgeState =
    SubmissionJudgeState(
      status = submission.status,
      judgeResult = submission.judgeResult,
      startedAt = submission.startedAt,
      finishedAt = submission.finishedAt
    )

  /** 将 judge-protocol 状态映射为提交域状态。 */
  def fromProtocolStatus(status: judgeprotocol.objects.SubmissionStatus): SubmissionStatus =
    status match
      case judgeprotocol.objects.SubmissionStatus.Queued => SubmissionStatus.Queued
      case judgeprotocol.objects.SubmissionStatus.Running => SubmissionStatus.Running
      case judgeprotocol.objects.SubmissionStatus.Completed => SubmissionStatus.Completed
      case judgeprotocol.objects.SubmissionStatus.Failed => SubmissionStatus.Failed

  /** 将 judge-protocol 结论映射为提交域结论。 */
  def fromProtocolVerdict(verdict: judgeprotocol.objects.SubmissionVerdict): SubmissionVerdict =
    verdict match
      case judgeprotocol.objects.SubmissionVerdict.Accepted => SubmissionVerdict.Accepted
      case judgeprotocol.objects.SubmissionVerdict.AcceptedByProtocol => SubmissionVerdict.AcceptedByProtocol
      case judgeprotocol.objects.SubmissionVerdict.WrongAnswer => SubmissionVerdict.WrongAnswer
      case judgeprotocol.objects.SubmissionVerdict.CompileError => SubmissionVerdict.CompileError
      case judgeprotocol.objects.SubmissionVerdict.RuntimeError => SubmissionVerdict.RuntimeError
      case judgeprotocol.objects.SubmissionVerdict.TimeLimitExceeded => SubmissionVerdict.TimeLimitExceeded
      case judgeprotocol.objects.SubmissionVerdict.IdlenessLimitExceeded => SubmissionVerdict.IdlenessLimitExceeded
      case judgeprotocol.objects.SubmissionVerdict.SystemError => SubmissionVerdict.SystemError

  /** 将 judge-protocol 语言映射为提交域语言；未知语言返回 None 以便 claim 时忽略。 */
  def toSubmissionLanguage(language: judgeprotocol.objects.SubmissionLanguage): Option[domains.submission.objects.SubmissionLanguage] =
    language match
      case judgeprotocol.objects.SubmissionLanguage.Cpp17 => Some(domains.submission.objects.SubmissionLanguage.Cpp17)
      case judgeprotocol.objects.SubmissionLanguage.Python3 => Some(domains.submission.objects.SubmissionLanguage.Python3)
      case judgeprotocol.objects.SubmissionLanguage.Text => Some(domains.submission.objects.SubmissionLanguage.Text)

  private def statusName(status: SubmissionStatus): String =
    status match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  private def validateStatusMatchesResult(status: SubmissionStatus, judgeResult: JudgeResult): Either[String, Unit] =
    val hasSystemError = containsSystemError(judgeResult)
    val hasReason = containsFailureReason(judgeResult)
    status match
      case SubmissionStatus.Completed if hasSystemError =>
        Left("Completed judge updates must not include a system error judgeResult.")
      case SubmissionStatus.Completed if hasReason =>
        Left("Completed judge updates must not include judge failure reasons.")
      case SubmissionStatus.Failed if !hasSystemError =>
        Left("Failed judge updates must include a system error judgeResult.")
      case SubmissionStatus.Completed | SubmissionStatus.Failed =>
        Right(())
      case _ =>
        Left("Judging may only finish with completed or failed status.")

  private def validateReasonMatchesVerdict(judgeResult: JudgeResult): Either[String, Unit] =
    validateSummaryReason("judgeResult baseResult", judgeResult.baseResult)
      .flatMap(_ => validateSummaryReason("judgeResult worstResult", judgeResult.worstResult))
      .flatMap(_ =>
        judgeResult.subtasks
          .map(subtask =>
            val subtaskLabel = judgeNodeLabel("subtask", subtask.index, subtask.label)
            validateSummaryReason(s"$subtaskLabel baseResult", subtask.baseResult)
              .flatMap(_ => validateSummaryReason(s"$subtaskLabel worstResult", subtask.worstResult))
              .flatMap(_ =>
                subtask.testcases
                  .map(testcase => validateNodeReason(judgeNodeLabel("testcase", testcase.index, testcase.label), testcase.verdict, testcase.reason))
                  .collectFirst { case Left(message) => Left(message) }
                  .getOrElse(Right(()))
              )
          )
          .collectFirst { case Left(message) => Left(message) }
          .getOrElse(Right(()))
      )

  private def validateSummaryReason(label: String, summary: JudgeResultSummary): Either[String, Unit] =
    validateNodeReason(label, summary.verdict, summary.reason)

  private def validateNodeReason(
    label: String,
    verdict: judgeprotocol.objects.SubmissionVerdict,
    reason: Option[JudgeFailureReason]
  ): Either[String, Unit] =
    if reason.nonEmpty && verdict != judgeprotocol.objects.SubmissionVerdict.SystemError then
      Left(s"$label reason is only allowed with system_error verdict.")
    else if verdict == judgeprotocol.objects.SubmissionVerdict.SystemError && reason.isEmpty then
      Left(s"$label system_error verdict must include reason.")
    else
      Right(())

  private def judgeNodeLabel(kind: String, index: Int, label: Option[String]): String =
    label match
      case Some(value) => s"$kind $index ($value)"
      case None => s"$kind $index"

  private def containsSystemError(judgeResult: JudgeResult): Boolean =
    judgeResult.baseResult.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
      judgeResult.worstResult.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
      judgeResult.subtasks.exists(subtask =>
        subtask.baseResult.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
          subtask.worstResult.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError ||
          subtask.testcases.exists(_.verdict == judgeprotocol.objects.SubmissionVerdict.SystemError)
      )

  private def containsFailureReason(judgeResult: JudgeResult): Boolean =
    judgeResult.baseResult.reason.nonEmpty ||
      judgeResult.worstResult.reason.nonEmpty ||
      judgeResult.subtasks.exists(subtask =>
        subtask.baseResult.reason.nonEmpty ||
          subtask.worstResult.reason.nonEmpty ||
          subtask.testcases.exists(_.reason.nonEmpty)
      )
