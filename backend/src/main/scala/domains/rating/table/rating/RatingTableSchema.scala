package domains.rating.table.rating

import cats.effect.IO

import java.sql.Connection

/** 评分表结构初始化对象，维护评分比赛序列和用户当前评分状态表。 */
object RatingTableSchema:

  val initContestTableSql: String =
    """
      |create table if not exists rating_contests (
      |  position integer primary key,
      |  contest_slug varchar(64) not null,
      |  contest_title varchar(120) not null,
      |  contest_start_at timestamp not null,
      |  contest_end_at timestamp not null,
      |  rating_m integer not null,
      |  participant_count integer not null,
      |  overlap_warning boolean not null,
      |  ranking_snapshot_json text not null,
      |  appended_by_username varchar(120) references auth_accounts(username) on delete set null,
      |  appended_at timestamp not null,
      |  constraint rating_contests_m_check check (rating_m between 2 and 100),
      |  constraint rating_contests_participant_count_check check (participant_count >= 2),
      |  constraint rating_contests_time_range_check check (contest_end_at > contest_start_at)
      |);
      |""".stripMargin

  val initUserStateTableSql: String =
    """
      |create table if not exists rating_user_states (
      |  username varchar(120) primary key references auth_accounts(username) on delete cascade,
      |  particles_json text not null,
      |  current_rating numeric not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  /** 执行评分比赛表和用户评分状态表的幂等初始化 SQL。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initContestTableSql)
        statement.execute(initUserStateTableSql)
      finally statement.close()
    }
