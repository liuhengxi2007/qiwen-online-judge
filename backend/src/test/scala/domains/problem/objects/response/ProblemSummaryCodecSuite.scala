package domains.problem.objects.response

import domains.problem.objects.{OtherUserSubmissionAccess, ProblemData, ProblemId, ProblemSlug, ProblemTitle}
import io.circe.syntax.*
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.time.Instant
import java.util.UUID

class ProblemSummaryCodecSuite extends FunSuite:

  test("encodes nullable problem author field") {
    val summary = ProblemSummary(
      id = ProblemId(UUID.fromString("11111111-1111-4111-8111-111111111111")),
      slug = ProblemSlug("sample-problem"),
      title = ProblemTitle("Sample Problem"),
      data = ProblemData(None),
      ready = false,
      accessPolicy = ResourceAccessPolicy(BaseAccess.Restricted, Nil, Nil),
      otherUserSubmissionAccess = OtherUserSubmissionAccess.None,
      author = None,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH
    )

    val json = summary.asJson.noSpaces

    assert(json.contains("\"author\":null"))
    assert(!json.contains("creator"))
  }
