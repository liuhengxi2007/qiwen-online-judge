package domains.submission.model

import munit.FunSuite

class SubmissionVerdictFilterSuite extends FunSuite:

  test("parse accepts every supported value") {
    val cases = List(
      "all" -> SubmissionVerdictFilter.All,
      "pending" -> SubmissionVerdictFilter.Pending,
      "accepted" -> SubmissionVerdictFilter.Accepted,
      "wrong_answer" -> SubmissionVerdictFilter.WrongAnswer,
      "compile_error" -> SubmissionVerdictFilter.CompileError,
      "runtime_error" -> SubmissionVerdictFilter.RuntimeError,
      "time_limit_exceeded" -> SubmissionVerdictFilter.TimeLimitExceeded,
      "system_error" -> SubmissionVerdictFilter.SystemError
    )

    cases.foreach { (raw, expected) =>
      assertEquals(SubmissionVerdictFilter.parse(s" $raw "), Right(expected))
    }
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

  test("toDatabase round-trips representative values") {
    val cases = List(
      SubmissionVerdictFilter.All -> "all",
      SubmissionVerdictFilter.Accepted -> "accepted",
      SubmissionVerdictFilter.TimeLimitExceeded -> "time_limit_exceeded"
    )

    cases.foreach { (value, expected) =>
      assertEquals(SubmissionVerdictFilter.toDatabase(value), expected)
    }
  }
