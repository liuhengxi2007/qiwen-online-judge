package domains.problemset.objects.response

import domains.problemset.objects.{ProblemSetDescription, ProblemSetId, ProblemSetSlug, ProblemSetTitle}
import io.circe.syntax.*
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceVisibilityPolicy}

import java.time.Instant
import java.util.UUID

class ProblemSetSummaryCodecSuite extends FunSuite:

  test("encodes nullable problem set author field") {
    val summary = ProblemSetSummary(
      id = ProblemSetId(UUID.fromString("11111111-1111-4111-8111-111111111111")),
      slug = ProblemSetSlug("sample-set"),
      title = ProblemSetTitle("Sample Set"),
      description = ProblemSetDescription("Practice set."),
      accessPolicy = ResourceVisibilityPolicy(BaseAccess.Restricted, Nil),
      author = None,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH
    )

    val json = summary.asJson.noSpaces

    assert(json.contains("\"author\":null"))
    assert(!json.contains("creator"))
  }
