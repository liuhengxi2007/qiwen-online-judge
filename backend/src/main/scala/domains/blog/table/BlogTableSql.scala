package domains.blog.table

import domains.shared.sql.UserIdentitySql

object BlogTableSql:

  val insertSql: String =
    s"""
      |insert into blogs (id, public_id, author_username, title, content, visibility, created_at, updated_at)
      |values (?, nextval('blog_public_id_seq'), ?, ?, ?, ?, ?, ?)
      |returning public_id, title, content, visibility, ${UserIdentitySql.returningColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  private val blogSelectColumns: String =
    s"""
      |b.public_id,
      |       b.title,
      |       b.content,
      |       b.visibility,
      |       ${UserIdentitySql.selectColumns("b.author_username", "author", "au")},
      |       coalesce(vs.score, 0) as score,
      |       viewer_vote.vote as viewer_vote,
      |       b.created_at,
      |       b.updated_at
      |""".stripMargin

  private val blogScoreJoinSql: String =
    """
      |left join (
      |  select blog_id,
      |         sum(case when vote = 'up' then 1 when vote = 'down' then -1 else 0 end)::int as score
      |  from blog_votes
      |  group by blog_id
      |) vs on vs.blog_id = b.id
      |""".stripMargin

  val listSql: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSql
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.visibility = 'public' or b.author_username = ?
      |order by b.created_at desc, b.public_id desc
      |""".stripMargin

  val listByAuthorSql: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSql
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.author_username = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by b.public_id asc
      |""".stripMargin

  val listByProblemSql: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSql
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where p.slug = ?
      |  and bpl.status = 'accepted'
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by b.created_at desc, b.public_id desc
      |""".stripMargin

  val contributionByAuthorSql: String =
    """
      |select coalesce(blog_scores.blog_score, 0)::numeric +
      |       coalesce(comment_scores.comment_score, 0)::numeric * 0.1 as contribution
      |from (select ?::varchar as username) target
      |left join (
      |  select b.author_username,
      |         sum(case when bv.vote = 'up' then 1 when bv.vote = 'down' then -1 else 0 end)::numeric as blog_score
      |  from blogs b
      |  left join blog_votes bv on bv.blog_id = b.id
      |  where lower(b.author_username) = lower(?)
      |  group by b.author_username
      |) blog_scores on blog_scores.author_username = target.username
      |left join (
      |  select c.author_username,
      |         sum(case when bcv.vote = 'up' then 1 when bcv.vote = 'down' then -1 else 0 end)::numeric as comment_score
      |  from blog_comments c
      |  left join blog_comment_votes bcv on bcv.comment_id = c.id
      |  where lower(c.author_username) = lower(?)
      |  group by c.author_username
      |) comment_scores on comment_scores.author_username = target.username
      |""".stripMargin

  val findByIdSql: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSql
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |""".stripMargin

  val updateBlogSql: String =
    """
      |update blogs
      |set title = ?,
      |    content = ?,
      |    visibility = ?,
      |    updated_at = ?
      |where public_id = ?
      |  and author_username = ?
      |""".stripMargin

  val listRelatedProblemsSql: String =
    """
      |select p.slug, p.title
      |from blog_problem_links bpl
      |join blogs b on b.id = bpl.blog_id
      |join problems p on p.id = bpl.problem_id
      |where b.public_id = ?
      |  and bpl.status = 'accepted'
      |order by bpl.linked_at desc, p.slug asc
      |""".stripMargin

  val listPendingByProblemSql: String =
    s"""
      |select $blogSelectColumns
      |from blogs b
      |join blog_problem_links bpl on bpl.blog_id = b.id
      |join problems p on p.id = bpl.problem_id
      |${UserIdentitySql.joinAuthUsers("b.author_username", "au")}
      |$blogScoreJoinSql
      |left join blog_votes viewer_vote on viewer_vote.blog_id = b.id and viewer_vote.username = ?
      |where p.slug = ?
      |  and bpl.status = 'pending'
      |order by bpl.linked_at asc, b.public_id asc
      |""".stripMargin

  val linkProblemSql: String =
    """
      |insert into blog_problem_links (blog_id, problem_id, linked_by, linked_at, status)
      |select b.id, p.id, ?, ?, 'accepted'
      |from blogs b
      |join problems p on p.slug = ?
      |where b.public_id = ?
      |  and b.visibility = 'public'
      |on conflict (blog_id, problem_id)
      |do update set status = 'accepted',
      |              linked_by = excluded.linked_by,
      |              linked_at = excluded.linked_at
      |""".stripMargin

  val submitProblemLinkSql: String =
    """
      |insert into blog_problem_links (blog_id, problem_id, linked_by, linked_at, status)
      |select b.id, p.id, ?, ?, 'pending'
      |from blogs b
      |join problems p on p.slug = ?
      |where b.public_id = ?
      |  and b.visibility = 'public'
      |  and b.author_username = ?
      |on conflict (blog_id, problem_id) do nothing
      |""".stripMargin

  val acceptProblemLinkSql: String =
    """
      |update blog_problem_links bpl
      |set status = 'accepted',
      |    linked_by = ?,
      |    linked_at = ?
      |from blogs b, problems p
      |where bpl.blog_id = b.id
      |  and bpl.problem_id = p.id
      |  and b.public_id = ?
      |  and p.slug = ?
      |  and bpl.status = 'pending'
      |""".stripMargin

  val deleteProblemLinkSql: String =
    """
      |delete from blog_problem_links bpl
      |using blogs b, problems p
      |where bpl.blog_id = b.id
      |  and bpl.problem_id = p.id
      |  and b.public_id = ?
      |  and p.slug = ?
      |""".stripMargin

  val deleteBlogSql: String =
    """
      |delete from blogs
      |where public_id = ?
      |  and author_username = ?
      |""".stripMargin

  val upsertVoteSql: String =
    """
      |insert into blog_votes (blog_id, username, vote, created_at, updated_at)
      |select id, ?, ?, ?, ?
      |from blogs
      |where public_id = ?
      |  and (visibility = 'public' or author_username = ?)
      |on conflict (blog_id, username)
      |do update set vote = excluded.vote,
      |              updated_at = excluded.updated_at
      |""".stripMargin

  val deleteVoteSql: String =
    """
      |delete from blog_votes
      |where blog_id = (select id from blogs where public_id = ? and (visibility = 'public' or author_username = ?))
      |  and username = ?
      |""".stripMargin

  val findViewerVoteSql: String =
    """
      |select bv.vote
      |from blog_votes bv
      |join blogs b on b.id = bv.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and bv.username = ?
      |""".stripMargin

  val listCommentsSql: String =
    s"""
      |select c.public_id,
      |       pc.public_id as parent_id,
      |       c.content,
      |       ${UserIdentitySql.selectColumns("c.author_username", "author", "au")},
      |       coalesce(cvs.score, 0) as score,
      |       viewer_vote.vote as viewer_vote,
      |       c.created_at,
      |       c.updated_at
      |from blog_comments c
      |join blogs b on b.id = c.blog_id
      |left join blog_comments pc on pc.id = c.parent_comment_id
      |${UserIdentitySql.joinAuthUsers("c.author_username", "au")}
      |left join (
      |  select comment_id,
      |         sum(case when vote = 'up' then 1 when vote = 'down' then -1 else 0 end)::int as score
      |  from blog_comment_votes
      |  group by comment_id
      |) cvs on cvs.comment_id = c.id
      |left join blog_comment_votes viewer_vote on viewer_vote.comment_id = c.id and viewer_vote.username = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |order by c.public_id asc
      |""".stripMargin

  val insertCommentSql: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), id, null, ?, ?, ?, ?
      |from blogs
      |where public_id = ?
      |  and (visibility = 'public' or author_username = ?)
      |returning public_id, content, ${UserIdentitySql.returningColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  val insertReplySql: String =
    s"""
      |insert into blog_comments (id, public_id, blog_id, parent_comment_id, author_username, content, created_at, updated_at)
      |select ?, nextval('blog_comment_public_id_seq'), b.id, parent_comment.id, ?, ?, ?, ?
      |from blogs b
      |join blog_comments parent_comment on parent_comment.blog_id = b.id and parent_comment.public_id = ?
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |returning public_id, content, ${UserIdentitySql.returningColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  val findCommentVoteSql: String =
    """
      |select bcv.vote
      |from blog_comment_votes bcv
      |join blog_comments c on c.id = bcv.comment_id
      |join blogs b on b.id = c.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and bcv.username = ?
      |""".stripMargin

  val upsertCommentVoteSql: String =
    """
      |insert into blog_comment_votes (comment_id, username, vote, created_at, updated_at)
      |select c.id, ?, ?, ?, ?
      |from blog_comments c
      |join blogs b on b.id = c.blog_id
      |where b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |on conflict (comment_id, username)
      |do update set vote = excluded.vote,
      |              updated_at = excluded.updated_at
      |""".stripMargin

  val deleteCommentVoteSql: String =
    """
      |delete from blog_comment_votes
      |where comment_id = (
      |  select c.id
      |  from blog_comments c
      |  join blogs b on b.id = c.blog_id
      |  where b.public_id = ?
      |    and (b.visibility = 'public' or b.author_username = ?)
      |    and c.public_id = ?
      |)
      |and username = ?
      |""".stripMargin

  val updateCommentSql: String =
    """
      |update blog_comments c
      |set content = ?,
      |    updated_at = ?
      |from blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin

  val deleteCommentSql: String =
    """
      |delete from blog_comments c
      |using blogs b
      |where b.id = c.blog_id
      |  and b.public_id = ?
      |  and (b.visibility = 'public' or b.author_username = ?)
      |  and c.public_id = ?
      |  and c.author_username = ?
      |""".stripMargin
