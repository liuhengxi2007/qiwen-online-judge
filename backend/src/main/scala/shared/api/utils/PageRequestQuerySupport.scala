package shared.api.utils

import shared.objects.PageRequest

/** 查询参数分页解析工具，将 page/pageSize 从字符串请求参数转换为 PageRequest。 */
object PageRequestQuerySupport:

  /** 解析分页查询参数；缺失时使用默认值，非法或非正数字返回错误。 */
  def parsePageRequest(
    queryParams: Map[String, String],
    defaultPage: Int = 1,
    defaultPageSize: Int = 10
  ): Either[String, PageRequest] =
    for
      page <- parsePositiveInt(queryParams.get("page"), "page", defaultPage)
      pageSize <- parsePositiveInt(queryParams.get("pageSize"), "pageSize", defaultPageSize)
    yield PageRequest(page = page, pageSize = pageSize)

  private def parsePositiveInt(rawValue: Option[String], name: String, defaultValue: Int): Either[String, Int] =
    rawValue match
      case None => Right(defaultValue)
      case Some(value) =>
        value.trim.toIntOption.filter(_ > 0).toRight(s"$name must be a positive integer.")
