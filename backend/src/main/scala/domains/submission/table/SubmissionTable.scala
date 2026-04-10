package domains.submission.table

import cats.effect.IO
import domains.auth.model.{AuthUser, Username}
import domains.problem.model.{ProblemId, ProblemSlug}
import domains.submission.application.SubmissionPolicy
import domains.submission.model.{SubmissionDetail, SubmissionId, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionSummary, SubmissionVerdict}

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: Int,
  spaceLimitMb: Int
)

object SubmissionTable:

  val initTableSql: String =
    """
      |create table if not exists submissions (
      |  id uuid primary key,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  submitter_username varchar(120) not null references auth_users(username),
      |  language varchar(32) not null,
      |  status varchar(32) not null default 'queued',
      |  verdict varchar(64),
      |  judge_message text,
      |  source_code text not null,
      |  submitted_at timestamp not null,
      |  started_at timestamp,
      |  finished_at timestamp
      |);
      |""".stripMargin

  val addStatusColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'status'
      |  ) then
      |    alter table submissions add column status varchar(32);
      |  end if;
      |
      |  update submissions
      |  set status = 'queued'
      |  where status is null or btrim(status) = '';
      |end $$;
      |""".stripMargin

  val addJudgeResultColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'verdict'
      |  ) then
      |    alter table submissions add column verdict varchar(64);
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'judge_message'
      |  ) then
      |    alter table submissions add column judge_message text;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'started_at'
      |  ) then
      |    alter table submissions add column started_at timestamp;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'finished_at'
      |  ) then
      |    alter table submissions add column finished_at timestamp;
      |  end if;
      |end $$;
      |""".stripMargin

  val setStatusDefaultSql: String =
    """
      |alter table submissions
      |alter column status set default 'queued'
      |""".stripMargin

  val setStatusNotNullSql: String =
    """
      |alter table submissions
      |alter column status set not null
      |""".stripMargin

  val createPublicIdSequenceSql: String =
    """
      |create sequence if not exists submission_public_id_seq start with 1 increment by 1
      |""".stripMargin

  val addPublicIdColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'public_id'
      |  ) then
      |    alter table submissions add column public_id bigint;
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillPublicIdSql: String =
    """
      |with ordered_submissions as (
      |  select id, row_number() over (order by submitted_at asc, id asc) as next_public_id
      |  from submissions
      |  where public_id is null
      |)
      |update submissions s
      |set public_id = ordered_submissions.next_public_id
      |from ordered_submissions
      |where s.id = ordered_submissions.id
      |""".stripMargin

  val syncPublicIdSequenceSql: String =
    """
      |do $$
      |declare
      |  current_max_public_id bigint;
      |begin
      |  select max(public_id) into current_max_public_id from submissions;
      |  if current_max_public_id is not null then
      |    perform setval('submission_public_id_seq', current_max_public_id, true);
      |  end if;
      |end $$;
      |""".stripMargin

  val setPublicIdDefaultSql: String =
    """
      |alter table submissions
      |alter column public_id set default nextval('submission_public_id_seq')
      |""".stripMargin

  val setPublicIdNotNullSql: String =
    """
      |alter table submissions
      |alter column public_id set not null
      |""".stripMargin

  val addPublicIdUniqueConstraintSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'submissions_public_id_key'
      |  ) then
      |    alter table submissions add constraint submissions_public_id_key unique (public_id);
      |  end if;
      |end $$;
      |""".stripMargin

  val insertSql: String =
    """
      |insert into submissions (id, public_id, problem_id, submitter_username, language, status, verdict, judge_message, source_code, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning public_id, status, verdict, judge_message, submitted_at, started_at, finished_at
      |""".stripMargin

  val listSql: String =
    """
      |select s.public_id, s.problem_id, p.slug as problem_slug, s.submitter_username, s.language, s.status, s.verdict, s.submitted_at, s.started_at, s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |where
      |  (? = false or s.submitter_username = ?)
      |  and (
      |    ? = true
      |    or p.owner_username = ?
      |    or p.base_access = 'public'
      |    or exists (
      |      select 1
      |      from resource_viewer_grants rvg
      |      where rvg.resource_kind = 'problem'
      |        and rvg.resource_id = p.id
      |        and rvg.subject_kind = 'user'
      |        and rvg.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from resource_viewer_grants rvg
      |      join user_groups ug on ug.slug = rvg.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where rvg.resource_kind = 'problem'
      |        and rvg.resource_id = p.id
      |        and rvg.subject_kind = 'user_group'
      |        and ugm.username = ?
      |    )
      |    or exists (
      |      select 1
      |      from problem_set_problems psp
      |      join problem_sets ps on ps.id = psp.problem_set_id
      |      where psp.problem_id = p.id
      |        and (
      |          ? = true
      |          or ps.owner_username = ?
      |          or ps.base_access = 'public'
      |          or exists (
      |            select 1
      |            from resource_viewer_grants rvg
      |            where rvg.resource_kind = 'problem_set'
      |              and rvg.resource_id = ps.id
      |              and rvg.subject_kind = 'user'
      |              and rvg.subject_key = ?
      |          )
      |          or exists (
      |            select 1
      |            from resource_viewer_grants rvg
      |            join user_groups ug on ug.slug = rvg.subject_key
      |            join user_group_memberships ugm on ugm.user_group_id = ug.id
      |            where rvg.resource_kind = 'problem_set'
      |              and rvg.resource_id = ps.id
      |              and rvg.subject_kind = 'user_group'
      |              and ugm.username = ?
      |          )
      |        )
      |    )
      |  )
      |order by s.submitted_at desc, s.public_id desc
      |""".stripMargin

  val findByIdSql: String =
    """
      |select s.public_id, s.problem_id, p.slug as problem_slug, s.submitter_username, s.language, s.status, s.verdict, s.judge_message, s.source_code, s.submitted_at, s.started_at, s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
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
      |set status = 'running',
      |    started_at = ?,
      |    finished_at = null,
      |    verdict = null,
      |    judge_message = null
      |from next_submission ns, problems p
      |where s.id = ns.id
      |  and p.id = s.problem_id
      |returning s.public_id, s.problem_id, p.slug as problem_slug, s.language, s.source_code, p.time_limit_ms, p.space_limit_mb
      |""".stripMargin

  val markCompletedSql: String =
    """
      |update submissions
      |set status = ?, verdict = ?, judge_message = ?, finished_at = ?
      |where public_id = ?
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(addStatusColumnSql)
        statement.execute(addJudgeResultColumnsSql)
        statement.execute(createPublicIdSequenceSql)
        statement.execute(addPublicIdColumnSql)
        statement.execute(backfillPublicIdSql)
        statement.execute(syncPublicIdSequenceSql)
        statement.execute(setStatusDefaultSql)
        statement.execute(setStatusNotNullSql)
        statement.execute(setPublicIdDefaultSql)
        statement.execute(setPublicIdNotNullSql)
        statement.execute(addPublicIdUniqueConstraintSql)
      finally statement.close()
    }

  def insert(
    connection: Connection,
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    submitterUsername: Username,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  ): IO[SubmissionDetail] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, problemId.value)
        statement.setString(3, submitterUsername.value)
        statement.setString(4, SubmissionLanguage.toDatabase(language))
        statement.setString(5, SubmissionStatus.toDatabase(SubmissionStatus.Queued))
        statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setNull(7, java.sql.Types.LONGVARCHAR)
        statement.setString(8, sourceCode.value)
        statement.setTimestamp(9, Timestamp.from(now))
        statement.setNull(10, java.sql.Types.TIMESTAMP)
        statement.setNull(11, java.sql.Types.TIMESTAMP)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            SubmissionDetail(
              id = SubmissionId(resultSet.getLong("public_id")),
              problemId = problemId,
              problemSlug = problemSlug,
              submitterUsername = submitterUsername,
              language = language,
              status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
              verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
              judgeMessage = Option(resultSet.getString("judge_message")),
              sourceCode = sourceCode,
              submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
              startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
              finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
            )
          else missingInsertResult("submission")
        finally resultSet.close()
      finally statement.close()
    }

  def listVisibleTo(connection: Connection, actor: AuthUser, submitterUsername: Option[Username]): IO[List[SubmissionSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        statement.setBoolean(1, submitterUsername.nonEmpty)
        statement.setString(2, submitterUsername.map(_.value).getOrElse(""))
        statement.setBoolean(3, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(4, actor.username.value)
        statement.setString(5, actor.username.value)
        statement.setString(6, actor.username.value)
        statement.setBoolean(7, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(8, actor.username.value)
        statement.setString(9, actor.username.value)
        statement.setString(10, actor.username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readSubmissionSummary(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def findById(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSql)
      try
        statement.setLong(1, submissionId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readSubmissionDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  def claimNextCpp17(connection: Connection): IO[Option[ClaimedSubmission]] =
    IO.blocking {
      val statement = connection.prepareStatement(claimNextCpp17Sql)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ClaimedSubmission(
                id = SubmissionId(resultSet.getLong("public_id")),
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
                problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
                language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
                sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse),
                timeLimitMs = resultSet.getInt("time_limit_ms"),
                spaceLimitMb = resultSet.getInt("space_limit_mb")
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def markCompleted(
    connection: Connection,
    submissionId: SubmissionId,
    status: SubmissionStatus,
    verdict: Option[SubmissionVerdict],
    judgeMessage: Option[String]
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(markCompletedSql)
      try
        statement.setString(1, SubmissionStatus.toDatabase(status))
        verdict match
          case Some(value) => statement.setString(2, SubmissionVerdict.toDatabase(value))
          case None => statement.setNull(2, java.sql.Types.VARCHAR)
        judgeMessage match
          case Some(value) => statement.setString(3, value)
          case None => statement.setNull(3, java.sql.Types.LONGVARCHAR)
        statement.setTimestamp(4, Timestamp.from(Instant.now()))
        statement.setLong(5, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def readSubmissionSummary(resultSet: java.sql.ResultSet): SubmissionSummary =
    SubmissionSummary(
      id = SubmissionId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
      problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
      submitterUsername = Username.canonical(resultSet.getString("submitter_username")),
      language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
      status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
      verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
      submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
      startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )

  private def readSubmissionDetail(resultSet: java.sql.ResultSet): SubmissionDetail =
    SubmissionDetail(
      id = SubmissionId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
      problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
      submitterUsername = Username.canonical(resultSet.getString("submitter_username")),
      language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
      status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
      verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
      judgeMessage = Option(resultSet.getString("judge_message")),
      sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse),
      submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
      startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  private def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")
