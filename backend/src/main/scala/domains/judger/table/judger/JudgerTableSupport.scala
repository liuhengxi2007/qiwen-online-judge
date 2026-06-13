package domains.judger.table.judger



import domains.judger.objects.response.RegisteredJudgerListItem
import judgeprotocol.objects.SubmissionLanguage

/** judger 表读取辅助；负责 ResultSet 映射和支持语言列解析。 */
object JudgerTableSupport:

  /** 从当前 ResultSet 行读取管理端列表项。 */
  def readRegisteredJudgerListItem(resultSet: java.sql.ResultSet): RegisteredJudgerListItem =
    RegisteredJudgerListItem(
      judgerId = resultSet.getString("judger_id"),
      requestedPrefix = resultSet.getString("requested_prefix"),
      host = resultSet.getString("host"),
      processId = Option(resultSet.getString("process_id")),
      supportedLanguages =
        Option(resultSet.getString("supported_languages"))
          .toList
          .flatMap(_.split(",").toList)
          .map(_.trim)
          .filter(_.nonEmpty),
      registeredAt = resultSet.getTimestamp("registered_at").toInstant,
      lastHeartbeatAt = resultSet.getTimestamp("last_heartbeat_at").toInstant
    )

  /** 从逗号分隔列解析协议语言；未知语言会被忽略。 */
  def parseSupportedLanguages(raw: String): List[SubmissionLanguage] =
    raw
      .split(",")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap {
        case "cpp17" => Some(SubmissionLanguage.Cpp17)
        case "python3" => Some(SubmissionLanguage.Python3)
        case "text" => Some(SubmissionLanguage.Text)
        case _ => None
      }
