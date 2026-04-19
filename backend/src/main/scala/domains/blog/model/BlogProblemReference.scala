package domains.blog.model

import domains.problem.model.{ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class BlogProblemReference(
  slug: ProblemSlug,
  title: ProblemTitle
)

object BlogProblemReference:
  given Encoder[BlogProblemReference] = deriveEncoder[BlogProblemReference]
  given Decoder[BlogProblemReference] = deriveDecoder[BlogProblemReference]
