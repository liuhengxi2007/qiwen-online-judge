package shared.api

import java.net.URLDecoder

final case class ApiPath private (segments: List[ApiPath.Segment]):

  def matchParams(rawPath: String): Option[PathParams] =
    val requestSegments = ApiPath.pathSegments(rawPath)
    if requestSegments.length != segments.length then None
    else
      segments
        .zip(requestSegments)
        .foldLeft(Option(Map.empty[String, String])) {
          case (None, _) =>
            None
          case (Some(params), (ApiPath.Segment.Static(expected), actual)) if expected == actual =>
            Some(params)
          case (Some(params), (ApiPath.Segment.Param(name), actual)) =>
            Some(params.updated(name, actual))
          case _ =>
            None
        }
        .map(PathParams(_))

object ApiPath:

  enum Segment:
    case Static(value: String)
    case Param(name: String)

  def apply(pattern: String): ApiPath =
    ApiPath(
      pathSegments(pattern).map {
        case value if value.startsWith(":") =>
          Segment.Param(value.drop(1))
        case value =>
          Segment.Static(value)
      }
    )

  private def pathSegments(path: String): List[String] =
    path
      .split('/')
      .toList
      .filter(_.nonEmpty)
      .map(segment => URLDecoder.decode(segment, "UTF-8"))

final case class PathParams(values: Map[String, String]):

  def get(name: String): Option[String] =
    values.get(name)

  def require(name: String): Either[String, String] =
    values.get(name).toRight(s"Missing path parameter: $name.")
