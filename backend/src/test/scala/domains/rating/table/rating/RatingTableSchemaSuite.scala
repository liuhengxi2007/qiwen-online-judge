package domains.rating.table.rating

import munit.FunSuite

class RatingTableSchemaSuite extends FunSuite:

  test("rating contest schema allows repeated contest slugs") {
    assert(RatingTableSchema.initContestTableSql.contains("contest_slug varchar(64) not null"))
    assert(!RatingTableSchema.initContestTableSql.contains("contest_slug varchar(64) not null unique"))
    assert(!RatingTableSchema.initContestTableSql.contains("unique (contest_slug)"))
  }

  test("rating contest schema constrains m and participant count") {
    assert(RatingTableSchema.initContestTableSql.contains("constraint rating_contests_m_check check (rating_m between 2 and 100)"))
    assert(RatingTableSchema.initContestTableSql.contains("constraint rating_contests_participant_count_check check (participant_count >= 2)"))
  }
