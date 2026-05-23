package domains.problem.application

import domains.problem.application.input.{CreateProblemRequest, UpdateProblemRequest}
import domains.problem.model.{OthersSubmissionAccess, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}
import munit.FunSuite
import shared.access.{BaseAccess, ResourceAccessPolicy}

class ProblemValidationSuite extends FunSuite:

  private val accessPolicy = ResourceAccessPolicy(BaseAccess.OwnerOnly, Nil, Nil)

  test("validateCreate normalizes fields through the parsers") {
    val request = CreateProblemRequest(
      slug = ProblemSlug(" sample-problem "),
      title = ProblemTitle(" Sample Title "),
      statement = ProblemStatementText(" Example statement "),
      timeLimitMs = ProblemTimeLimitMs(1000),
      spaceLimitMb = ProblemSpaceLimitMb(256),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.Summary
    )

    val result = ProblemValidation.validateCreate(request)

    assertEquals(
      result,
      Right(
        request.copy(
          slug = ProblemSlug("sample-problem"),
          title = ProblemTitle("Sample Title"),
          statement = ProblemStatementText("Example statement")
        )
      )
    )
  }

  test("validateCreate rejects invalid slugs") {
    val request = CreateProblemRequest(
      slug = ProblemSlug("bad slug"),
      title = ProblemTitle("Title"),
      statement = ProblemStatementText("Statement"),
      timeLimitMs = ProblemTimeLimitMs(1000),
      spaceLimitMb = ProblemSpaceLimitMb(256),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.None
    )

    val result = ProblemValidation.validateCreate(request)

    assertEquals(result, Left("Problem slug may contain only lowercase letters, numbers, and hyphens."))
  }

  test("validateCreate rejects blank titles before returning the request") {
    val request = CreateProblemRequest(
      slug = ProblemSlug("sample-problem"),
      title = ProblemTitle("   "),
      statement = ProblemStatementText("Statement"),
      timeLimitMs = ProblemTimeLimitMs(1000),
      spaceLimitMb = ProblemSpaceLimitMb(256),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.None
    )

    val result = ProblemValidation.validateCreate(request)

    assertEquals(result, Left("Problem title is required."))
  }

  test("validateUpdate rejects overlong statements") {
    val request = UpdateProblemRequest(
      title = ProblemTitle("Title"),
      statement = ProblemStatementText("x" * 20001),
      timeLimitMs = ProblemTimeLimitMs(1000),
      spaceLimitMb = ProblemSpaceLimitMb(256),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.Detail
    )

    val result = ProblemValidation.validateUpdate(request)

    assertEquals(result, Left("Problem statement must be at most 20000 characters."))
  }

  test("validateUpdate rejects too-small time limits") {
    val request = UpdateProblemRequest(
      title = ProblemTitle("Title"),
      statement = ProblemStatementText("Statement"),
      timeLimitMs = ProblemTimeLimitMs(0),
      spaceLimitMb = ProblemSpaceLimitMb(256),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.Detail
    )

    val result = ProblemValidation.validateUpdate(request)

    assertEquals(result, Left("Problem time limit must be at least 1 ms."))
  }

  test("validateUpdate rejects too-large space limits") {
    val request = UpdateProblemRequest(
      title = ProblemTitle("Title"),
      statement = ProblemStatementText("Statement"),
      timeLimitMs = ProblemTimeLimitMs(1000),
      spaceLimitMb = ProblemSpaceLimitMb(70000),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.Detail
    )

    val result = ProblemValidation.validateUpdate(request)

    assertEquals(result, Left("Problem space limit must be at most 65536 MB."))
  }

  test("validateUpdate accepts exact numeric lower and upper bounds") {
    val request = UpdateProblemRequest(
      title = ProblemTitle("Title"),
      statement = ProblemStatementText("Statement"),
      timeLimitMs = ProblemTimeLimitMs(1),
      spaceLimitMb = ProblemSpaceLimitMb(65536),
      accessPolicy = accessPolicy,
      othersSubmissionAccess = OthersSubmissionAccess.Detail
    )

    val result = ProblemValidation.validateUpdate(request)

    assertEquals(result, Right(request))
  }
