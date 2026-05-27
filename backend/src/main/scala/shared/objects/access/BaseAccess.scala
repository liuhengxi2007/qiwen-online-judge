package shared.objects.access

enum BaseAccess:
  case OwnerOnly
  case Public

object BaseAccess:
  def parse(value: String): Either[String, BaseAccess] =
    value.trim match
      case "owner_only" => Right(BaseAccess.OwnerOnly)
      case "public" => Right(BaseAccess.Public)
      case _ => Left("Base access must be one of: owner_only, public.")
