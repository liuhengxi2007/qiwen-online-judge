package domains.judger.http.mapper

import judgeprotocol.model.JudgerId

object JudgerRegistryHttpRequestMappers:

  def judgerId(rawJudgerId: String): Either[String, JudgerId] =
    JudgerId.parse(rawJudgerId)
