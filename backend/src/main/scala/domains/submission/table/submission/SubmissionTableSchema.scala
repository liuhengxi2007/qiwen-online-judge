package domains.submission.table.submission

import cats.effect.IO

import java.sql.Connection

object SubmissionTableSchema:

  val createPublicIdSequenceSql: String =
    """
      |create sequence if not exists submission_public_id_seq start with 1 increment by 1
      |""".stripMargin

  val initTableSql: String =
    """
      |create table if not exists submissions (
      |  id uuid primary key,
      |  public_id bigint not null default nextval('submission_public_id_seq'),
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  contest_id uuid references contests(id) on delete set null,
      |  submitter_username varchar(120) not null references auth_accounts(username),
      |  program_manifest jsonb not null,
      |  language varchar(32) generated always as ((program_manifest #>> array['programs', program_manifest ->> 'defaultProgramKey', 'language'])) stored,
      |  status varchar(32) not null default 'queued',
      |  judge_priority integer not null default 0,
      |  judge_queued_at timestamp not null default current_timestamp,
      |  hack_revision bigint not null default 0,
      |  judge_result jsonb,
      |  verdict varchar(64) generated always as (judge_result #>> '{baseResult,verdict}') stored,
      |  time_used_ms bigint generated always as ((judge_result #>> '{baseResult,timeUsedMs}')::bigint) stored,
      |  memory_used_kb bigint generated always as ((judge_result #>> '{baseResult,memoryUsedKb}')::bigint) stored,
      |  score numeric generated always as ((judge_result #>> '{baseResult,score}')::numeric) stored,
      |  code_length integer generated always as (((program_manifest #>> array['programs', program_manifest ->> 'defaultProgramKey', 'sizeBytes'])::integer)) stored,
      |  submitted_at timestamp not null,
      |  started_at timestamp,
      |  finished_at timestamp
      |);
      |""".stripMargin

  val addLifecycleColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'contest_id'
      |  ) then
      |    alter table submissions add column contest_id uuid references contests(id) on delete set null;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'status'
      |  ) then
      |    alter table submissions add column status varchar(32);
      |  end if;
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'judge_result'
      |  ) then
      |    alter table submissions add column judge_result jsonb;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'judge_priority'
      |  ) then
      |    alter table submissions add column judge_priority integer;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'judge_queued_at'
      |  ) then
      |    alter table submissions add column judge_queued_at timestamp;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'hack_revision'
      |  ) then
      |    alter table submissions add column hack_revision bigint;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'started_at'
      |  ) then
      |    alter table submissions add column started_at timestamp;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'finished_at'
      |  ) then
      |    alter table submissions add column finished_at timestamp;
      |  end if;
      |
      |  update submissions
      |  set status = 'queued'
      |  where status is null or btrim(status) = '';
      |
      |  update submissions
      |  set judge_priority = 0
      |  where judge_priority is null;
      |
      |  update submissions
      |  set judge_queued_at = submitted_at
      |  where judge_queued_at is null;
      |
      |  update submissions
      |  set hack_revision = 0
      |  where hack_revision is null;
      |end $$;
      |""".stripMargin

  val backfillContestSubmissionSourceSql: String =
    """
      |with contest_submission_sources as (
      |  select
      |    s.id as submission_id,
      |    (array_agg(c.id order by c.start_at desc, c.id))[1] as contest_id,
      |    count(*) as contest_count
      |  from submissions s
      |  join contest_problems cp on cp.problem_id = s.problem_id
      |  join contests c on c.id = cp.contest_id
      |  join contest_registrations cr on cr.contest_id = c.id and cr.username = s.submitter_username
      |  where s.contest_id is null
      |    and s.submitted_at >= c.start_at
      |    and s.submitted_at <= c.end_at
      |  group by s.id
      |)
      |update submissions s
      |set contest_id = contest_submission_sources.contest_id
      |from contest_submission_sources
      |where s.id = contest_submission_sources.submission_id
      |  and s.contest_id is null
      |  and contest_submission_sources.contest_count = 1
      |""".stripMargin

  val ensureLegacyJudgeScalarColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'verdict'
      |  ) then
      |    alter table submissions add column verdict varchar(64);
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'time_used_ms'
      |  ) then
      |    alter table submissions add column time_used_ms bigint;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'memory_used_kb'
      |  ) then
      |    alter table submissions add column memory_used_kb bigint;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'score'
      |  ) then
      |    alter table submissions add column score numeric;
      |  end if;
      |end $$;
      |""".stripMargin

  val addProgramManifestColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'program_manifest'
      |  ) then
      |    alter table submissions add column program_manifest jsonb;
      |  end if;
      |end $$;
      |""".stripMargin

  val setProgramManifestNotNullSql: String =
    """
      |alter table submissions
      |alter column program_manifest set not null
      |""".stripMargin

  val backfillJudgeResultFromLegacySql: String =
    """
      |update submissions
      |set judge_result = jsonb_strip_nulls(
      |  jsonb_build_object(
      |    'baseResult', jsonb_strip_nulls(jsonb_build_object(
      |      'score', coalesce(score, 0),
      |      'verdict', verdict,
      |      'timeUsedMs', time_used_ms,
      |      'memoryUsedKb', memory_used_kb
      |    )),
      |    'worstResult', jsonb_strip_nulls(jsonb_build_object(
      |      'score', coalesce(score, 0),
      |      'verdict', verdict,
      |      'timeUsedMs', time_used_ms,
      |      'memoryUsedKb', memory_used_kb
      |    )),
      |    'subtasks', '[]'::jsonb
      |  )
      |)
      |where judge_result is null
      |  and verdict is not null
      |""".stripMargin

  val backfillJudgeResultSummaryVerdictsSql: String =
    """
      |update submissions
      |set judge_result = jsonb_set(
      |  jsonb_set(
      |    judge_result,
      |    '{baseResult,verdict}',
      |    to_jsonb(judge_result ->> 'verdict'),
      |    true
      |  ),
      |  '{worstResult,verdict}',
      |  to_jsonb(judge_result ->> 'verdict'),
      |  true
      |)
      |where judge_result ? 'verdict'
      |  and (
      |    judge_result #>> '{baseResult,verdict}' is null
      |    or judge_result #>> '{worstResult,verdict}' is null
      |  )
      |""".stripMargin

  val replaceLanguageWithGeneratedSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'language'
      |      and is_generated <> 'ALWAYS'
      |  ) then
      |    alter table submissions drop column language;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'language'
      |  ) then
      |    alter table submissions add column language varchar(32)
      |      generated always as ((program_manifest #>> array['programs', program_manifest ->> 'defaultProgramKey', 'language'])) stored;
      |  end if;
      |end $$;
      |""".stripMargin

  val replaceJudgeDerivedColumnsSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'verdict'
      |      and (is_generated <> 'ALWAYS' or generation_expression not like '%baseResult%')
      |  ) then
      |    alter table submissions drop column verdict;
      |  end if;
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'verdict'
      |  ) then
      |    alter table submissions add column verdict varchar(64)
      |      generated always as (judge_result #>> '{baseResult,verdict}') stored;
      |  end if;
      |
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'time_used_ms'
      |      and (is_generated <> 'ALWAYS' or generation_expression like '%CASE WHEN%' or generation_expression not like '%baseResult%')
      |  ) then
      |    alter table submissions drop column time_used_ms;
      |  end if;
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'time_used_ms'
      |  ) then
      |    alter table submissions add column time_used_ms bigint
      |      generated always as ((judge_result #>> '{baseResult,timeUsedMs}')::bigint) stored;
      |  end if;
      |
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'memory_used_kb'
      |      and (is_generated <> 'ALWAYS' or generation_expression like '%CASE WHEN%' or generation_expression not like '%baseResult%')
      |  ) then
      |    alter table submissions drop column memory_used_kb;
      |  end if;
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'memory_used_kb'
      |  ) then
      |    alter table submissions add column memory_used_kb bigint
      |      generated always as ((judge_result #>> '{baseResult,memoryUsedKb}')::bigint) stored;
      |  end if;
      |
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'score'
      |      and (is_generated <> 'ALWAYS' or generation_expression like '%CASE WHEN%' or generation_expression not like '%baseResult%')
      |  ) then
      |    alter table submissions drop column score;
      |  end if;
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'score'
      |  ) then
      |    alter table submissions add column score numeric
      |      generated always as ((judge_result #>> '{baseResult,score}')::numeric) stored;
      |  end if;
      |end $$;
      |""".stripMargin

  val addCodeLengthGeneratedColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'code_length' and is_generated <> 'ALWAYS'
      |  ) then
      |    alter table submissions drop column code_length;
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'code_length'
      |  ) then
      |    alter table submissions add column code_length integer
      |      generated always as (((program_manifest #>> array['programs', program_manifest ->> 'defaultProgramKey', 'sizeBytes'])::integer)) stored;
      |  end if;
      |end $$;
      |""".stripMargin

  val dropSourceCodeColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'source_code'
      |  ) then
      |    alter table submissions drop column source_code;
      |  end if;
      |end $$;
      |""".stripMargin

  val dropJudgeMessageColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public' and table_name = 'submissions' and column_name = 'judge_message'
      |  ) then
      |    alter table submissions drop column judge_message;
      |  end if;
      |end $$;
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

  val setStatusDefaultSql: String =
    """
      |alter table submissions
      |alter column status set default 'queued'
      |""".stripMargin

  val setJudgePriorityDefaultSql: String =
    """
      |alter table submissions
      |alter column judge_priority set default 0
      |""".stripMargin

  val setJudgeQueuedAtDefaultSql: String =
    """
      |alter table submissions
      |alter column judge_queued_at set default current_timestamp
      |""".stripMargin

  val setHackRevisionDefaultSql: String =
    """
      |alter table submissions
      |alter column hack_revision set default 0
      |""".stripMargin

  val setStatusNotNullSql: String =
    """
      |alter table submissions
      |alter column status set not null
      |""".stripMargin

  val setJudgePriorityNotNullSql: String =
    """
      |alter table submissions
      |alter column judge_priority set not null
      |""".stripMargin

  val setJudgeQueuedAtNotNullSql: String =
    """
      |alter table submissions
      |alter column judge_queued_at set not null
      |""".stripMargin

  val setHackRevisionNotNullSql: String =
    """
      |alter table submissions
      |alter column hack_revision set not null
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

  val createIndexesSql: String =
    """
      |create index if not exists submissions_submitted_at_idx
      |  on submissions (submitted_at desc, public_id desc);
      |create index if not exists submissions_verdict_idx
      |  on submissions (verdict);
      |create index if not exists submissions_time_used_ms_idx
      |  on submissions (time_used_ms);
      |create index if not exists submissions_memory_used_kb_idx
      |  on submissions (memory_used_kb);
      |create index if not exists submissions_code_length_idx
      |  on submissions (code_length);
      |create index if not exists submissions_contest_id_submitted_at_idx
      |  on submissions (contest_id, submitted_at desc, public_id desc);
      |create index if not exists submissions_queued_language_priority_idx
      |  on submissions (language, judge_priority desc, judge_queued_at asc, public_id asc)
      |  where status = 'queued';
      |""".stripMargin

  def initializeBeforeProgramManifestBackfill(connection: Connection): IO[Unit] =
    executeStatements(
      connection,
      List(
        createPublicIdSequenceSql,
        initTableSql,
        addLifecycleColumnsSql,
        backfillContestSubmissionSourceSql,
        ensureLegacyJudgeScalarColumnsSql,
        addProgramManifestColumnSql,
        addPublicIdColumnSql,
        backfillPublicIdSql,
        syncPublicIdSequenceSql
      )
    )

  def initializeAfterProgramManifestBackfill(connection: Connection): IO[Unit] =
    executeStatements(
      connection,
      List(
        setProgramManifestNotNullSql,
        backfillJudgeResultFromLegacySql,
        backfillJudgeResultSummaryVerdictsSql,
        replaceLanguageWithGeneratedSql,
        addCodeLengthGeneratedColumnSql,
        replaceJudgeDerivedColumnsSql,
        dropSourceCodeColumnSql,
        dropJudgeMessageColumnSql,
        setStatusDefaultSql,
        setStatusNotNullSql,
        setJudgePriorityDefaultSql,
        setJudgePriorityNotNullSql,
        setJudgeQueuedAtDefaultSql,
        setJudgeQueuedAtNotNullSql,
        setHackRevisionDefaultSql,
        setHackRevisionNotNullSql,
        setPublicIdDefaultSql,
        setPublicIdNotNullSql,
        addPublicIdUniqueConstraintSql,
        createIndexesSql
      )
    )

  def initialize(connection: Connection): IO[Unit] =
    initializeBeforeProgramManifestBackfill(connection).flatMap(_ => initializeAfterProgramManifestBackfill(connection))

  private def executeStatements(connection: Connection, statements: List[String]): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statements.foreach(sql => statement.execute(sql))
      finally statement.close()
    }
