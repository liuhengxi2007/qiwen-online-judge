package domains.problem.api

import domains.problem.objects.ProblemDataPath
import munit.FunSuite
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

class MaterializeHackProblemDataSuite extends FunSuite:

  test("appendHackTestcaseToJudgeYaml adds hack testcase to selected subtask") {
    val updatedBytes = MaterializeHackProblemData.appendHackTestcaseToJudgeYaml(
      judgeYamlBytes = yaml("""
        |version: 2
        |hack: false
        |limits:
        |  timeMs: 1000
        |  memoryMb: 256
        |checker:
        |  type: builtin
        |  name: exact
        |aggregation:
        |  testcases: sum_max_max
        |subtasks:
        |  - label: first
        |    testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |  - label: second
        |    testcases:
        |      - input: sample/2.in
        |        answer: sample/2.ans
        |"""),
      subtaskIndex = 2,
      testcaseLabel = "hack #7",
      inputPath = ProblemDataPath("hacks/7.in"),
      answerPath = Some(ProblemDataPath("hacks/7.ans"))
    )

    val secondSubtask = subtasks(updatedBytes)(1)
    val testcases = secondSubtask("testcases").asInstanceOf[java.util.List[?]].asScala.toList
    val hackTestcase = testcases.last.asInstanceOf[java.util.Map[String, Any]].asScala.toMap

    assertEquals(hackTestcase("label"), "hack #7")
    assertEquals(hackTestcase("type"), "hack")
    assertEquals(hackTestcase("input"), "hacks/7.in")
    assertEquals(hackTestcase("answer"), "hacks/7.ans")
  }

  test("appendHackTestcaseToJudgeYaml omits answer when hack has no answer") {
    val updatedBytes = MaterializeHackProblemData.appendHackTestcaseToJudgeYaml(
      judgeYamlBytes = yaml("""
        |version: 2
        |hack: false
        |subtasks:
        |  - testcases:
        |      - input: sample/1.in
        |        answer: sample/1.ans
        |"""),
      subtaskIndex = 1,
      testcaseLabel = "hack #8",
      inputPath = ProblemDataPath("hacks/8.in"),
      answerPath = None
    )

    val firstSubtask = subtasks(updatedBytes).head
    val testcases = firstSubtask("testcases").asInstanceOf[java.util.List[?]].asScala.toList
    val hackTestcase = testcases.last.asInstanceOf[java.util.Map[String, Any]].asScala.toMap

    assertEquals(hackTestcase("type"), "hack")
    assertEquals(hackTestcase("input"), "hacks/8.in")
    assert(!hackTestcase.contains("answer"))
  }

  private def subtasks(bytes: Array[Byte]): List[Map[String, Any]] =
    val settings = LoadSettings.builder().setLabel("judge.yaml").build()
    val root = Load(settings)
      .loadFromString(new String(bytes, StandardCharsets.UTF_8))
      .asInstanceOf[java.util.Map[String, Any]]
      .asScala
      .toMap
    root("subtasks").asInstanceOf[java.util.List[?]].asScala.toList.map(_.asInstanceOf[java.util.Map[String, Any]].asScala.toMap)

  private def yaml(content: String): Array[Byte] =
    content.stripMargin.trim.nn.getBytes(StandardCharsets.UTF_8)
