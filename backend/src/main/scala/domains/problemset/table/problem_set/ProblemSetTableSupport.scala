package domains.problemset.table.problem_set



import cats.effect.IO
import database.utils.ResourceAccessTableSupport.{decodeBaseAccessColumn, parseColumn, parseOptionalColumn}
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.problemset.objects.{ProblemSet, ProblemSetDescription, ProblemSetId, ProblemSetProblemSummary, ProblemSetSlug, ProblemSetTitle}
import domains.problemset.objects.response.ProblemSetSummary
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.access.{ResourceAccessPolicy, ResourceId}
import database.utils.{UserIdentityRow, UserIdentitySql}

import java.sql.{PreparedStatement, ResultSet}

object ProblemSetTableSupport:

  private def userIdentityFromRow(row: UserIdentityRow): UserIdentity =
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  private def readOptionalUserIdentity(resultSet: ResultSet, prefix: String): Option[UserIdentity] =
    UserIdentitySql.readOptionalUserIdentityRow(resultSet, prefix).map(userIdentityFromRow)

  def readProblemSetSummaryBase(resultSet: ResultSet): ProblemSetSummary =
    ProblemSetSummary(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def readProblemSetDetailBase(resultSet: ResultSet): ProblemSet =
    ProblemSet(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      problems = Nil,
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn), Nil, Nil),
      author = readOptionalUserIdentity(resultSet, "author"),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def listProblemsForSet(connection: java.sql.Connection, problemSetId: ProblemSetId, listProblemsForSetSql: String): IO[List[ProblemSetProblemSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProblemsForSetSql)
      try
        statement.setObject(1, problemSetId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map { _ =>
              ProblemSetProblemSummary(
                id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
                slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
                title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
                position = resultSet.getInt("position")
              )
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def bindAccessQuery(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    statement.setBoolean(1, actor.siteManager || actor.problemManager)
    statement.setString(2, actor.username.value)
    statement.setString(3, actor.username.value)
    pageSize.foreach(statement.setInt(4, _))
    offset.foreach(statement.setInt(5, _))

  def toResourceId(problemSetId: ProblemSetId): ResourceId =
    ResourceId(problemSetId.value)
