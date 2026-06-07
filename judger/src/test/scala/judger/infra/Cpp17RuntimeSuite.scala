package judger.infra

import judgeprotocol.objects.SubmissionSourceCode
import munit.FunSuite

import java.nio.file.Files
import scala.jdk.CollectionConverters.*

class Cpp17RuntimeSuite extends FunSuite:

  test("compile args include contestant and stub sources in one command") {
    val args = Cpp17Runtime.compileArgs("main-main.cpp", Some("stub-main.cpp"), "main-main")

    assertEquals(args, List("main-main.cpp", "stub-main.cpp", "-o", "main-main", "-O2", "-std=c++17", "-I", "."))
  }

  test("compile args omit stub source when absent") {
    val args = Cpp17Runtime.compileArgs("main-main.cpp", None, "main-main")

    assertEquals(args, List("main-main.cpp", "-o", "main-main", "-O2", "-std=c++17", "-I", "."))
  }

  test("writes program headers by include filename") {
    val root = Files.createTempDirectory("cpp17-runtime-suite")
    try
      Cpp17Runtime.writeHeaders(root, List(ProgramHeaderSource("xxx.h", SubmissionSourceCode("#define VALUE 42\n"))))

      assertEquals(Files.readString(root.resolve("xxx.h")), "#define VALUE 42\n")
      assert(!Files.exists(root.resolve("headers").resolve("xxx.h")))
    finally deleteRecursively(root)
  }

  private def deleteRecursively(root: java.nio.file.Path): Unit =
    if Files.exists(root) then
      Files.walk(root).iterator().asScala.toList.reverse.foreach(path => Files.deleteIfExists(path))
