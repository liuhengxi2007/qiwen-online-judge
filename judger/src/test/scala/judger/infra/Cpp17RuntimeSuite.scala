package judger.infra

import munit.FunSuite

class Cpp17RuntimeSuite extends FunSuite:

  test("compile args include contestant and stub sources in one command") {
    val args = Cpp17Runtime.compileArgs("main-main.cpp", Some("stub-main.cpp"), "main-main")

    assertEquals(args, List("main-main.cpp", "stub-main.cpp", "-o", "main-main", "-O2", "-std=c++17"))
  }

  test("compile args omit stub source when absent") {
    val args = Cpp17Runtime.compileArgs("main-main.cpp", None, "main-main")

    assertEquals(args, List("main-main.cpp", "-o", "main-main", "-O2", "-std=c++17"))
  }
