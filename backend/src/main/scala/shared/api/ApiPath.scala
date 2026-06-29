package shared.api

import java.net.URLDecoder

/** API 路径模式，支持静态段和 :param 参数段匹配。 */
final case class ApiPath private (segments: List[ApiPath.Segment]):

  /** 尝试匹配请求路径，成功时返回解码后的路径参数，段数或静态段不符则为 None。 */
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

/** 构造 API 路径模式并定义内部路径段类型。 */
object ApiPath:

  /** 路径模式中的单段定义，区分字面量和命名参数。 */
  enum Segment:
    case Static(value: String)
    case Param(name: String)

  /** 从 /api/:id 形式的模式创建 ApiPath，路径段会按 UTF-8 URL 解码。 */
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

/** 已匹配的路径参数集合，用于 API decode 阶段读取必需参数。 */
final case class PathParams(values: Map[String, String]):

  /** 获取可选路径参数。 */
  def get(name: String): Option[String] =
    values.get(name)

  /** 获取必需路径参数，缺失时返回可映射为 400 的错误信息。 */
  def require(name: String): Either[String, String] =
    values.get(name).toRight(s"Missing path parameter: $name.")
