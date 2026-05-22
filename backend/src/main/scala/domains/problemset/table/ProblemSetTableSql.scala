package domains.problemset.table



import shared.sql.UserIdentitySql

object ProblemSetTableSql:

  val listSql: String =
    s"""
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ${UserIdentitySql.selectColumns("ps.creator_username", "creator", "au")}, ps.created_at, ps.updated_at
      |from problem_sets ps
      |${UserIdentitySql.joinAuthUsers("ps.creator_username", "au")}
      |where
      |  ? = true
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
    s"""
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ${UserIdentitySql.selectColumns("ps.creator_username", "creator", "au")}, ps.created_at, ps.updated_at
      |from problem_sets ps
      |${UserIdentitySql.joinAuthUsers("ps.creator_username", "au")}
      |where ps.slug = ?
      |""".stripMargin

  val insertSql: String =
    s"""
      |insert into problem_sets (id, slug, title, description, visibility, base_access, creator_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, description, base_access, ${UserIdentitySql.returningColumns("creator_username", "creator")}, created_at, updated_at
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
