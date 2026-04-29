package domains.problem.application

import domains.problem.model.{CreateProblemRequest, OthersSubmissionAccess, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle, UpdateProblemRequest}
import domains.shared.access.{BaseAccess, ResourceAccessPolicy}
import munit.FunSuite

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
