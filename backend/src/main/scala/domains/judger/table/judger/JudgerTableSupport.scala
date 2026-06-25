package domains.judger.table.judger



import domains.judger.objects.response.RegisteredJudgerListItem
import domains.submission.objects.SubmissionLanguage as DomainSubmissionLanguage
import judgeprotocol.objects.{JudgerId, SubmissionLanguage as ProtocolSubmissionLanguage}

/** judger 表读取辅助；负责 ResultSet 映射和支持语言列解析。 */
object JudgerTableSupport:

  /** 从当前 ResultSet 行读取管理端列表项。 */
  def readRegisteredJudgerListItem(resultSet: java.sql.ResultSet): RegisteredJudgerListItem =
    RegisteredJudgerListItem(
      judgerId = readJudgerId(resultSet.getString("judger_id"), "judger_id"),
      requestedPrefix = readJudgerId(resultSet.getString("requested_prefix"), "requested_prefix"),
      host = resultSet.getString("host"),
      processId = Option(resultSet.getString("process_id")),
      supportedLanguages = parseRegisteredJudgerLanguages(resultSet.getString("supported_languages")),
      registeredAt = resultSet.getTimestamp("registered_at").toInstant,
      lastHeartbeatAt = resultSet.getTimestamp("last_heartbeat_at").toInstant
    )

  /** 从数据库列读取 judger id；失败表示表数据已损坏，直接暴露为非法状态。 */
  private def readJudgerId(raw: String, column: String): JudgerId =
    JudgerId.parse(raw).fold(
      error => throw IllegalStateException(s"Invalid $column in judgers table: $error"),
      identity
    )

  /** 解析管理端列表展示用语言；未知值表示表数据损坏。 */
  private def parseRegisteredJudgerLanguages(raw: String): List[DomainSubmissionLanguage] =
    parseLanguageTokens(raw).map(parseDomainSubmissionLanguage)

  /** 从逗号分隔列解析 worker 协议语言；未知值表示表数据损坏。 */
  def parseSupportedLanguages(raw: String): List[ProtocolSubmissionLanguage] =
    parseLanguageTokens(raw).map(parseProtocolSubmissionLanguage)

  private def parseLanguageTokens(raw: String): List[String] =
    Option(raw)
      .toList
      .flatMap(_.split(",").toList)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def parseDomainSubmissionLanguage(raw: String): DomainSubmissionLanguage =
    DomainSubmissionLanguage.parse(raw).fold(
      error => throw IllegalStateException(s"Invalid supported language in judgers table: $error"),
      identity
    )

  private def parseProtocolSubmissionLanguage(raw: String): ProtocolSubmissionLanguage =
    raw match
      case "cpp17" => ProtocolSubmissionLanguage.Cpp17
      case "python3" => ProtocolSubmissionLanguage.Python3
      case "text" => ProtocolSubmissionLanguage.Text
      case other => throw IllegalStateException(s"Invalid supported language in judgers table: $other")
