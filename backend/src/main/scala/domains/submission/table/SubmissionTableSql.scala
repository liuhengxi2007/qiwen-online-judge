package domains.submission.table

import domains.submission.model.{SubmissionSort, SubmissionSortDirection}
import domains.shared.sql.UserIdentitySql

object SubmissionTableSql:

  private val detailVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.others_submission_access = 'detail'
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user'
      |          and rag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        join user_groups ug on ug.slug = rag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user_group'
      |          and ugm.username = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_set_problems psp
      |        join problem_sets ps on ps.id = psp.problem_set_id
      |        where psp.problem_id = p.id
      |          and (
      |            ? = true
      |            or ps.base_access = 'public'
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user'
      |                and rag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              join user_groups ug on ug.slug = rag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val summaryVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.others_submission_access in ('summary', 'detail')
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user'
      |          and rag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        join user_groups ug on ug.slug = rag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user_group'
      |          and ugm.username = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_set_problems psp
      |        join problem_sets ps on ps.id = psp.problem_set_id
      |        where psp.problem_id = p.id
      |          and (
      |            ? = true
      |            or ps.base_access = 'public'
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user'
      |                and rag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              join user_groups ug on ug.slug = rag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val usernameFilterPredicate: String =
    """
      |(? = false or lower(s.submitter_username) like lower(?) escape '\' or lower(au.display_name) like lower(?) escape '\')
      |""".stripMargin

  private val problemQueryFilterPredicate: String =
    """
      |(? = false or lower(p.slug) like lower(?) escape '\' or lower(p.title) like lower(?) escape '\')
      |""".stripMargin

  private val verdictFilterPredicate: String =
    """
      |(
      |  ? = true
      |  or (? = true and s.verdict is null)
      |  or (? = true and s.verdict = ?)
      |)
      |""".stripMargin

  val insertSql: String =
    s"""
      |insert into submissions (id, public_id, problem_id, submitter_username, language, status, verdict, judge_message, time_used_ms, memory_used_kb, source_code, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning public_id, status, verdict, judge_message, time_used_ms, memory_used_kb, ${UserIdentitySql.returningColumns("submitter_username", "submitter")}, submitted_at, started_at, finished_at
      |""".stripMargin

  val countSql: String =
    s"""
      |select count(*) as total_items
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where
      |  $summaryVisibilityPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |""".stripMargin

  def listSql(sort: SubmissionSort, direction: SubmissionSortDirection): String =
    s"""
      |select s.public_id,
      |       s.problem_id,
      |       p.slug as problem_slug,
      |       p.title as problem_title,
      |       $detailVisibilityPredicate as can_view_detail,
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
      |  $summaryVisibilityPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |order by ${orderByClause(sort, direction)}
      |limit ? offset ?
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

  private def orderByClause(sort: SubmissionSort, direction: SubmissionSortDirection): String =
    val submittedDirection = toSqlDirection(direction)
    sort match
      case SubmissionSort.Submitted =>
        s"s.submitted_at $submittedDirection, s.public_id $submittedDirection"
      case SubmissionSort.Time =>
        s"s.time_used_ms $submittedDirection nulls last, s.submitted_at desc, s.public_id desc"
      case SubmissionSort.Memory =>
        s"s.memory_used_kb $submittedDirection nulls last, s.submitted_at desc, s.public_id desc"
      case SubmissionSort.CodeLength =>
        s"octet_length(s.source_code) $submittedDirection, s.submitted_at desc, s.public_id desc"

  private def toSqlDirection(direction: SubmissionSortDirection): String =
    direction match
      case SubmissionSortDirection.Asc => "asc"
      case SubmissionSortDirection.Desc => "desc"
