package domains.problem.table

import domains.shared.sql.UserIdentitySql

object ProblemTableSql:

  val listSql: String =
    s"""
      |select p.id, p.slug, p.title, p.data_name, p.time_limit_ms, p.space_limit_mb, p.base_access, p.others_submission_access, ${UserIdentitySql.selectColumns("p.creator_username", "creator", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.joinAuthUsers("p.creator_username", "au")}
      |where
      |  ? = true
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user'
      |            and rag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          join user_groups ug on ug.slug = rag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user_group'
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
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user'
      |            and rag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          join user_groups ug on ug.slug = rag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
      |  )
      |""".stripMargin

  val findBySlugSql: String =
    s"""
      |select p.id, p.slug, p.title, p.statement_text, p.data_name, p.time_limit_ms, p.space_limit_mb, p.base_access, p.others_submission_access, ${UserIdentitySql.selectColumns("p.creator_username", "creator", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.joinAuthUsers("p.creator_username", "au")}
      |where p.slug = ?
      |""".stripMargin

  val insertSql: String =
    s"""
      |insert into problems (id, slug, title, statement_text, data_name, data_bytes, time_limit_ms, space_limit_mb, visibility, base_access, others_submission_access, creator_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, data_name, time_limit_ms, space_limit_mb, base_access, others_submission_access, ${UserIdentitySql.returningColumns("creator_username", "creator")}, created_at, updated_at
      |""".stripMargin

  val updateSql: String =
    """
      |update problems
      |set title = ?, statement_text = ?, time_limit_ms = ?, space_limit_mb = ?, visibility = ?, base_access = ?, others_submission_access = ?, updated_at = ?
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
