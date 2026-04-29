package domains.problemset.application

import domains.problem.model.ProblemSlug
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetDescription, ProblemSetSlug, ProblemSetTitle, UpdateProblemSetRequest}
import domains.shared.access.{BaseAccess, ResourceAccessPolicy}
import munit.FunSuite

class ProblemSetValidationSuite extends FunSuite:

  private val accessPolicy = ResourceAccessPolicy(BaseAccess.Public, Nil, Nil)

  test("validateCreate trims slug title and description") {
    val request = CreateProblemSetRequest(
      slug = ProblemSetSlug(" sample-set "),
      title = ProblemSetTitle(" Sample Set "),
      description = ProblemSetDescription(" Description "),
      accessPolicy = accessPolicy
    )

    val result = ProblemSetValidation.validateCreate(request)

    assertEquals(
      result,
      Right(
        request.copy(
          slug = ProblemSetSlug("sample-set"),
          title = ProblemSetTitle("Sample Set"),
          description = ProblemSetDescription("Description")
        )
      )
    )
  }

  test("validateAddProblem rejects invalid problem slugs") {
    val request = AddProblemToProblemSetRequest(ProblemSlug("bad slug"))

    val result = ProblemSetValidation.validateAddProblem(request)

    assertEquals(result, Left("Problem slug may contain only lowercase letters, numbers, and hyphens."))
  }

  test("validateUpdate rejects overlong descriptions") {
    val request = UpdateProblemSetRequest(
      title = ProblemSetTitle("Title"),
      description = ProblemSetDescription("x" * 2001),
      accessPolicy = accessPolicy
    )

    val result = ProblemSetValidation.validateUpdate(request)

    assertEquals(result, Left("Problem set description must be at most 2000 characters."))
  }
