package domains.submission.api

import cats.effect.IO
import cats.syntax.all.*
import domains.submission.objects.request.*
import munit.CatsEffectSuite
import org.http4s.{Request, Uri}
import shared.api.PathParams
import shared.objects.PageRequest

class ListSubmissionsDecodeSuite extends CatsEffectSuite:

  test("decode uses default list request when query parameters are absent") {
    decode("/api/submissions").map { result =>
      assertEquals(
        result,
        SubmissionListRequest(
          userQuery = None,
          problemQuery = None,
          verdict = SubmissionVerdictFilter.All,
          sort = SubmissionSort.Submitted,
          direction = SubmissionSortDirection.Desc,
          pageRequest = PageRequest(page = 1, pageSize = 10)
        )
      )
    }
  }

  test("decode accepts valid page, sort, direction, verdict, and text filters") {
    decode("/api/submissions?page=3&pageSize=50&sort=time&direction=desc&verdict=accepted&username=%20alice%20&problem=%20p100%20").map {
      result =>
        assertEquals(
          result,
          SubmissionListRequest(
            userQuery = Some(SubmissionUserQuery("alice")),
            problemQuery = Some(SubmissionProblemQuery("p100")),
            verdict = SubmissionVerdictFilter.Accepted,
            sort = SubmissionSort.Time,
            direction = SubmissionSortDirection.Desc,
            pageRequest = PageRequest(page = 3, pageSize = 50)
          )
        )
    }
  }

  test("decode falls back to default pagination for invalid, zero, and negative values") {
    val cases = List(
      "/api/submissions?page=abc&pageSize=ten",
      "/api/submissions?page=0&pageSize=0",
      "/api/submissions?page=-2&pageSize=-20"
    )

    cases.traverse(decode).map { results =>
      results.foreach(result => assertEquals(result.pageRequest, PageRequest(page = 1, pageSize = 10)))
    }
  }

  test("decode falls back to default sort, direction, and verdict values when invalid") {
    decode("/api/submissions?sort=bad&direction=sideways&verdict=mystery").map { result =>
      assertEquals(result.sort, SubmissionSort.Submitted)
      assertEquals(result.direction, SubmissionSortDirection.Desc)
      assertEquals(result.verdict, SubmissionVerdictFilter.All)
    }
  }

  test("decode derives default direction from resolved sort when direction is absent or invalid") {
    (decode("/api/submissions?sort=time"), decode("/api/submissions?sort=memory&direction=bad")).tupled.map {
      case (absentDirection, invalidDirection) =>
        assertEquals(absentDirection.sort, SubmissionSort.Time)
        assertEquals(absentDirection.direction, SubmissionSortDirection.Asc)
        assertEquals(invalidDirection.sort, SubmissionSort.Memory)
        assertEquals(invalidDirection.direction, SubmissionSortDirection.Asc)
    }
  }

  private def decode(uri: String): IO[SubmissionListRequest] =
    ListSubmissions.decode(Request[IO](uri = parsedUri(uri)), PathParams(Map.empty))

  private def parsedUri(value: String): Uri =
    Uri.fromString(value).fold(error => fail(error.toString), identity)
