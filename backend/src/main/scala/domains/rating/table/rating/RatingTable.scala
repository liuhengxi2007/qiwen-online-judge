package domains.rating.table.rating

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.contest.objects.{ContestSlug, ContestTitle}
import domains.rating.objects.RatingValue
import domains.rating.objects.internal.RatingContestSnapshot
import domains.rating.objects.response.{RatingContestListItem, RatingRanklistItem}
import domains.rating.utils.RatingCalculator
import domains.rating.utils.RatingCalculator.{RatingContestEvent, RankedParticipant}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import io.circe.parser.decode
import io.circe.syntax.*
import shared.objects.{PageRequest, PageResponse}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

/** 评分表访问对象，负责维护评分比赛序列、重算用户粒子状态和读取排行榜。 */
object RatingTable:

  /** 追加评分比赛的表层结果，区分成功和重复追加。 */
  enum AppendContestResult:
    case AlreadyAppended
    case Appended

  /** 追加评分比赛时写入 rating_contests 的完整记录。 */
  final case class AppendContestRecord(
    contestSlug: ContestSlug,
    contestTitle: ContestTitle,
    contestStartAt: Instant,
    contestEndAt: Instant,
    m: Int,
    participantCount: Int,
    overlapWarning: Boolean,
    snapshot: RatingContestSnapshot,
    appendedBy: Username,
    appendedAt: Instant
  )

  /** 从数据库读取并参与重算的评分比赛事件。 */
  final case class StoredContestEvent(
    position: Int,
    m: Int,
    snapshot: RatingContestSnapshot
  )

  /** 初始化评分比赛序列表和用户当前评分状态表。 */
  def initialize(connection: Connection): IO[Unit] =
    RatingTableSchema.initialize(connection)

  /** 在事务级 advisory lock 下追加比赛并重算所有当前用户评分状态。 */
  def appendContest(connection: Connection, record: AppendContestRecord): IO[AppendContestResult] =
    for
      _ <- lockSequence(connection)
      alreadyAppended <- contestExists(connection, record.contestSlug)
      result <- if alreadyAppended then IO.pure(AppendContestResult.AlreadyAppended)
      else
        for
          position <- nextPosition(connection)
          _ <- insertContest(connection, position, record)
          _ <- rebuildCurrentStates(connection)
        yield AppendContestResult.Appended
    yield result

  /** 在事务级 advisory lock 下删除评分序列最后一场比赛，成功删除后重算评分状态。 */
  def popLatestContest(connection: Connection): IO[Boolean] =
    for
      _ <- lockSequence(connection)
      deleted <- deleteLatestContest(connection)
      _ <- if deleted then rebuildCurrentStates(connection) else IO.unit
    yield deleted

  private val advisoryLockKey: Long = 7_202_406_071L

  /** 获取评分序列事务级 advisory lock，串行化追加、弹出和重建操作。 */
  private def lockSequence(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement("select pg_advisory_xact_lock(?)")
      try
        statement.setLong(1, advisoryLockKey)
        statement.execute()
      finally statement.close()
    }

  private val nextPositionSql: String =
    """
      |select coalesce(max(position), 0) + 1 as next_position
      |from rating_contests
      |""".stripMargin

  /** 读取下一场评分比赛的位置编号；调用方需已持有评分序列锁。 */
  private def nextPosition(connection: Connection): IO[Int] =
    IO.blocking {
      val statement = connection.prepareStatement(nextPositionSql)
      try
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getInt("next_position") else 1
        finally resultSet.close()
      finally statement.close()
    }

  private val contestExistsSql: String =
    """
      |select 1
      |from rating_contests
      |where contest_slug = ?
      |limit 1
      |""".stripMargin

  private def contestExists(connection: Connection, contestSlug: ContestSlug): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(contestExistsSql)
      try
        statement.setString(1, contestSlug.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val insertContestSql: String =
    """
      |insert into rating_contests (
      |  position,
      |  contest_slug,
      |  contest_title,
      |  contest_start_at,
      |  contest_end_at,
      |  rating_m,
      |  participant_count,
      |  overlap_warning,
      |  ranking_snapshot_json,
      |  appended_by_username,
      |  appended_at
      |)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  private def insertContest(connection: Connection, position: Int, record: AppendContestRecord): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(insertContestSql)
      try
        statement.setInt(1, position)
        statement.setString(2, record.contestSlug.value)
        statement.setString(3, record.contestTitle.value)
        statement.setTimestamp(4, Timestamp.from(record.contestStartAt))
        statement.setTimestamp(5, Timestamp.from(record.contestEndAt))
        statement.setInt(6, record.m)
        statement.setInt(7, record.participantCount)
        statement.setBoolean(8, record.overlapWarning)
        statement.setString(9, record.snapshot.asJson.noSpaces)
        statement.setString(10, record.appendedBy.value)
        statement.setTimestamp(11, Timestamp.from(record.appendedAt))
        statement.executeUpdate()
      finally statement.close()
    }

  private val deleteLatestContestSql: String =
    """
      |delete from rating_contests
      |where position = (
      |  select max(position)
      |  from rating_contests
      |)
      |""".stripMargin

  private def deleteLatestContest(connection: Connection): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteLatestContestSql)
      try statement.executeUpdate() > 0
      finally statement.close()
    }

  private val listContestEventsSql: String =
    """
      |select position, rating_m, ranking_snapshot_json
      |from rating_contests
      |order by position asc
      |""".stripMargin

  private def listContestEvents(connection: Connection): IO[List[StoredContestEvent]] =
    IO.blocking {
      val statement = connection.prepareStatement(listContestEventsSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readStoredContestEvent(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val listExistingUsersSql: String =
    """
      |select username
      |from auth_accounts
      |""".stripMargin

  private def listExistingUsers(connection: Connection): IO[Set[Username]] =
    IO.blocking {
      val statement = connection.prepareStatement(listExistingUsersSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => Username.canonical(resultSet.getString("username")))
            .toSet
        finally resultSet.close()
      finally statement.close()
    }

  private def rebuildCurrentStates(connection: Connection): IO[Unit] =
    for
      events <- listContestEvents(connection)
      existingUsers <- listExistingUsers(connection)
      calculatorEvents = events.map(event =>
        RatingContestEvent(
          participants = event.snapshot.participants.map(participant => RankedParticipant(participant.username, participant.rank)),
          m = event.m
        )
      )
      states = RatingCalculator.applyContestSequence(calculatorEvents, existingUsers)
      _ <- replaceUserStates(connection, states)
    yield ()

  private val deleteUserStatesSql: String =
    """
      |delete from rating_user_states
      |""".stripMargin

  private val insertUserStateSql: String =
    """
      |insert into rating_user_states (username, particles_json, current_rating, updated_at)
      |values (?, ?, ?, ?)
      |""".stripMargin

  private def replaceUserStates(connection: Connection, states: Map[Username, Vector[Double]]): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val deleteStatement = connection.prepareStatement(deleteUserStatesSql)
      try deleteStatement.executeUpdate()
      finally deleteStatement.close()

      val insertStatement = connection.prepareStatement(insertUserStateSql)
      try
        states.toSeq.sortBy(_._1.value).foreach { case (username, particles) =>
          insertStatement.setString(1, username.value)
          insertStatement.setString(2, particles.asJson.noSpaces)
          insertStatement.setBigDecimal(3, BigDecimal(RatingCalculator.ratingOf(particles)).bigDecimal)
          insertStatement.setTimestamp(4, Timestamp.from(now))
          insertStatement.addBatch()
        }
        insertStatement.executeBatch()
      finally insertStatement.close()
    }

  private val listManageContestsSql: String =
    s"""
      |select
      |  rc.position,
      |  rc.contest_slug,
      |  rc.contest_title,
      |  rc.contest_start_at,
      |  rc.contest_end_at,
      |  rc.rating_m,
      |  rc.participant_count,
      |  rc.overlap_warning,
      |  rc.appended_at,
      |  ${UserIdentitySql.selectOptionalColumns("rc.appended_by_username", "appended_by", "up")}
      |from rating_contests rc
      |${UserIdentitySql.leftJoinUserProfiles("rc.appended_by_username", "up")}
      |order by rc.position asc
      |""".stripMargin

  /** 读取评分管理页比赛序列，按追加位置升序返回。 */
  def listManageContests(connection: Connection): IO[List[RatingContestListItem]] =
    IO.blocking {
      val statement = connection.prepareStatement(listManageContestsSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readContestListItem(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val findLastContestEndAtSql: String =
    """
      |select contest_end_at
      |from rating_contests
      |order by position desc
      |limit 1
      |""".stripMargin

  /** 读取评分序列中最后一场比赛的结束时间，用于提示时间重叠。 */
  def findLastContestEndAt(connection: Connection): IO[Option[Instant]] =
    IO.blocking {
      val statement = connection.prepareStatement(findLastContestEndAtSql)
      try
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(resultSet.getTimestamp("contest_end_at").toInstant)
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val findUserRatingSql: String =
    """
      |select current_rating
      |from rating_user_states
      |where username = ?
      |""".stripMargin

  /** 读取用户当前评分；用户没有评分状态时返回初始评分。 */
  def findUserRating(connection: Connection, username: Username): IO[RatingValue] =
    IO.blocking {
      val statement = connection.prepareStatement(findUserRatingSql)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then RatingValue(BigDecimal(resultSet.getBigDecimal("current_rating")))
          else RatingValue.initial
        finally resultSet.close()
      finally statement.close()
    }

  private val countRanklistItemsSql: String =
    """
      |select count(*) as total_items
      |from rating_user_states
      |""".stripMargin

  private def countRanklistItems(connection: Connection): IO[Long] =
    IO.blocking {
      val statement = connection.prepareStatement(countRanklistItemsSql)
      try
        val resultSet = statement.executeQuery()
        try if resultSet.next() then resultSet.getLong("total_items") else 0L
        finally resultSet.close()
      finally statement.close()
    }

  private val listRanklistSql: String =
    s"""
      |select
      |  rus.current_rating,
      |  rus.username as user_username,
      |  up.display_name as user_display_name
      |from rating_user_states rus
      |${UserIdentitySql.joinUserProfiles("rus.username", "up")}
      |order by rus.current_rating desc, rus.username asc
      |limit ? offset ?
      |""".stripMargin

  /** 分页读取当前评分排行榜，按 rating 降序和用户名升序稳定排序。 */
  def listRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[RatingRanklistItem]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      totalItems <- countRanklistItems(connection)
      items <- IO.blocking {
        val statement = connection.prepareStatement(listRanklistSql)
        try
          statement.setInt(1, normalizedPageRequest.pageSize)
          statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readRanklistItem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(items = items, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = totalItems)

  private def readStoredContestEvent(resultSet: ResultSet): StoredContestEvent =
    StoredContestEvent(
      position = resultSet.getInt("position"),
      m = resultSet.getInt("rating_m"),
      /** 注意：评分快照 JSON 来自系统写入的历史记录；解码失败表示持久化数据损坏，应暴露为内部异常而非业务错误。 */
      snapshot = decode[RatingContestSnapshot](resultSet.getString("ranking_snapshot_json"))
        .fold(error => throw IllegalStateException(s"Invalid rating contest snapshot JSON: ${error.getMessage}"), identity)
    )

  private def readContestListItem(resultSet: ResultSet): RatingContestListItem =
    RatingContestListItem(
      position = resultSet.getInt("position"),
      contestSlug = ContestSlug(resultSet.getString("contest_slug")),
      contestTitle = ContestTitle(resultSet.getString("contest_title")),
      contestStartAt = resultSet.getTimestamp("contest_start_at").toInstant,
      contestEndAt = resultSet.getTimestamp("contest_end_at").toInstant,
      m = resultSet.getInt("rating_m"),
      participantCount = resultSet.getInt("participant_count"),
      overlapWarning = resultSet.getBoolean("overlap_warning"),
      appendedBy = readOptionalUserIdentity(resultSet, "appended_by"),
      appendedAt = resultSet.getTimestamp("appended_at").toInstant
    )

  private def readRanklistItem(resultSet: ResultSet): RatingRanklistItem =
    RatingRanklistItem(
      user = readUserIdentity(resultSet, "user"),
      rating = RatingValue(BigDecimal(resultSet.getBigDecimal("current_rating")))
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    val row = UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix)
    row.map(value => UserIdentity(username = Username.canonical(value.username), displayName = DisplayName(value.displayName)))

  private def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(username = Username.canonical(row.username), displayName = DisplayName(row.displayName))
