package shared.api.utils

import munit.FunSuite
import shared.objects.PageRequest

class PageRequestQuerySupportSuite extends FunSuite:

  test("parsePageRequest uses default values when query parameters are absent") {
    val result = PageRequestQuerySupport.parsePageRequest(Map.empty)

    assertEquals(result, Right(PageRequest(page = 1, pageSize = 10)))
  }

  test("parsePageRequest accepts positive integer values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "3", "pageSize" -> "50"))

    assertEquals(result, Right(PageRequest(page = 3, pageSize = 50)))
  }

  test("parsePageRequest rejects non-integer values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "abc", "pageSize" -> "ten"))

    assertEquals(result, Left("page must be a positive integer."))
  }

  test("parsePageRequest rejects zero values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "0", "pageSize" -> "0"))

    assertEquals(result, Left("page must be a positive integer."))
  }

  test("parsePageRequest rejects negative values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "-2", "pageSize" -> "-20"))

    assertEquals(result, Left("page must be a positive integer."))
  }
