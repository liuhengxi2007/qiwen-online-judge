package domains.problem.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{CreateProblemRequest, Problem, ProblemData, ProblemDataFilename, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemSummary, ProblemTimeLimitMs, ProblemTitle, UpdateProblemRequest}
import domains.shared.access.{AccessSubject, BaseAccess, ResourceAccessPolicy, ResourceId, ResourceKind, ResourceViewerGrantTable}
import domains.shared.model.PageResponse

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
      |  data_name varchar(255),
      |  data_bytes bytea,
      |  time_limit_ms integer not null default 1000,
      |  space_limit_mb integer not null default 256,
      |  base_access varchar(32) not null default 'owner_only' check (base_access in ('owner_only', 'public')),
      |  owner_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
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
      |      and table_name = 'problems'
      |      and column_name = 'base_access'
      |  ) then
      |    alter table problems add column base_access varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'visibility'
      |  ) then
      |    update problems
      |    set base_access = case visibility
      |      when 'public' then 'public'
      |      else 'owner_only'
      |    end
      |    where base_access is null or btrim(base_access) = '';
      |  else
      |    update problems
      |    set base_access = 'owner_only'
      |    where base_access is null or btrim(base_access) = '';
      |  end if;
      |end $$;
      |""".stripMargin

  val addVisibilityColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'visibility'
      |  ) then
      |    alter table problems add column visibility varchar(32);
      |  end if;
      |
      |  update problems
      |  set visibility = case base_access
      |    when 'public' then 'public'
      |    else 'private'
      |  end
      |  where visibility is null or btrim(visibility) = '';
      |end $$;
      |""".stripMargin

  val addDataAndLimitColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'data_name'
      |  ) then
      |    alter table problems add column data_name varchar(255);
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'data_bytes'
      |  ) then
      |    alter table problems add column data_bytes bytea;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'time_limit_ms'
      |  ) then
      |    alter table problems add column time_limit_ms integer;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'space_limit_mb'
      |  ) then
      |    alter table problems add column space_limit_mb integer;
      |  end if;
      |
      |  update problems
      |  set time_limit_ms = 1000
      |  where time_limit_ms is null;
      |
      |  update problems
      |  set space_limit_mb = 256
      |  where space_limit_mb is null;
      |end $$;
      |""".stripMargin

  val setTimeLimitNotNullSql: String =
    """
      |alter table problems
      |alter column time_limit_ms set not null
      |""".stripMargin

  val setTimeLimitDefaultSql: String =
    """
      |alter table problems
      |alter column time_limit_ms set default 1000
      |""".stripMargin

  val setSpaceLimitNotNullSql: String =
    """
      |alter table problems
      |alter column space_limit_mb set not null
      |""".stripMargin

  val setSpaceLimitDefaultSql: String =
    """
      |alter table problems
      |alter column space_limit_mb set default 256
      |""".stripMargin

  val setBaseAccessNotNullSql: String =
    """
      |alter table problems
      |alter column base_access set not null
      |""".stripMargin

  val setBaseAccessDefaultSql: String =
    """
      |alter table problems
      |alter column base_access set default 'owner_only'
      |""".stripMargin

  val dropStatusColumnSql: String =
    """
      |alter table problems
      |drop column if exists status
      |""".stripMargin

  val listSql: String =
    """
      |select p.id, p.slug, p.title, p.data_name, p.time_limit_ms, p.space_limit_mb, p.base_access, p.owner_username, p.created_at, p.updated_at
      |from problems p
      |where
      |  ? = true
      |  or p.owner_username = ?
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    where rvg.resource_kind = 'problem'
      |      and rvg.resource_id = p.id
      |      and rvg.subject_kind = 'user'
      |      and rvg.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    join user_groups ug on ug.slug = rvg.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rvg.resource_kind = 'problem'
      |      and rvg.resource_id = p.id
      |      and rvg.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.owner_username = ?
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_viewer_grants rvg
      |          where rvg.resource_kind = 'problem_set'
      |            and rvg.resource_id = ps.id
      |            and rvg.subject_kind = 'user'
      |            and rvg.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_viewer_grants rvg
      |          join user_groups ug on ug.slug = rvg.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rvg.resource_kind = 'problem_set'
      |            and rvg.resource_id = ps.id
      |            and rvg.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
      |  )
      |order by p.updated_at desc, p.slug asc
      |limit ? offset ?
      |""".stripMargin

  val countSql: String =
    """
      |select count(*) as total_items
      |from problems p
      |where
      |  ? = true
      |  or p.owner_username = ?
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    where rvg.resource_kind = 'problem'
      |      and rvg.resource_id = p.id
      |      and rvg.subject_kind = 'user'
      |      and rvg.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_viewer_grants rvg
      |    join user_groups ug on ug.slug = rvg.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rvg.resource_kind = 'problem'
      |      and rvg.resource_id = p.id
      |      and rvg.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.owner_username = ?
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_viewer_grants rvg
      |          where rvg.resource_kind = 'problem_set'
      |            and rvg.resource_id = ps.id
      |            and rvg.subject_kind = 'user'
      |            and rvg.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_viewer_grants rvg
      |          join user_groups ug on ug.slug = rvg.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rvg.resource_kind = 'problem_set'
      |            and rvg.resource_id = ps.id
      |            and rvg.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
      |  )
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, title, statement_text, data_name, time_limit_ms, space_limit_mb, base_access, owner_username, created_at, updated_at
      |from problems
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into problems (id, slug, title, statement_text, data_name, data_bytes, time_limit_ms, space_limit_mb, visibility, base_access, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, data_name, time_limit_ms, space_limit_mb, base_access, owner_username, created_at, updated_at
      |""".stripMargin

  val updateSql: String =
    """
      |update problems
      |set title = ?, statement_text = ?, time_limit_ms = ?, space_limit_mb = ?, visibility = ?, base_access = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  val updateDataSql: String =
    """
      |update problems
      |set data_name = ?, updated_at = ?
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
      try
        statement.execute(initTableSql)
        statement.execute(addVisibilityColumnSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(addDataAndLimitColumnsSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(setTimeLimitDefaultSql)
        statement.execute(setTimeLimitNotNullSql)
        statement.execute(setSpaceLimitDefaultSql)
        statement.execute(setSpaceLimitNotNullSql)
        statement.execute(dropStatusColumnSql)
      finally statement.close()
    }

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          bindVisibilityQuery(statement, actor, pageSize = None, offset = None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql)
        try
          bindVisibilityQuery(statement, actor, pageSize = Some(pageSize), offset = Some((page - 1) * pageSize))
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemListItemBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.foldLeft(IO.pure(List.empty[ProblemSummary])) { (accIO, item) =>
        for
          acc <- accIO
          grants <- ResourceViewerGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id))
        yield acc :+ item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, grants))
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[Problem]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problem) =>
        ResourceViewerGrantTable
          .listForResource(connection, ResourceKind.Problem, toResourceId(problem.id))
          .map(grants => Some(problem.copy(accessPolicy = policyFrom(problem.accessPolicy.baseAccess, grants))))
      case None =>
        IO.pure(None)
    }

  def hasVisibleContainingProblemSet(
    connection: Connection,
    actor: domains.auth.model.AuthUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasVisibleContainingProblemSetSql)
      try
        bindContainingProblemSetVisibilityQuery(statement, actor, problemId)
        val resultSet = statement.executeQuery()
        try resultSet.next()
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
        statement.setNull(5, java.sql.Types.VARCHAR)
        statement.setNull(6, java.sql.Types.BINARY)
        statement.setInt(7, request.timeLimitMs.value)
        statement.setInt(8, request.spaceLimitMb.value)
        statement.setString(9, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(10, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(11, ownerUsername.value)
        statement.setTimestamp(12, Timestamp.from(now))
        statement.setTimestamp(13, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemDetailBase(resultSet)
          else throw new IllegalStateException("Insert succeeded but returned no problem")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problem =>
      val sanitizedPolicy = sanitizePolicy(ownerUsername, request.accessPolicy)
      ResourceViewerGrantTable
        .replaceForResource(connection, ResourceKind.Problem, toResourceId(problem.id), sanitizedPolicy.viewerGrants)
        .as(problem.copy(accessPolicy = sanitizedPolicy))
    }

  def update(connection: Connection, problemId: ProblemId, request: UpdateProblemRequest): IO[Unit] =
    for
      _ <- IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.statement.value)
        statement.setInt(3, request.timeLimitMs.value)
        statement.setInt(4, request.spaceLimitMb.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setTimestamp(7, Timestamp.from(now))
        statement.setObject(8, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ResourceViewerGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), request.accessPolicy.viewerGrants)
    yield ()

  def updateData(connection: Connection, problemId: ProblemId, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, Some(filename))

  def updateData(connection: Connection, problemId: ProblemId, filename: Option[ProblemDataFilename]): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateDataSql)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setTimestamp(2, Timestamp.from(now))
        statement.setObject(3, problemId.value)
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

  private def readProblemListItemBase(resultSet: ResultSet): ProblemSummary =
    ProblemSummary(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSlug.unsafe(resultSet.getString("slug")),
      title = ProblemTitle.unsafe(resultSet.getString("title")),
      data = ProblemData.unsafe(Option(resultSet.getString("data_name"))),
      timeLimitMs = ProblemTimeLimitMs.unsafe(resultSet.getInt("time_limit_ms")),
      spaceLimitMb = ProblemSpaceLimitMb.unsafe(resultSet.getInt("space_limit_mb")),
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  private def readProblemDetailBase(resultSet: ResultSet): Problem =
    Problem(
      id = ProblemId(resultSet.getObject("id", classOf[java.util.UUID])),
      slug = ProblemSlug.unsafe(resultSet.getString("slug")),
      title = ProblemTitle.unsafe(resultSet.getString("title")),
      statement = ProblemStatementText.unsafe(resultSet.getString("statement_text")),
      data = ProblemData.unsafe(Option(resultSet.getString("data_name"))),
      timeLimitMs = ProblemTimeLimitMs.unsafe(resultSet.getInt("time_limit_ms")),
      spaceLimitMb = ProblemSpaceLimitMb.unsafe(resultSet.getInt("space_limit_mb")),
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      ownerUsername = Username.canonical(resultSet.getString("owner_username")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

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
    statement.setBoolean(5, actor.siteManager || actor.problemManager)
    statement.setString(6, actor.username.value)
    statement.setString(7, actor.username.value)
    statement.setString(8, actor.username.value)
    pageSize.foreach(statement.setInt(9, _))
    offset.foreach(statement.setInt(10, _))

  private val hasVisibleContainingProblemSetSql: String =
    """
      |select 1
      |from problem_set_problems psp
      |join problem_sets ps on ps.id = psp.problem_set_id
      |where psp.problem_id = ?
      |  and (
      |    ? = true
      |    or ps.owner_username = ?
      |    or ps.base_access = 'public'
      |    or exists (
      |      select 1
      |      from resource_viewer_grants rvg
      |      where rvg.resource_kind = 'problem_set'
      |        and rvg.resource_id = ps.id
      |        and rvg.subject_kind = 'user'
      |        and rvg.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from resource_viewer_grants rvg
      |      join user_groups ug on ug.slug = rvg.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where rvg.resource_kind = 'problem_set'
      |        and rvg.resource_id = ps.id
      |        and rvg.subject_kind = 'user_group'
      |        and ugm.username = ?
      |    )
      |  )
      |limit 1
      |""".stripMargin

  private def bindContainingProblemSetVisibilityQuery(
    statement: java.sql.PreparedStatement,
    actor: domains.auth.model.AuthUser,
    problemId: ProblemId
  ): Unit =
    statement.setObject(1, problemId.value)
    statement.setBoolean(2, actor.siteManager || actor.problemManager)
    statement.setString(3, actor.username.value)
    statement.setString(4, actor.username.value)
    statement.setString(5, actor.username.value)

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

  private def toResourceId(problemId: ProblemId): ResourceId =
    ResourceId(problemId.value)

  private def toLegacyVisibility(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Public => "public"
      case BaseAccess.OwnerOnly => "private"
