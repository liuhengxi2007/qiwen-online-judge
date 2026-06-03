package domains.contest.table.contest

import cats.effect.IO
import domains.contest.objects.*
import domains.contest.objects.response.{ContestRegistrant, ContestRegistrationStatus, ContestSummary}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import database.utils.{UserIdentityRow, UserIdentitySql}
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.sql.ResultSet

object ContestTableSupport:

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def encodeBaseAccessColumn(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"

  def decodeBaseAccessColumn(value: String): Option[BaseAccess] =
    BaseAccess.parse(value).toOption

  private def userIdentityFromRow(row: UserIdentityRow): UserIdentity =
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix).map(userIdentityFromRow)

  def readContestBase(resultSet: ResultSet): Contest =
    Contest(
      id = ContestId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("contests.slug", resultSet.getString("slug"), ContestSlug.parse),
      title = parseColumn("contests.title", resultSet.getString("title"), ContestTitle.parse),
      description = parseColumn("contests.description", resultSet.getString("description"), ContestDescription.parse),
      startAt = resultSet.getTimestamp("start_at").toInstant,
      endAt = resultSet.getTimestamp("end_at").toInstant,
      problems = Nil,
      accessPolicy = ResourceAccessPolicy(
        parseOptionalColumn("contests.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn),
        Nil,
        Nil
      ),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def readContestRegistrant(resultSet: ResultSet): ContestRegistrant =
    ContestRegistrant(
      user = readOptionalUserIdentity(resultSet, "user").getOrElse(
        throw IllegalStateException("Contest registration row is missing user identity")
      ),
      registeredAt = resultSet.getTimestamp("registered_at").toInstant
    )

  def readContestSummaryBase(resultSet: ResultSet): ContestSummary =
    ContestSummary(
      id = ContestId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("contests.slug", resultSet.getString("slug"), ContestSlug.parse),
      title = parseColumn("contests.title", resultSet.getString("title"), ContestTitle.parse),
      description = parseColumn("contests.description", resultSet.getString("description"), ContestDescription.parse),
      startAt = resultSet.getTimestamp("start_at").toInstant,
      endAt = resultSet.getTimestamp("end_at").toInstant,
      accessPolicy = ResourceAccessPolicy(
        parseOptionalColumn("contests.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn),
        Nil,
        Nil
      ),
      registrationStatus = ContestRegistrationStatus.notRegistered,
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def listProblemsForContest(connection: java.sql.Connection, contestId: ContestId, listProblemsForContestSql: String): IO[List[ContestProblemSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProblemsForContestSql)
      try
        statement.setObject(1, contestId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map { _ =>
              ContestProblemSummary(
                id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
                slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
                title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
                position = resultSet.getInt("position"),
                alias = parseColumn("contest_problems.alias", resultSet.getString("alias"), ContestProblemAlias.parse)
              )
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }
