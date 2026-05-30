package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionLanguage, SubmissionSourceCode}

final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  problemDataVersion: String,
  roundingScale: Int,
  aggregation: JudgeTaskAggregation,
  subtasks: List[JudgeTaskSubtask]
)

object JudgeTask:
  given Encoder[JudgeTask] = deriveEncoder[JudgeTask]
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]
