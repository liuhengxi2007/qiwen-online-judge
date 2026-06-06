package domains.submission.objects.request

import munit.FunSuite

class SubmissionVerdictFilterSuite extends FunSuite:

  test("parse accepts every supported value") {
    val cases = List(
      "all" -> SubmissionVerdictFilter.All,
      "pending" -> SubmissionVerdictFilter.Pending,
      "accepted" -> SubmissionVerdictFilter.Accepted,
      "accepted_by_protocol" -> SubmissionVerdictFilter.AcceptedByProtocol,
      "wrong_answer" -> SubmissionVerdictFilter.WrongAnswer,
      "compile_error" -> SubmissionVerdictFilter.CompileError,
      "runtime_error" -> SubmissionVerdictFilter.RuntimeError,
      "time_limit_exceeded" -> SubmissionVerdictFilter.TimeLimitExceeded,
      "idleness_limit_exceeded" -> SubmissionVerdictFilter.IdlenessLimitExceeded,
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
        "Submission verdict filter must be one of: all, pending, accepted, accepted_by_protocol, wrong_answer, compile_error, runtime_error, time_limit_exceeded, idleness_limit_exceeded, system_error."
      )
    )
  }

  test("encode round-trips representative values") {
    val cases = List(
      SubmissionVerdictFilter.All -> "all",
      SubmissionVerdictFilter.Accepted -> "accepted",
      SubmissionVerdictFilter.AcceptedByProtocol -> "accepted_by_protocol",
      SubmissionVerdictFilter.TimeLimitExceeded -> "time_limit_exceeded",
      SubmissionVerdictFilter.IdlenessLimitExceeded -> "idleness_limit_exceeded"
    )

    cases.foreach { (value, expected) =>
      assertEquals(SubmissionVerdictFilter.encode(value), expected)
    }
  }
