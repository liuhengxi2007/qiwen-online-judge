package domains.submission.http.mapper

import domains.submission.model.request.*
import munit.FunSuite
import shared.model.PageRequest

class SubmissionHttpRequestMappersSuite extends FunSuite:

  test("listSubmissionsRequest uses default list request when query parameters are absent") {
    val result = SubmissionHttpRequestMappers.listSubmissionsRequest(Map.empty)

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

  test("listSubmissionsRequest accepts valid page, sort, direction, verdict, and text filters") {
    val result = SubmissionHttpRequestMappers.listSubmissionsRequest(
      Map(
        "page" -> "3",
        "pageSize" -> "50",
        "sort" -> "time",
        "direction" -> "desc",
        "verdict" -> "accepted",
        "username" -> " alice ",
        "problem" -> " p100 "
      )
    )

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

  test("listSubmissionsRequest falls back to default pagination for invalid, zero, and negative values") {
    val cases = List(
      Map("page" -> "abc", "pageSize" -> "ten"),
      Map("page" -> "0", "pageSize" -> "0"),
      Map("page" -> "-2", "pageSize" -> "-20")
    )

    cases.foreach { queryParams =>
      val result = SubmissionHttpRequestMappers.listSubmissionsRequest(queryParams)

      assertEquals(result.pageRequest, PageRequest(page = 1, pageSize = 10))
    }
  }

  test("listSubmissionsRequest falls back to default sort, direction, and verdict values when invalid") {
    val result = SubmissionHttpRequestMappers.listSubmissionsRequest(
      Map("sort" -> "bad", "direction" -> "sideways", "verdict" -> "mystery")
    )

    assertEquals(result.sort, SubmissionSort.Submitted)
    assertEquals(result.direction, SubmissionSortDirection.Desc)
    assertEquals(result.verdict, SubmissionVerdictFilter.All)
  }

  test("listSubmissionsRequest derives default direction from resolved sort when direction is absent or invalid") {
    val absentDirection = SubmissionHttpRequestMappers.listSubmissionsRequest(Map("sort" -> "time"))
    val invalidDirection = SubmissionHttpRequestMappers.listSubmissionsRequest(
      Map("sort" -> "memory", "direction" -> "bad")
    )

    assertEquals(absentDirection.sort, SubmissionSort.Time)
    assertEquals(absentDirection.direction, SubmissionSortDirection.Asc)
    assertEquals(invalidDirection.sort, SubmissionSort.Memory)
    assertEquals(invalidDirection.direction, SubmissionSortDirection.Asc)
  }
