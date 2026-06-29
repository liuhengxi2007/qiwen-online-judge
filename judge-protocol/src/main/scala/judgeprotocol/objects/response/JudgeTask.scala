package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{ProblemSlug, SubmissionId}

/** backend 下发给 judger 的完整提交判题任务，包含程序、数据版本、聚合规则和子任务树。 */
final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  startedAtEpochMilli: Long,
  programs: Map[String, JudgeTaskProgram],
  problemDataVersion: String,
  roundingScale: Int,
  aggregation: JudgeTaskAggregation,
  subtasks: List[JudgeTaskSubtask]
)

/** 负责完整判题任务的协议编解码；字段形状必须与 backend 构造器保持一致。 */
object JudgeTask:
  given Encoder[JudgeTask] = deriveEncoder[JudgeTask]
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]
