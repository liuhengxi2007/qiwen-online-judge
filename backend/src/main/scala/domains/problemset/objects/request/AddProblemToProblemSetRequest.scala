package domains.problemset.objects.request


import domains.problem.objects.ProblemSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 添加题单题目请求体，携带要加入题单的题目 slug。 */
final case class AddProblemToProblemSetRequest(
  problemSlug: ProblemSlug
)

/** 提供添加题单题目请求体 JSON codec。 */
object AddProblemToProblemSetRequest:
  given Encoder[AddProblemToProblemSetRequest] = deriveEncoder[AddProblemToProblemSetRequest]
  given Decoder[AddProblemToProblemSetRequest] = deriveDecoder[AddProblemToProblemSetRequest]
