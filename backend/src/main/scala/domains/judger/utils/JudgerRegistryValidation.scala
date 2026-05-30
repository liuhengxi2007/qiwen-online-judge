package domains.judger.utils

import judgeprotocol.objects.request.RegisterJudgerRequest

object JudgerRegistryValidation:

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
