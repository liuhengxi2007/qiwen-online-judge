package domains.hack.table.hack

import cats.effect.IO

import java.sql.Connection

object HackTableSchema:

  val createPublicIdSequenceSql: String =
    """
      |create sequence if not exists hack_public_id_seq start with 1 increment by 1
      |""".stripMargin

  val initAttemptTableSql: String =
    """
      |create table if not exists hack_attempts (
      |  id uuid primary key,
      |  public_id bigint not null default nextval('hack_public_id_seq'),
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  target_submission_public_id bigint not null references submissions(public_id) on delete cascade,
      |  author_username varchar(120) not null references auth_accounts(username),
      |  subtask_index integer not null,
      |  subtask_label varchar(255),
      |  status varchar(32) not null default 'queued' check (status in ('queued', 'running', 'success', 'no_effect', 'invalid', 'failed')),
      |  input_text text not null,
      |  strategy_provider_source text,
      |  answer_text text,
      |  old_score numeric not null,
      |  new_score numeric,
      |  validator_message text,
      |  standard_message text,
      |  target_message text,
      |  created_at timestamp not null,
      |  started_at timestamp,
      |  finished_at timestamp
      |);
      |""".stripMargin

  val initProblemHackTestcaseTableSql: String =
    """
      |create table if not exists problem_hack_testcases (
      |  id uuid primary key,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  hack_attempt_public_id bigint not null unique references hack_attempts(public_id) on delete cascade,
      |  subtask_index integer not null,
      |  input_text text not null,
      |  answer_text text,
      |  strategy_provider_source text,
      |  created_by_username varchar(120) not null references auth_accounts(username),
      |  created_at timestamp not null
      |);
      |""".stripMargin

  val allowProblemHackTestcaseAnswerNullSql: String =
    """
      |alter table problem_hack_testcases
      |alter column answer_text drop not null
      |""".stripMargin

  val addPublicIdUniqueConstraintSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'hack_attempts_public_id_key'
      |  ) then
      |    alter table hack_attempts add constraint hack_attempts_public_id_key unique (public_id);
      |  end if;
      |end $$;
      |""".stripMargin

  val createIndexesSql: String =
    """
      |create index if not exists hack_attempts_created_at_idx
      |  on hack_attempts (created_at desc, public_id desc);
      |create index if not exists hack_attempts_status_idx
      |  on hack_attempts (status, created_at asc, public_id asc);
      |create index if not exists hack_attempts_target_submission_idx
      |  on hack_attempts (target_submission_public_id);
      |create index if not exists problem_hack_testcases_problem_idx
      |  on problem_hack_testcases (problem_id, subtask_index, created_at asc);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        List(
          createPublicIdSequenceSql,
          initAttemptTableSql,
          addPublicIdUniqueConstraintSql,
          initProblemHackTestcaseTableSql,
          allowProblemHackTestcaseAnswerNullSql,
          createIndexesSql
        ).foreach(statement.execute)
      finally statement.close()
    }
