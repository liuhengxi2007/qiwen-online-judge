package domains.contest.objects.request

import domains.problem.objects.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 添加赛题请求体，携带要加入比赛的题目 slug。 */
final case class AddProblemToContestRequest(
  problemSlug: ProblemSlug
)

/** 提供添加赛题请求体 JSON codec。 */
object AddProblemToContestRequest:
  given Encoder[AddProblemToContestRequest] = deriveEncoder[AddProblemToContestRequest]
  given Decoder[AddProblemToContestRequest] = deriveDecoder[AddProblemToContestRequest]
