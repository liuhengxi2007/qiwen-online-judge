package judger.application

import cats.effect.IO
import cats.effect.kernel.Ref
import judgeprotocol.objects.{SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.ReportJudgeResultRequest
import judgeprotocol.objects.response.{JudgeFailureReason, JudgeTask}
import judger.config.AppConfig
import judger.http.JudgeHttpClient
import judger.infra.{Cpp17Runtime, JudgeExecutor, ProblemDataCache, Python3Runtime}
import judger.objects.RegisteredJudger
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
      JudgeExecutor.judge(
        task,
        config,
        problemDataCache,
        Map(
          SubmissionLanguage.Cpp17 -> Cpp17Runtime,
          SubmissionLanguage.Python3 -> Python3Runtime
        )
      )

    resultIo.flatMap { result =>
      httpClient.reportResult(task.submissionId, result) *>
        logResult(task, result)
    }

  private def logResult(task: JudgeTask, result: ReportJudgeResultRequest): IO[Unit] =
    val status = SubmissionStatus.render(result.status)
    val resultVerdict = result.judgeResult.map(_.verdict)
    val verdict = resultVerdict.map(SubmissionVerdict.render).getOrElse("pending")
    val summary =
      s"[judger] Submission #${task.submissionId.value} finished with status=$status verdict=$verdict."

    (result.status, resultVerdict) match
      case (SubmissionStatus.Failed, _) | (_, Some(SubmissionVerdict.SystemError)) =>
        logger.error(s"$summary ${result.judgeResult.flatMap(_.reason).map(JudgeFailureReason.render).getOrElse("")}".trim)
      case _ =>
        IO.unit
