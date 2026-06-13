package judgeprotocol.objects.response

import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.deriveEncoder

/** worker claim 接口返回的联合任务载体，通过 kind 区分普通判题和 hack 判题。 */
final case class JudgeWorkerTask(
  kind: String,
  judge: Option[JudgeTask],
  hack: Option[HackTask]
)

/** 提供 worker 任务联合类型的构造函数和协议编解码。 */
object JudgeWorkerTask:
  /** 构造普通提交判题任务；输出中仅填充 judge 字段。 */
  def judge(task: JudgeTask): JudgeWorkerTask =
    JudgeWorkerTask(kind = "judge", judge = Some(task), hack = None)

  /** 构造 hack 尝试判题任务；输出中仅填充 hack 字段。 */
  def hack(task: HackTask): JudgeWorkerTask =
    JudgeWorkerTask(kind = "hack", judge = None, hack = Some(task))

  given Encoder[JudgeWorkerTask] = deriveEncoder[JudgeWorkerTask]
  given Decoder[JudgeWorkerTask] = Decoder.instance { cursor =>
    for
      kind <- cursor.downField("kind").as[String]
      judge <- cursor.downField("judge").as[Option[JudgeTask]]
      hack <- cursor.downField("hack").as[Option[HackTask]]
      _ <- kind match
        case "judge" if judge.nonEmpty && hack.isEmpty => Right(())
        case "hack" if hack.nonEmpty && judge.isEmpty => Right(())
        case "judge" => Left(DecodingFailure("judge worker task requires judge payload only", cursor.history))
        case "hack" => Left(DecodingFailure("hack worker task requires hack payload only", cursor.history))
        case other => Left(DecodingFailure(s"Unsupported worker task kind: $other", cursor.history))
    yield JudgeWorkerTask(kind = kind, judge = judge, hack = hack)
  }
