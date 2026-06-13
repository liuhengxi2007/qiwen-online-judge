package domains.user.objects



import domains.problem.objects.{ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 用户已通过题目记录，用于个人资料页展示最近 AC 题目。 */
final case class UserAcceptedProblem(
  slug: ProblemSlug,
  title: ProblemTitle,
  acceptedAt: Instant
)

/** 提供已通过题目记录 JSON 编解码，并以 ISO 字符串传输时间。 */
object UserAcceptedProblem:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[UserAcceptedProblem] = deriveEncoder[UserAcceptedProblem]
  given Decoder[UserAcceptedProblem] = deriveDecoder[UserAcceptedProblem]
