package domains.submission.http.utils

import domains.submission.application.input.*
import munit.FunSuite
import shared.model.PageRequest

class SubmissionListRequestQuerySupportSuite extends FunSuite:

  test("parseListRequest uses default list request when query parameters are absent") {
    val result = SubmissionListRequestQuerySupport.parseListRequest(Map.empty)

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

  test("parseListRequest accepts valid page, sort, direction, verdict, and text filters") {
    val result = SubmissionListRequestQuerySupport.parseListRequest(
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

  test("parseListRequest falls back to default pagination for invalid, zero, and negative values") {
    val cases = List(
      Map("page" -> "abc", "pageSize" -> "ten"),
      Map("page" -> "0", "pageSize" -> "0"),
      Map("page" -> "-2", "pageSize" -> "-20")
    )

    cases.foreach { queryParams =>
      val result = SubmissionListRequestQuerySupport.parseListRequest(queryParams)

      assertEquals(result.pageRequest, PageRequest(page = 1, pageSize = 10))
    }
  }

  test("parseListRequest falls back to default sort, direction, and verdict values when invalid") {
    val result = SubmissionListRequestQuerySupport.parseListRequest(
      Map("sort" -> "bad", "direction" -> "sideways", "verdict" -> "mystery")
    )

    assertEquals(result.sort, SubmissionSort.Submitted)
    assertEquals(result.direction, SubmissionSortDirection.Desc)
    assertEquals(result.verdict, SubmissionVerdictFilter.All)
  }

  test("parseListRequest derives default direction from resolved sort when direction is absent or invalid") {
    val absentDirection = SubmissionListRequestQuerySupport.parseListRequest(Map("sort" -> "time"))
    val invalidDirection = SubmissionListRequestQuerySupport.parseListRequest(
      Map("sort" -> "memory", "direction" -> "bad")
    )

    assertEquals(absentDirection.sort, SubmissionSort.Time)
    assertEquals(absentDirection.direction, SubmissionSortDirection.Asc)
    assertEquals(invalidDirection.sort, SubmissionSort.Memory)
    assertEquals(invalidDirection.direction, SubmissionSortDirection.Asc)
  }
