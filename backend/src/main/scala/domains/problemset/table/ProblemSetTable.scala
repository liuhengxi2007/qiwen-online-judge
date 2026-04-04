package domains.problemset.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.problemset.model.{CreateProblemSetRequest, ProblemSet, ProblemSetDescription, ProblemSetId, ProblemSetProblem, ProblemSetSlug, ProblemSetSummaryView, ProblemSetTitle, UpdateProblemSetRequest}
import domains.shared.access.{AccessSubject, BaseAccess, ResourceAccessPolicy, ResourceId, ResourceKind, ResourceViewerGrantTable}
import domains.shared.model.{PageResponse, ResourceStatus}

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
      |  status varchar(32) not null check (status in ('draft', 'published', 'archived')),
      |  owner_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
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

  val listSql: String =
    """
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ps.status, ps.owner_username, ps.created_at, ps.updated_at
      |from problem_sets ps
      |where
      |  ? = true
      |  or ps.owner_username = ?
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    where rvg.resource_kind = 'problem_set'
      |      and rvg.resource_id = ps.id
      |      and rvg.subject_kind = 'user'
      |      and rvg.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    join user_groups ug on ug.slug = rvg.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rvg.resource_kind = 'problem_set'
      |      and rvg.resource_id = ps.id
      |      and rvg.subject_kind = 'user_group'
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
      |  or ps.owner_username = ?
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    where rvg.resource_kind = 'problem_set'
      |      and rvg.resource_id = ps.id
      |      and rvg.subject_kind = 'user'
      |      and rvg.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    join user_groups ug on ug.slug = rvg.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rvg.resource_kind = 'problem_set'
      |      and rvg.resource_id = ps.id
      |      and rvg.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, title, description, base_access, status, owner_username, created_at, updated_at
      |from problem_sets
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into problem_sets (id, slug, title, description, base_access, status, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, description, base_access, status, owner_username, created_at, updated_at
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
      |set title = ?, description = ?, base_access = ?, updated_at = ?
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
        statement.execute(addBaseAccessColumnSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(initProblemRelationTableSql)
      finally statement.close()
    }

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSetSummaryView]] =
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
      itemsWithPolicies <- items.foldLeft(IO.pure(List.empty[ProblemSetSummaryView])) { (accIO, item) =>
        for
          acc <- accIO
          grants <- ResourceViewerGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(item.id))
        yield acc :+ item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, grants))
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
          grants <- ResourceViewerGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id))
        yield Some(problemSet.copy(problems = problems, accessPolicy = policyFrom(problemSet.accessPolicy.baseAccess, grants)))
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
        statement.setString(5, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(6, ResourceStatus.toDatabase(ResourceStatus.Draft))
        statement.setString(7, ownerUsername.value)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setTimestamp(9, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemSetDetailBase(resultSet).copy(problems = Nil)
          else throw new IllegalStateException("Insert succeeded but returned no problem set")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problemSet =>
      val sanitizedPolicy = sanitizePolicy(ownerUsername, request.accessPolicy)
      ResourceViewerGrantTable
        .replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), sanitizedPolicy.viewerGrants)
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
        statement.setString(3, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setObject(5, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    } *> ResourceViewerGrantTable.replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSetId), request.accessPolicy.viewerGrants)

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

  private def readProblemSetSummaryBase(resultSet: ResultSet): ProblemSetSummaryView =
    ProblemSetSummaryView(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSetSlug.unsafe(resultSet.getString("slug")),
      title = ProblemSetTitle.unsafe(resultSet.getString("title")),
      description = ProblemSetDescription.unsafe(resultSet.getString("description")),
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readProblemSetDetailBase(resultSet: ResultSet): ProblemSet =
    ProblemSet(
      id = ProblemSetId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSetSlug.unsafe(resultSet.getString("slug")),
      title = ProblemSetTitle.unsafe(resultSet.getString("title")),
      description = ProblemSetDescription.unsafe(resultSet.getString("description")),
      problems = Nil,
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def listProblemsForSet(connection: Connection, problemSetId: ProblemSetId): IO[List[ProblemSetProblem]] =
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
              ProblemSetProblem(
                id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
                slug = ProblemSlug.unsafe(resultSet.getString("slug")),
                title = ProblemTitle.unsafe(resultSet.getString("title")),
                position = resultSet.getInt("position")
              )
            }
            .toList
        finally resultSet.close()
      finally statement.close()
    }

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

  private def policyFrom(baseAccess: BaseAccess, grants: List[domains.shared.access.ResourceViewerGrant]): ResourceAccessPolicy =
    ResourceAccessPolicy(baseAccess = baseAccess, viewerGrants = grants.map(_.subject))

  private def sanitizePolicy(ownerUsername: Username, policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants
        .distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
        .filter {
          case AccessSubject.User(username) => username.value != ownerUsername.value
          case AccessSubject.UserGroup(_) => true
        }
    )

  private def toResourceId(problemSetId: ProblemSetId): ResourceId =
    ResourceId(problemSetId.value)
