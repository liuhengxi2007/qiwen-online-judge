package domains.problem.objects.request

import domains.user.objects.Username
import io.circe.parser.decode
import munit.FunSuite

class UpdateProblemRequestCodecSuite extends FunSuite:

  private val baseJson =
    """
      |{
      |  "title": "Sample Problem",
      |  "statement": "Solve it.",
      |  "accessPolicy": {
      |    "baseAccess": "restricted",
      |    "viewerGrants": [],
      |    "managerGrants": []
      |  },
      |  "otherUserSubmissionAccess": "none",
      |  "authorUsername": null
      |}
      |""".stripMargin

  test("decodes null problem author username as no author") {
    val result = decode[UpdateProblemRequest](baseJson)

    assertEquals(result.map(_.authorUsername), Right(None))
  }

  test("decodes and normalizes problem author username") {
    val result = decode[UpdateProblemRequest](baseJson.replace("null", "\" Alice_01 \""))

    assertEquals(result.map(_.authorUsername), Right(Some(Username("alice_01"))))
  }

  test("rejects invalid problem author username") {
    val result = decode[UpdateProblemRequest](baseJson.replace("null", "\"ab\""))

    assert(result.isLeft)
  }
