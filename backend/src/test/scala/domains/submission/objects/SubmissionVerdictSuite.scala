package domains.submission.objects

import io.circe.syntax.*
import munit.FunSuite

class SubmissionVerdictSuite extends FunSuite:

  test("parse and encode idleness limit exceeded") {
    val parsed = SubmissionVerdict.parse(" idleness_limit_exceeded ")

    assertEquals(parsed, Right(SubmissionVerdict.IdlenessLimitExceeded))
    assertEquals(parsed.map(_.asJson.noSpaces), Right("\"idleness_limit_exceeded\""))
  }
