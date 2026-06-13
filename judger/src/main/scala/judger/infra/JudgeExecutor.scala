package judger.infra

import cats.effect.IO
import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.request.{ReportHackResultRequest, ReportJudgeResultRequest}
import judgeprotocol.objects.response.{HackTask, JudgeTask}
import judger.config.AppConfig

/** 判题执行门面，隔离上层服务与普通提交/hack runner 的具体实现。 */
object JudgeExecutor:
  /** 执行普通提交判题；返回可直接 POST 给 backend 的结果请求。 */
  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportJudgeResultRequest] =
    SubmissionJudgeRunner.judge(task, config, problemDataCache, runtimes)

  /** 执行 hack 尝试；返回可直接 POST 给 backend 的 hack 结果请求。 */
  def hack(
    task: HackTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportHackResultRequest] =
    HackAttemptRunner.hack(task, config, problemDataCache, runtimes)
