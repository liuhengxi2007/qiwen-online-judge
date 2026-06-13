package shared.api.utils

import shared.objects.PageRequest

/** 查询参数分页解析工具，将 page/pageSize 从字符串请求参数转换为 PageRequest。 */
object PageRequestQuerySupport:

  /** 解析分页查询参数；非法或非正数字回退到调用方提供的默认值。 */
  def parsePageRequest(
    queryParams: Map[String, String],
    defaultPage: Int = 1,
    defaultPageSize: Int = 10
  ): PageRequest =
    /** FIXME-CN: 非数字或非正分页参数会静默回退默认值，调用方无法区分用户输入错误和未传参数。 */
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), defaultPage),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), defaultPageSize)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)
