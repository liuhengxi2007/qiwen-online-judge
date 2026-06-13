package domains.blog.objects



import domains.problem.objects.{ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 博客已接受关联题目的轻量引用，用于列表和详情展示。 */
final case class BlogProblemReference(
  slug: ProblemSlug,
  title: ProblemTitle
)

/** 提供博客关联题目引用 JSON codec。 */
object BlogProblemReference:
  given Encoder[BlogProblemReference] = deriveEncoder[BlogProblemReference]
  given Decoder[BlogProblemReference] = deriveDecoder[BlogProblemReference]
