package domains.submission.application

import domains.problem.model.ProblemSlug
import domains.submission.model.{CreateSubmissionRequest, SubmissionLanguage, SubmissionSourceCode}
import munit.FunSuite

class SubmissionValidationSuite extends FunSuite:

  test("validateCreate accepts valid problem slugs and source code") {
    val request = CreateSubmissionRequest(
      problemSlug = ProblemSlug("sample-problem"),
      language = SubmissionLanguage.Cpp17,
      sourceCode = SubmissionSourceCode("int main() { return 0; }")
    )

    val result = SubmissionValidation.validateCreate(request)

    assertEquals(result, Right(request))
  }

  test("validateCreate rejects invalid slugs") {
    val request = CreateSubmissionRequest(
      problemSlug = ProblemSlug("bad slug"),
      language = SubmissionLanguage.Python3,
      sourceCode = SubmissionSourceCode("print(1)")
    )

    val result = SubmissionValidation.validateCreate(request)

    assertEquals(result, Left("Problem slug may contain only lowercase letters, numbers, and hyphens."))
  }

  test("validateCreate rejects blank source code") {
    val request = CreateSubmissionRequest(
      problemSlug = ProblemSlug("sample-problem"),
      language = SubmissionLanguage.Python3,
      sourceCode = SubmissionSourceCode("   ")
    )

    val result = SubmissionValidation.validateCreate(request)

    assertEquals(result, Left("Source code is required."))
  }
