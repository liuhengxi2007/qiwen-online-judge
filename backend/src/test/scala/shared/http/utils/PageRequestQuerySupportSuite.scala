package shared.http.utils

import munit.FunSuite
import shared.objects.PageRequest

class PageRequestQuerySupportSuite extends FunSuite:

  test("parsePageRequest uses default values when query parameters are absent") {
    val result = PageRequestQuerySupport.parsePageRequest(Map.empty)

    assertEquals(result, PageRequest(page = 1, pageSize = 10))
  }

  test("parsePageRequest accepts positive integer values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "3", "pageSize" -> "50"))

    assertEquals(result, PageRequest(page = 3, pageSize = 50))
  }

  test("parsePageRequest falls back to defaults for non-integer values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "abc", "pageSize" -> "ten"))

    assertEquals(result, PageRequest(page = 1, pageSize = 10))
  }

  test("parsePageRequest falls back to defaults for zero values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "0", "pageSize" -> "0"))

    assertEquals(result, PageRequest(page = 1, pageSize = 10))
  }

  test("parsePageRequest falls back to defaults for negative values") {
    val result = PageRequestQuerySupport.parsePageRequest(Map("page" -> "-2", "pageSize" -> "-20"))

    assertEquals(result, PageRequest(page = 1, pageSize = 10))
  }
