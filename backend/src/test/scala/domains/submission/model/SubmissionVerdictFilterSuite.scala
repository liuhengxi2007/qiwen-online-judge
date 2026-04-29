package domains.submission.model

import munit.FunSuite

class SubmissionVerdictFilterSuite extends FunSuite:

  test("parse accepts supported values after trimming") {
    val result = SubmissionVerdictFilter.parse(" accepted ")

    assertEquals(result, Right(SubmissionVerdictFilter.Accepted))
  }

  test("parse rejects unsupported values") {
    val result = SubmissionVerdictFilter.parse("mystery")

    assertEquals(
      result,
      Left(
        "Submission verdict filter must be one of: all, pending, accepted, wrong_answer, compile_error, runtime_error, time_limit_exceeded, system_error."
      )
    )
  }

  test("toDatabase matches the expected wire value") {
    assertEquals(SubmissionVerdictFilter.toDatabase(SubmissionVerdictFilter.TimeLimitExceeded), "time_limit_exceeded")
  }
