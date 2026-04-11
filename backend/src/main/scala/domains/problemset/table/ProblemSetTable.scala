package domains.problemset.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.problemset.model.{CreateProblemSetRequest, ProblemSet, ProblemSetDescription, ProblemSetId, ProblemSetProblemSummary, ProblemSetSlug, ProblemSetSummary, ProblemSetTitle, UpdateProblemSetRequest}
import domains.shared.access.{AccessSubject, BaseAccess, GrantRole, ResourceAccessGrant, ResourceAccessGrantTable, ResourceAccessPolicy, ResourceId, ResourceKind}
import domains.shared.model.PageResponse

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

object ProblemSetTable:

  val initTableSql: String =
    """
      |create table if not exists problem_sets (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  description text not null,
      |  base_access varchar(32) not null default 'owner_only' check (base_access in ('owner_only', 'public')),
      |  creator_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val migrateCreatorUsernameColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'owner_username'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'creator_username'
      |  ) then
      |    alter table problem_sets rename column owner_username to creator_username;
      |  end if;
      |end $$;
      |""".stripMargin

  val initProblemRelationTableSql: String =
    """
      |create table if not exists problem_set_problems (
      |  problem_set_id uuid not null references problem_sets(id) on delete cascade,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  position integer not null,
      |  primary key (problem_set_id, problem_id),
      |  unique (problem_set_id, position)
      |);
      |""".stripMargin

  val addBaseAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'base_access'
      |  ) then
      |    alter table problem_sets add column base_access varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'visibility'
      |  ) then
      |    update problem_sets
      |    set base_access = case visibility
      |      when 'public' then 'public'
      |      else 'owner_only'
      |    end
      |    where base_access is null or btrim(base_access) = '';
      |  else
      |    update problem_sets
      |    set base_access = 'owner_only'
      |    where base_access is null or btrim(base_access) = '';
      |  end if;
      |end $$;
      |""".stripMargin

  val setBaseAccessNotNullSql: String =
    """
      |alter table problem_sets
      |alter column base_access set not null
      |""".stripMargin

  val setBaseAccessDefaultSql: String =
    """
      |alter table problem_sets
      |alter column base_access set default 'owner_only'
      |""".stripMargin

  val dropStatusColumnSql: String =
    """
      |alter table problem_sets
      |drop column if exists status
      |""".stripMargin

  val listSql: String =
    """
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ps.creator_username, ps.created_at, ps.updated_at
      |from problem_sets ps
      |where
      |  ? = true
      |  or ps.creator_username = ?
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem_set'
      |      and rag.resource_id = ps.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem_set'
      |      and rag.resource_id = ps.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |order by ps.updated_at desc, ps.slug asc
      |limit ? offset ?
      |""".stripMargin

  val countSql: String =
    """
      |select count(*) as total_items
      |from problem_sets ps
      |where
      |  ? = true
      |  or ps.creator_username = ?
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem_set'
      |      and rag.resource_id = ps.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem_set'
      |      and rag.resource_id = ps.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, title, description, base_access, creator_username, created_at, updated_at
      |from problem_sets
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into problem_sets (id, slug, title, description, visibility, base_access, creator_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, description, base_access, creator_username, created_at, updated_at
      |""".stripMargin

  val listProblemsForSetSql: String =
    """
      |select p.id, p.slug, p.title, psp.position
      |from problem_set_problems psp
      |join problems p on p.id = psp.problem_id
      |where psp.problem_set_id = ?
      |order by psp.position asc, p.slug asc
      |""".stripMargin

  val relationExistsSql: String =
    """
      |select 1
      |from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  val nextPositionSql: String =
    """
      |select coalesce(max(position), 0) as current_max
      |from problem_set_problems
      |where problem_set_id = ?
      |""".stripMargin

  val insertRelationSql: String =
    """
      |insert into problem_set_problems (problem_set_id, problem_id, position)
      |values (?, ?, ?)
      |""".stripMargin

  val updateSql: String =
    """
      |update problem_sets
      |set title = ?, description = ?, visibility = ?, base_access = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from problem_sets
      |where id = ?
      |""".stripMargin

  val findRelationPositionSql: String =
    """
      |select position
      |from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  val deleteRelationSql: String =
    """
      |delete from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  val compactPositionsSql: String =
    """
      |update problem_set_problems
      |set position = position - 1
      |where problem_set_id = ? and position > ?
      |""".stripMargin

  enum AddProblemTableResult:
    case AlreadyLinked
    case Linked

  enum RemoveProblemTableResult:
    case NotLinked
    case Removed

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(migrateCreatorUsernameColumnSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(dropStatusColumnSql)
        statement.execute(initProblemRelationTableSql)
      finally statement.close()
    }

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSetSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          bindVisibilityQuery(statement, actor, None, None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql)
        try
          bindVisibilityQuery(statement, actor, Some(pageSize), Some((page - 1) * pageSize))
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemSetSummaryBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.foldLeft(IO.pure(List.empty[ProblemSetSummary])) { (accIO, item) =>
        for
          acc <- accIO
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(item.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(item.id), GrantRole.Manager)
        yield acc :+ item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, viewerGrants, managerGrants))
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  def findBySlug(connection: Connection, slug: ProblemSetSlug): IO[Option[ProblemSet]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemSetDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problemSet) =>
        for
          problems <- listProblemsForSet(connection, problemSet.id)
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Manager)
        yield Some(problemSet.copy(problems = problems, accessPolicy = policyFrom(problemSet.accessPolicy.baseAccess, viewerGrants, managerGrants)))
      case None =>
        IO.pure(None)
    }

  def insert(connection: Connection, ownerUsername: Username, request: CreateProblemSetRequest): IO[ProblemSet] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, ProblemSetId.random().value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.description.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(7, ownerUsername.value)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setTimestamp(9, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemSetDetailBase(resultSet).copy(problems = Nil)
          else missingInsertResult("problem set")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problemSet =>
      val sanitizedPolicy = sanitizePolicy(ownerUsername, request.accessPolicy)
      ResourceAccessGrantTable
        .replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        .flatMap(_ =>
          ResourceAccessGrantTable.replaceForResource(
            connection,
            ResourceKind.ProblemSet,
            toResourceId(problemSet.id),
            GrantRole.Manager,
            sanitizedPolicy.managerGrants
          )
        )
        .as(problemSet.copy(accessPolicy = sanitizedPolicy))
    }

  def addProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[AddProblemTableResult] =
    for
      alreadyLinked <- IO.blocking {
        val statement = connection.prepareStatement(relationExistsSql)
        try
          statement.setObject(1, problemSetId.value)
          statement.setObject(2, problemId.value)
          val resultSet = statement.executeQuery()
          try resultSet.next()
          finally resultSet.close()
        finally statement.close()
      }
      result <- if alreadyLinked then
        IO.pure(AddProblemTableResult.AlreadyLinked)
      else
        IO.blocking {
          val nextPositionStatement = connection.prepareStatement(nextPositionSql)
          try
            nextPositionStatement.setObject(1, problemSetId.value)
            val nextPositionResultSet = nextPositionStatement.executeQuery()
            val nextPosition =
              try
                if nextPositionResultSet.next() then nextPositionResultSet.getInt("current_max") + 1
                else 1
              finally nextPositionResultSet.close()

            val insertStatement = connection.prepareStatement(insertRelationSql)
            try
              insertStatement.setObject(1, problemSetId.value)
              insertStatement.setObject(2, problemId.value)
              insertStatement.setInt(3, nextPosition)
              insertStatement.executeUpdate()
              AddProblemTableResult.Linked
            finally insertStatement.close()
          finally nextPositionStatement.close()
        }
    yield result

  def update(connection: Connection, problemSetId: ProblemSetId, request: UpdateProblemSetRequest): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.description.value)
        statement.setString(3, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(4, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setTimestamp(5, Timestamp.from(now))
        statement.setObject(6, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    } *>
      ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSetId), GrantRole.Viewer, request.accessPolicy.viewerGrants) *>
      ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSetId), GrantRole.Manager, request.accessPolicy.managerGrants)

  def delete(connection: Connection, problemSetId: ProblemSetId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setObject(1, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def removeProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[RemoveProblemTableResult] =
    IO.blocking {
      val positionStatement = connection.prepareStatement(findRelationPositionSql)
      try
        positionStatement.setObject(1, problemSetId.value)
        positionStatement.setObject(2, problemId.value)
        val resultSet = positionStatement.executeQuery()
        val maybePosition =
          try if resultSet.next() then Some(resultSet.getInt("position")) else None
          finally resultSet.close()

        maybePosition match
          case None =>
            RemoveProblemTableResult.NotLinked
          case Some(position) =>
            val deleteStatement = connection.prepareStatement(deleteRelationSql)
            try
              deleteStatement.setObject(1, problemSetId.value)
              deleteStatement.setObject(2, problemId.value)
              deleteStatement.executeUpdate()
            finally deleteStatement.close()

            val compactStatement = connection.prepareStatement(compactPositionsSql)
            try
              compactStatement.setObject(1, problemSetId.value)
              compactStatement.setInt(2, position)
              compactStatement.executeUpdate()
            finally compactStatement.close()

            RemoveProblemTableResult.Removed
      finally positionStatement.close()
    }

  private def readProblemSetSummaryBase(resultSet: ResultSet): ProblemSetSummary =
    ProblemSetSummary(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), BaseAccess.fromDatabase), Nil, Nil),
      creatorUsername = Username.canonical(resultSet.getString("creator_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readProblemSetDetailBase(resultSet: ResultSet): ProblemSet =
    ProblemSet(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = parseColumn("problem_sets.slug", resultSet.getString("slug"), ProblemSetSlug.parse),
      title = parseColumn("problem_sets.title", resultSet.getString("title"), ProblemSetTitle.parse),
      description = parseColumn("problem_sets.description", resultSet.getString("description"), ProblemSetDescription.parse),
      problems = Nil,
      accessPolicy =
        ResourceAccessPolicy(parseOptionalColumn("problem_sets.base_access", resultSet.getString("base_access"), BaseAccess.fromDatabase), Nil, Nil),
      creatorUsername = Username.canonical(resultSet.getString("creator_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def listProblemsForSet(connection: Connection, problemSetId: ProblemSetId): IO[List[ProblemSetProblemSummary]] =
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

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  private def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  private def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  private def bindVisibilityQuery(
    statement: java.sql.PreparedStatement,
    actor: domains.auth.model.AuthUser,
    pageSize: Option[Int],
    offset: Option[Int]
  ): Unit =
    statement.setBoolean(1, actor.siteManager || actor.problemManager)
    statement.setString(2, actor.username.value)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)
    pageSize.foreach(statement.setInt(5, _))
    offset.foreach(statement.setInt(6, _))

  private def policyFrom(
    baseAccess: BaseAccess,
    viewerGrants: List[ResourceAccessGrant],
    managerGrants: List[ResourceAccessGrant]
  ): ResourceAccessPolicy =
    ResourceAccessPolicy(baseAccess = baseAccess, viewerGrants = viewerGrants.map(_.subject), managerGrants = managerGrants.map(_.subject))

  private def sanitizePolicy(ownerUsername: Username, policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
        .filter {
          case AccessSubject.User(username) => username.value != ownerUsername.value
          case AccessSubject.UserGroup(_) => true
        },
      managerGrants = policy.managerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
        .filter {
          case AccessSubject.User(username) => username.value != ownerUsername.value
          case AccessSubject.UserGroup(_) => true
        }
    )

  private def toResourceId(problemSetId: ProblemSetId): ResourceId =
    ResourceId(problemSetId.value)

  private def toLegacyVisibility(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Public => "public"
      case BaseAccess.OwnerOnly => "private"
