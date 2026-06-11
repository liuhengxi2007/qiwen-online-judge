package judger.infra

import cats.effect.IO
import judgeprotocol.objects.SubmissionLanguage
import judgeprotocol.objects.request.{ReportHackResultRequest, ReportJudgeResultRequest}
import judgeprotocol.objects.response.{HackTask, JudgeTask}
import judger.config.AppConfig

object JudgeExecutor:
  def judge(
    task: JudgeTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportJudgeResultRequest] =
    SubmissionJudgeRunner.judge(task, config, problemDataCache, runtimes)

  def hack(
    task: HackTask,
    config: AppConfig,
    problemDataCache: ProblemDataCache,
    runtimes: Map[SubmissionLanguage, JudgeRuntime]
  ): IO[ReportHackResultRequest] =
    HackAttemptRunner.hack(task, config, problemDataCache, runtimes)
