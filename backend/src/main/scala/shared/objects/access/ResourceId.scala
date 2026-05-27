package shared.objects.access

import java.util.UUID
import scala.util.Try

final case class ResourceId(value: UUID)

object ResourceId:
  def parse(value: String): Either[String, ResourceId] =
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage).map(ResourceId(_))
