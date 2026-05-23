package domains.judger.table.judger



import domains.judger.application.output.RegisteredJudgerListItem
import judgeprotocol.model.SubmissionLanguage

object JudgerTableSupport:

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

  def parseSupportedLanguages(raw: String): List[SubmissionLanguage] =
    raw
      .split(",")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap {
        case "cpp17" => Some(SubmissionLanguage.Cpp17)
        case "python3" => Some(SubmissionLanguage.Python3)
        case _ => None
      }
