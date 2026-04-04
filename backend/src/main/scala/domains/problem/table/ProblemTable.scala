package domains.problem.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{CreateProblemRequest, Problem, ProblemId, ProblemSlug, ProblemStatementText, ProblemSummary, ProblemTitle, UpdateProblemRequest}
import domains.shared.access.{AccessSubject, BaseAccess, ResourceAccessPolicy, ResourceId, ResourceKind, ResourceViewerGrantTable}
import domains.shared.model.PageResponse
import domains.shared.model.ResourceStatus

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
      |  base_access varchar(32) not null default 'owner_only' check (base_access in ('owner_only', 'public')),
      |  status varchar(32) not null check (status in ('draft', 'published', 'archived')),
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

  val listSql: String =
    """
      |select p.id, p.slug, p.title, p.base_access, p.status, p.owner_username, p.created_at, p.updated_at
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
      |""".stripMargin

  val findBySlugSql: String =
    """
      |select id, slug, title, statement_text, base_access, status, owner_username, created_at, updated_at
      |from problems
      |where slug = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into problems (id, slug, title, statement_text, visibility, base_access, status, owner_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, base_access, status, owner_username, created_at, updated_at
      |""".stripMargin

  val updateSql: String =
    """
      |update problems
      |set title = ?, statement_text = ?, visibility = ?, base_access = ?, updated_at = ?
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
        statement.execute(addBaseAccessColumnSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
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

  def insert(connection: Connection, ownerUsername: Username, request: CreateProblemRequest): IO[Problem] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, ProblemId.random().value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.statement.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(7, ResourceStatus.toDatabase(ResourceStatus.Draft))
        statement.setString(8, ownerUsername.value)
        statement.setTimestamp(9, Timestamp.from(now))
        statement.setTimestamp(10, Timestamp.from(now))
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
        statement.setString(3, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(4, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setTimestamp(5, Timestamp.from(now))
        statement.setObject(6, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ResourceViewerGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), request.accessPolicy.viewerGrants)
    yield ()

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
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
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
      accessPolicy = ResourceAccessPolicy(BaseAccess.fromDatabaseUnsafe(resultSet.getString("base_access")), Nil),
      status = ResourceStatus.fromDatabaseUnsafe(resultSet.getString("status")),
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

  private def toResourceId(problemId: ProblemId): ResourceId =
    ResourceId(problemId.value)

  private def toLegacyVisibility(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Public => "public"
      case BaseAccess.OwnerOnly => "private"
