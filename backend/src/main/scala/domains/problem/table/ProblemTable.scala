package domains.problem.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{CreateProblemRequest, Problem, ProblemId, ProblemSlug, ProblemStatementText, ProblemSummary, ProblemTitle, UpdateProblemRequest}
import domains.shared.model.PageResponse
import domains.shared.model.{ResourceStatus, ResourceVisibility}

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

object ProblemTable:

  val initTableSql: String =
    """
      |create table if not exists problems (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  statement_text text not null,
      |  visibility varchar(32) not null check (visibility in ('private', 'group', 'public')),
      |  status varchar(32) not null check (status in ('draft', 'published', 'archived')),
      |  owner_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val listSql: String =
    """
      |select id, slug, title, visibility, status, owner_username, created_at, updated_at
      |from problems
      |order by updated_at desc, slug asc
      |limit ? offset ?
      |""".stripMargin

  val countSql: String =
    """
      |select count(*) as total_items
      |from problems
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, title, statement_text, visibility, status, owner_username, created_at, updated_at
      |from problems
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into problems (id, slug, title, statement_text, visibility, status, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, visibility, status, owner_username, created_at, updated_at
      |""".stripMargin

  val updateSql: String =
    """
      |update problems
      |set title = ?, statement_text = ?, visibility = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from problems
      |where id = ?
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }

  def list(connection: Connection, page: Int, pageSize: Int): IO[PageResponse[ProblemSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql)
        try
          statement.setInt(1, pageSize)
          statement.setInt(2, (page - 1) * pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemListItem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(items = items, page = page, pageSize = pageSize, totalItems = totalItems)

  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[Problem]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  def insert(connection: Connection, ownerUsername: Username, request: CreateProblemRequest): IO[Problem] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, ProblemId.random().value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.statement.value)
        statement.setString(5, ResourceVisibility.toDatabase(request.visibility))
        statement.setString(6, ResourceStatus.toDatabase(ResourceStatus.Draft))
        statement.setString(7, ownerUsername.value)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setTimestamp(9, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemDetail(resultSet)
          else throw new IllegalStateException("Insert succeeded but returned no problem")
        finally resultSet.close()
      finally statement.close()
    }

  def update(connection: Connection, problemId: ProblemId, request: UpdateProblemRequest): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.statement.value)
        statement.setString(3, ResourceVisibility.toDatabase(request.visibility))
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setObject(5, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def delete(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def readProblemListItem(resultSet: ResultSet): ProblemSummary =
    ProblemSummary(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSlug.unsafe(resultSet.getString("slug")),
      title = ProblemTitle.unsafe(resultSet.getString("title")),
      visibility = ResourceVisibility.fromDatabaseUnsafe(resultSet.getString("visibility")),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readProblemDetail(resultSet: ResultSet): Problem =
    Problem(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSlug.unsafe(resultSet.getString("slug")),
      title = ProblemTitle.unsafe(resultSet.getString("title")),
      statement = ProblemStatementText.unsafe(resultSet.getString("statement_text")),
      visibility = ResourceVisibility.fromDatabaseUnsafe(resultSet.getString("visibility")),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )
