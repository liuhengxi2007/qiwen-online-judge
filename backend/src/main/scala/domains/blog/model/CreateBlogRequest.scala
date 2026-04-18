package domains.blog.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility,
  blogType: BlogType,
  problemSlug: Option[domains.problem.model.ProblemSlug]
)

object CreateBlogRequest:
  given Encoder[CreateBlogRequest] = deriveEncoder[CreateBlogRequest]
  given Decoder[CreateBlogRequest] = deriveDecoder[CreateBlogRequest]
