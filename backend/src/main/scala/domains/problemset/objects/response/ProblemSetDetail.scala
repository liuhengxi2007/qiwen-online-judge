package domains.problemset.objects.response

import domains.problemset.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

final case class ProblemSetDetail(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

object ProblemSetDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetDetail] = deriveEncoder[ProblemSetDetail]
  given Decoder[ProblemSetDetail] = deriveDecoder[ProblemSetDetail]

  def fromProblemSet(problemSet: ProblemSet): ProblemSetDetail =
    ProblemSetDetail(
      id = problemSet.id,
      slug = problemSet.slug,
      title = problemSet.title,
      description = problemSet.description,
      problems = problemSet.problems,
      accessPolicy = problemSet.accessPolicy,
      author = problemSet.author,
      createdAt = problemSet.createdAt,
      updatedAt = problemSet.updatedAt
    )
