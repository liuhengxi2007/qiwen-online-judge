package domains.problemset.objects.response

import domains.problemset.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant
import scala.util.Try

/** 题单详情响应，包含题目列表、访问策略和作者信息。 */
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

/** 提供题单详情 JSON codec，以及从题单聚合构造响应的转换。 */
object ProblemSetDetail:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[ProblemSetDetail] = deriveEncoder[ProblemSetDetail]
  given Decoder[ProblemSetDetail] = deriveDecoder[ProblemSetDetail]

  /** 将题单聚合转换为接口详情响应。 */
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
