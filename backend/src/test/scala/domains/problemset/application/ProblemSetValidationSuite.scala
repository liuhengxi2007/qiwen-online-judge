package domains.problemset.application

import domains.problem.model.ProblemSlug
import domains.problemset.model.{ProblemSetDescription, ProblemSetSlug, ProblemSetTitle}
import domains.problemset.model.request.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import munit.FunSuite
import shared.model.access.{BaseAccess, ResourceAccessPolicy}

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

  test("validateAddProblem trims valid problem slugs") {
    val request = AddProblemToProblemSetRequest(ProblemSlug(" sample-problem "))

    val result = ProblemSetValidation.validateAddProblem(request)

    assertEquals(result, Right(request.copy(problemSlug = ProblemSlug("sample-problem"))))
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

  test("validateCreate rejects blank titles") {
    val request = CreateProblemSetRequest(
      slug = ProblemSetSlug("sample-set"),
      title = ProblemSetTitle("   "),
      description = ProblemSetDescription("Description"),
      accessPolicy = accessPolicy
    )

    val result = ProblemSetValidation.validateCreate(request)

    assertEquals(result, Left("Problem set title is required."))
  }

  test("validateUpdate accepts empty descriptions when the parser allows them") {
    val request = UpdateProblemSetRequest(
      title = ProblemSetTitle("Title"),
      description = ProblemSetDescription("   "),
      accessPolicy = accessPolicy
    )

    val result = ProblemSetValidation.validateUpdate(request)

    assertEquals(result, Right(request.copy(description = ProblemSetDescription(""))))
  }
