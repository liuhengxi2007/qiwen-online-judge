package judger.application

import cats.effect.IO
import cats.effect.kernel.Ref
import judgeprotocol.objects.{JudgeTask, ReportJudgeResultRequest, SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judger.config.{AppConfig, RegisteredJudger}
import judger.infra.{Cpp17Runtime, JudgeExecutor, JudgeHttpClient, ProblemDataCache, Python3Runtime}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationLong

final class JudgerService(
  config: AppConfig,
  registeredJudgerRef: Ref[IO, RegisteredJudger],
  httpClient: JudgeHttpClient,
  problemDataCache: ProblemDataCache,
  logger: Logger[IO]
):
  def runForever: IO[Nothing] =
    iteration.foreverM

  private def iteration: IO[Unit] =
    processOnce.handleErrorWith { error =>
      logger.error(error)(s"[judger] Unhandled execution error: ${Option(error.getMessage).getOrElse(error.getClass.getName)}")
    } *> IO.sleep(config.pollIntervalMs.millis)

  private def processOnce: IO[Unit] =
    registeredJudgerRef.get.flatMap(registeredJudger => httpClient.claimTask(registeredJudger.judgerId)).flatMap {
      case None =>
        IO.unit
      case Some(task) =>
        handleTask(task)
    }

  private def handleTask(task: JudgeTask): IO[Unit] =
    val resultIo =
      task.language match
        case SubmissionLanguage.Cpp17 =>
          JudgeExecutor.judge(task, config, problemDataCache, Cpp17Runtime)
        case SubmissionLanguage.Python3 =>
          JudgeExecutor.judge(task, config, problemDataCache, Python3Runtime)

    resultIo.flatMap { result =>
      httpClient.reportResult(task.submissionId, result) *>
        logResult(task, result)
    }

  private def logResult(task: JudgeTask, result: ReportJudgeResultRequest): IO[Unit] =
    val status = SubmissionStatus.render(result.status)
    val verdict = result.verdict.map(SubmissionVerdict.render).getOrElse("pending")
    val summary =
      s"[judger] Submission #${task.submissionId.value} (${SubmissionLanguage.render(task.language)}) finished with status=$status verdict=$verdict."

    (result.status, result.verdict) match
      case (SubmissionStatus.Failed, _) | (_, Some(SubmissionVerdict.SystemError)) =>
        logger.error(s"$summary ${result.judgeMessage.getOrElse("")}".trim)
      case _ =>
        IO.unit
