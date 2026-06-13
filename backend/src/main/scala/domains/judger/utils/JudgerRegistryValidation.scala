package domains.judger.utils

import judgeprotocol.objects.request.RegisterJudgerRequest

/** judger 注册请求校验；规范化主机和进程 id，并要求至少一个支持语言。 */
object JudgerRegistryValidation:

  /** 校验并规范化注册请求；不访问数据库。 */
  def validateRegisterRequest(request: RegisterJudgerRequest): Either[String, RegisterJudgerRequest] =
    val host = request.host.trim
    if host.isEmpty then Left("Judger host is required.")
    else if request.supportedLanguages.isEmpty then Left("Judger supported languages are required.")
    else
      Right(
        request.copy(
          host = host,
          processId = request.processId.map(_.trim).filter(_.nonEmpty)
        )
      )
