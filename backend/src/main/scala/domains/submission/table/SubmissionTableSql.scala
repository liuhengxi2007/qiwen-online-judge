package domains.submission.table

import domains.shared.sql.UserIdentitySql

object SubmissionTableSql:

  val insertSql: String =
    s"""
      |insert into submissions (id, public_id, problem_id, submitter_username, language, status, verdict, judge_message, time_used_ms, memory_used_kb, source_code, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning public_id, status, verdict, judge_message, time_used_ms, memory_used_kb, ${UserIdentitySql.returningColumns("submitter_username", "submitter")}, submitted_at, started_at, finished_at
      |""".stripMargin

  val listSql: String =
    s"""
      |select s.public_id,
      |       s.problem_id,
      |       p.slug as problem_slug,
      |       p.title as problem_title,
      |       ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")},
      |       s.language,
      |       s.status,
      |       s.verdict,
      |       s.time_used_ms,
      |       s.memory_used_kb,
      |       octet_length(s.source_code) as code_length,
      |       s.submitted_at,
      |       s.started_at,
      |       s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where
      |  (? = false or s.submitter_username = ?)
      |  and (
      |    ? = true
      |    or s.submitter_username = ?
      |    or (
      |      p.others_submission_access in ('summary', 'detail')
      |      and (
      |        p.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          where rag.resource_kind = 'problem'
      |            and rag.resource_id = p.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user'
      |            and rag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          join user_groups ug on ug.slug = rag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rag.resource_kind = 'problem'
      |            and rag.resource_id = p.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |        or exists (
      |          select 1
      |          from problem_set_problems psp
      |          join problem_sets ps on ps.id = psp.problem_set_id
      |          where psp.problem_id = p.id
      |            and (
      |              ? = true
      |              or ps.base_access = 'public'
      |              or exists (
      |                select 1
      |                from resource_access_grants rag
      |                where rag.resource_kind = 'problem_set'
      |                  and rag.resource_id = ps.id
      |                  and rag.grant_role = 'viewer'
      |                  and rag.subject_kind = 'user'
      |                  and rag.subject_key = ?
      |              )
      |              or exists (
      |                select 1
      |                from resource_access_grants rag
      |                join user_groups ug on ug.slug = rag.subject_key
      |                join user_group_memberships ugm on ugm.user_group_id = ug.id
      |                where rag.resource_kind = 'problem_set'
      |                  and rag.resource_id = ps.id
      |                  and rag.grant_role = 'viewer'
      |                  and rag.subject_kind = 'user_group'
      |                  and ugm.username = ?
      |              )
      |            )
      |        )
      |      )
      |    )
      |  )
      |order by s.submitted_at desc, s.public_id desc
      |""".stripMargin

  val findByIdSql: String =
    s"""
      |select s.public_id, s.problem_id, p.slug as problem_slug, p.title as problem_title, ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")}, s.language, s.status, s.verdict, s.judge_message, s.time_used_ms, s.memory_used_kb, octet_length(s.source_code) as code_length, s.source_code, s.submitted_at, s.started_at, s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where s.public_id = ?
      |""".stripMargin

  val claimNextCpp17Sql: String =
    """
      |with next_submission as (
      |  select s.id
      |  from submissions s
      |  where s.status = 'queued'
      |    and s.language = 'cpp17'
      |  order by s.submitted_at asc, s.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update submissions s
      |set status = ?,
      |    started_at = ?,
      |    finished_at = ?,
      |    verdict = ?,
      |    judge_message = ?,
      |    time_used_ms = null,
      |    memory_used_kb = null
      |from next_submission ns, problems p
      |where s.id = ns.id
      |  and p.id = s.problem_id
      |returning s.public_id, s.problem_id, p.slug as problem_slug, s.language, s.source_code, p.time_limit_ms, p.space_limit_mb
      |""".stripMargin

  val updateJudgeStateSql: String =
    """
      |update submissions
      |set status = ?, verdict = ?, judge_message = ?, time_used_ms = ?, memory_used_kb = ?, started_at = ?, finished_at = ?
      |where public_id = ?
      |""".stripMargin

  val deleteByIdSql: String =
    """
      |delete from submissions
      |where public_id = ?
      |""".stripMargin
