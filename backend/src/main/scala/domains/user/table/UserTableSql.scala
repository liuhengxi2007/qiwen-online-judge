package domains.user.table

object UserTableSql:
  val suggestionLimit: Int = 5

  val findByUsernameSql: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  val listUsersSql: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, site_manager, problem_manager
      |from auth_users
      |order by username asc
      |""".stripMargin

  val listSuggestionsSql: String =
    s"""
      |select username as submitter_username,
      |       display_name as submitter_display_name
      |from auth_users
      |where lower(username) like lower(?)
      |   or lower(display_name) like lower(?)
      |order by
      |  case
      |    when lower(username) = lower(?) then 0
      |    when lower(username) like lower(?) then 1
      |    when lower(display_name) like lower(?) then 2
      |    when position(lower(?) in lower(username)) > 0 then 3
      |    else 4
      |  end,
      |  lower(username) asc
      |limit $suggestionLimit
      |""".stripMargin

  val countUsersSql: String =
    """
      |select count(*) as total_items
      |from auth_users
      |""".stripMargin

  val listContributionRanklistSql: String =
    """
      |with blog_scores as (
      |  select b.author_username,
      |         sum(case when bv.vote = 'up' then 1 when bv.vote = 'down' then -1 else 0 end)::numeric as blog_score
      |  from blogs b
      |  left join blog_votes bv on bv.blog_id = b.id
      |  group by b.author_username
      |),
      |comment_scores as (
      |  select c.author_username,
      |         sum(case when bcv.vote = 'up' then 1 when bcv.vote = 'down' then -1 else 0 end)::numeric as comment_score
      |  from blog_comments c
      |  left join blog_comment_votes bcv on bcv.comment_id = c.id
      |  group by c.author_username
      |)
      |select au.username,
      |       au.display_name,
      |       round(coalesce(blog_scores.blog_score, 0)::numeric + coalesce(comment_scores.comment_score, 0)::numeric * 0.1) as contribution
      |from auth_users au
      |left join blog_scores on blog_scores.author_username = au.username
      |left join comment_scores on comment_scores.author_username = au.username
      |order by contribution desc, lower(au.display_name) asc, lower(au.username) asc
      |limit ? offset ?
      |""".stripMargin

  val listAcceptedRanklistSql: String =
    """
      |with accepted_counts as (
      |  select lower(s.submitter_username) as submitter_username,
      |         count(distinct s.problem_id)::int as accepted_count
      |  from submissions s
      |  where s.verdict = 'accepted'
      |  group by lower(s.submitter_username)
      |)
      |select au.username,
      |       au.display_name,
      |       coalesce(accepted_counts.accepted_count, 0) as accepted_count
      |from auth_users au
      |left join accepted_counts on accepted_counts.submitter_username = lower(au.username)
      |order by accepted_count desc, lower(au.display_name) asc, lower(au.username) asc
      |limit ? offset ?
      |""".stripMargin

  val listAcceptedProblemsSql: String =
    """
      |select p.slug,
      |       p.title,
      |       max(coalesce(s.finished_at, s.submitted_at)) as accepted_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |where lower(s.submitter_username) = lower(?)
      |  and s.verdict = 'accepted'
      |group by p.slug, p.title
      |order by accepted_at desc, p.slug asc
      |""".stripMargin

  val updatePermissionsSql: String =
    """
      |update auth_users
      |set site_manager = ?, problem_manager = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin

  val updateSettingsSql: String =
    """
      |update auth_users
      |set display_name = ?, email = ?, display_mode = ?, locale = ?, problem_title_display_mode = ?, password_hash = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from auth_users
      |where username = ?
      |""".stripMargin
