package domains.contest.table.contest

import cats.effect.IO
import domains.contest.objects.ContestId
import domains.problem.objects.ProblemId

import java.sql.Connection

object ContestProblemVisibilityTable:

  private val hasOutsideContestManagerAudienceSQL: String =
    """
      |with contest_managers as (
      |  select aa.username
      |  from auth_accounts aa
      |  where aa.site_manager = true or aa.contest_manager = true
      |
      |  union
      |
      |  select cag.subject_key as username
      |  from contest_access_grants cag
      |  where cag.contest_id = ?
      |    and cag.grant_role = 'manager'
      |    and cag.subject_kind = 'user'
      |
      |  union
      |
      |  select ugm.username
      |  from contest_access_grants cag
      |  join user_groups ug on ug.slug = cag.subject_key
      |  join user_group_memberships ugm on ugm.user_group_id = ug.id
      |  where cag.contest_id = ?
      |    and cag.grant_role = 'manager'
      |    and cag.subject_kind = 'user_group'
      |)
      |select 1
      |from problems p
      |where p.id = ?
      |  and (
      |    p.base_access = 'public'
      |    or exists (
      |      select 1
      |      from problem_set_problems psp
      |      join problem_sets ps on ps.id = psp.problem_set_id
      |      where psp.problem_id = p.id
      |        and ps.base_access = 'public'
      |    )
      |    or exists (
      |      select 1
      |      from problem_access_grants pag
      |      join auth_accounts aa on aa.username = pag.subject_key
      |      left join contest_managers cm on cm.username = aa.username
      |      where pag.problem_id = p.id
      |        and pag.grant_role in ('viewer', 'manager')
      |        and pag.subject_kind = 'user'
      |        and cm.username is null
      |    )
      |    or exists (
      |      select 1
      |      from problem_access_grants pag
      |      join user_groups ug on ug.slug = pag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      join auth_accounts aa on aa.username = ugm.username
      |      left join contest_managers cm on cm.username = aa.username
      |      where pag.problem_id = p.id
      |        and pag.grant_role in ('viewer', 'manager')
      |        and pag.subject_kind = 'user_group'
      |        and cm.username is null
      |    )
      |    or exists (
      |      select 1
      |      from problem_set_problems psp
      |      join problem_set_access_grants psag on psag.problem_set_id = psp.problem_set_id
      |      join auth_accounts aa on aa.username = psag.subject_key
      |      left join contest_managers cm on cm.username = aa.username
      |      where psp.problem_id = p.id
      |        and psag.grant_role = 'viewer'
      |        and psag.subject_kind = 'user'
      |        and cm.username is null
      |    )
      |    or exists (
      |      select 1
      |      from problem_set_problems psp
      |      join problem_set_access_grants psag on psag.problem_set_id = psp.problem_set_id
      |      join user_groups ug on ug.slug = psag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      join auth_accounts aa on aa.username = ugm.username
      |      left join contest_managers cm on cm.username = aa.username
      |      where psp.problem_id = p.id
      |        and psag.grant_role = 'viewer'
      |        and psag.subject_kind = 'user_group'
      |        and cm.username is null
      |    )
      |  )
      |limit 1
      |""".stripMargin

  def hasOutsideContestManagerAudience(
    connection: Connection,
    contestId: ContestId,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasOutsideContestManagerAudienceSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setObject(2, contestId.value)
        statement.setObject(3, problemId.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }
