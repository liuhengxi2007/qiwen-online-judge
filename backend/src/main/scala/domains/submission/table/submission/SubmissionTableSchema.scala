package domains.submission.table.submission



import cats.effect.IO

import java.sql.Connection

object SubmissionTableSchema:

  val initTableSql: String =
    """
      |create table if not exists submissions (
      |  id uuid primary key,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  submitter_username varchar(120) not null references auth_accounts(username),
      |  language varchar(32) not null,
      |  status varchar(32) not null default 'queued',
      |  verdict varchar(64),
      |  judge_message text,
      |  time_used_ms bigint,
      |  memory_used_kb bigint,
      |  score numeric,
      |  judge_result jsonb,
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
      |      and column_name = 'time_used_ms'
      |  ) then
      |    alter table submissions add column time_used_ms bigint;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'memory_used_kb'
      |  ) then
      |    alter table submissions add column memory_used_kb bigint;
      |  end if;
      |
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
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'score'
      |  ) then
      |    alter table submissions add column score numeric;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'submissions'
      |      and column_name = 'judge_result'
      |  ) then
      |    alter table submissions add column judge_result jsonb;
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
