package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskMode(
  `type`: String,
  role: Option[String],
  roles: List[String],
  interactor: Option[JudgeTaskFileRef]
)

object JudgeTaskMode:
  def traditional(role: String): JudgeTaskMode =
    JudgeTaskMode("traditional", Some(role), Nil, None)

  def interactive(roles: List[String], interactor: JudgeTaskFileRef): JudgeTaskMode =
    JudgeTaskMode("interactive", None, roles, Some(interactor))

  given Encoder[JudgeTaskMode] = deriveEncoder[JudgeTaskMode]
  given Decoder[JudgeTaskMode] = deriveDecoder[JudgeTaskMode]
