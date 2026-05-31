package domains.problemset.objects.request

import domains.user.objects.Username
import io.circe.parser.decode
import munit.FunSuite

class UpdateProblemSetRequestCodecSuite extends FunSuite:

  private val baseJson =
    """
      |{
      |  "title": "Sample Set",
      |  "description": "Practice set.",
      |  "accessPolicy": {
      |    "baseAccess": "restricted",
      |    "viewerGrants": [],
      |    "managerGrants": []
      |  },
      |  "authorUsername": null
      |}
      |""".stripMargin

  test("decodes null problem set author username as no author") {
    val result = decode[UpdateProblemSetRequest](baseJson)

    assertEquals(result.map(_.authorUsername), Right(None))
  }

  test("decodes and normalizes problem set author username") {
    val result = decode[UpdateProblemSetRequest](baseJson.replace("null", "\" Alice_01 \""))

    assertEquals(result.map(_.authorUsername), Right(Some(Username("alice_01"))))
  }

  test("rejects invalid problem set author username") {
    val result = decode[UpdateProblemSetRequest](baseJson.replace("null", "\"ab\""))

    assert(result.isLeft)
  }
