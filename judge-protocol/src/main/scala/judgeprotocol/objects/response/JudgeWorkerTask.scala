package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeWorkerTask(
  kind: String,
  judge: Option[JudgeTask],
  hack: Option[HackTask]
)

object JudgeWorkerTask:
  def judge(task: JudgeTask): JudgeWorkerTask =
    JudgeWorkerTask(kind = "judge", judge = Some(task), hack = None)

  def hack(task: HackTask): JudgeWorkerTask =
    JudgeWorkerTask(kind = "hack", judge = None, hack = Some(task))

  given Encoder[JudgeWorkerTask] = deriveEncoder[JudgeWorkerTask]
  given Decoder[JudgeWorkerTask] = deriveDecoder[JudgeWorkerTask]
