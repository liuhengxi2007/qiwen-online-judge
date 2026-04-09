package judger.application

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, ReportJudgeResultRequest, SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import judger.config.{AppConfig, RegisteredJudger}
import judger.infra.{Cpp17JudgeExecutor, JudgeHttpClient}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationLong

final class JudgerService(
  config: AppConfig,
  registeredJudger: RegisteredJudger,
  httpClient: JudgeHttpClient,
  logger: Logger[IO]
):
  def runForever: IO[Nothing] =
    iteration.foreverM

  private def iteration: IO[Unit] =
    processOnce.handleErrorWith { error =>
      logger.error(error)(s"[judger] ${error.getMessage}")
    } *> IO.sleep(config.pollIntervalMs.millis)

  private def processOnce: IO[Unit] =
    httpClient.claimTask(registeredJudger.judgerId).flatMap {
      case None =>
        IO.unit
      case Some(task) =>
        logger.info(
          s"[judger] Claimed submission #${task.submissionId.value} (${SubmissionLanguage.render(task.language)}) for problem ${task.problemSlug.value}."
        ) *> handleTask(task)
    }

  private def handleTask(task: JudgeTask): IO[Unit] =
    val resultIo =
      task.language match
        case SubmissionLanguage.Cpp17 =>
          Cpp17JudgeExecutor.judge(task, config)
        case other =>
          IO.pure(
            ReportJudgeResultRequest(
              status = SubmissionStatus.Failed,
              verdict = Some(SubmissionVerdict.SystemError),
              judgeMessage = Some(s"Unsupported language on this judger: ${SubmissionLanguage.render(other)}.")
            )
          )

    resultIo.flatMap { result =>
      httpClient.reportResult(task.submissionId, result) *>
        logger.info(
          s"[judger] Finished submission #${task.submissionId.value} with status=${SubmissionStatus.render(result.status)}, verdict=${result.verdict.map(SubmissionVerdict.render).getOrElse("pending")}."
        )
    }
