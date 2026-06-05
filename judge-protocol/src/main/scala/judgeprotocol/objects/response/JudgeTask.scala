package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{ProblemSlug, SubmissionId}

final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  programs: Map[String, JudgeTaskProgram],
  problemDataVersion: String,
  roundingScale: Int,
  aggregation: JudgeTaskAggregation,
  subtasks: List[JudgeTaskSubtask]
)

object JudgeTask:
  given Encoder[JudgeTask] = deriveEncoder[JudgeTask]
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]
