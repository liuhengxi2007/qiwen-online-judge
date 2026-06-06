package judger.application

import cats.effect.IO
import cats.effect.kernel.Ref
import judgeprotocol.objects.{SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judgeprotocol.objects.request.{ReportHackResultRequest, ReportJudgeResultRequest}
import judgeprotocol.objects.response.{HackTask, JudgeFailureReason, JudgeTask, JudgeWorkerTask}
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

  private def handleTask(task: JudgeWorkerTask): IO[Unit] =
    task.kind match
      case "judge" =>
        task.judge match
          case Some(judgeTask) => handleJudgeTask(judgeTask)
          case None => logger.error("[judger] Claimed judge task payload was missing judge data.")
      case "hack" =>
        task.hack match
          case Some(hackTask) => handleHackTask(hackTask)
          case None => logger.error("[judger] Claimed hack task payload was missing hack data.")
      case other =>
        logger.error(s"[judger] Claimed unsupported task kind: $other.")

  private def runtimes =
    Map(
      SubmissionLanguage.Cpp17 -> Cpp17Runtime,
      SubmissionLanguage.Python3 -> Python3Runtime
    )

  private def handleJudgeTask(task: JudgeTask): IO[Unit] =
    JudgeExecutor
      .judge(task, config, problemDataCache, runtimes)
      .flatMap { result =>
        httpClient.reportResult(task.submissionId, result) *>
          logResult(task, result)
      }

  private def handleHackTask(task: HackTask): IO[Unit] =
    JudgeExecutor
      .hack(task, config, problemDataCache, runtimes)
      .flatMap { result =>
        httpClient.reportHackResult(task.hackId, result) *>
          logHackResult(task, result)
      }

  private def logResult(task: JudgeTask, result: ReportJudgeResultRequest): IO[Unit] =
    val status = SubmissionStatus.render(result.status)
    val resultVerdict = result.judgeResult.map(_.baseResult.verdict)
    val verdict = resultVerdict.map(SubmissionVerdict.render).getOrElse("pending")
    val summary =
      s"[judger] Submission #${task.submissionId.value} finished with status=$status verdict=$verdict."

    (result.status, resultVerdict) match
      case (SubmissionStatus.Failed, _) | (_, Some(SubmissionVerdict.SystemError)) =>
        logger.error(s"$summary ${result.judgeResult.flatMap(_.baseResult.reason).map(JudgeFailureReason.render).getOrElse("")}".trim)
      case _ =>
        IO.unit

  private def logHackResult(task: HackTask, result: ReportHackResultRequest): IO[Unit] =
    val summary =
      s"[judger] Hack #${task.hackId} finished with status=${result.status} oldScore=${result.oldScore} newScore=${result.newScore.map(_.toString).getOrElse("n/a")}."
    result.status match
      case "failed" => logger.error(summary)
      case _ => IO.unit
