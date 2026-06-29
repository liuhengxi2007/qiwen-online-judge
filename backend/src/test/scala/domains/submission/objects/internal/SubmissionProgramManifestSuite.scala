package domains.submission.objects.internal

import domains.submission.objects.{SubmissionLanguage, SubmissionSourceCode}
import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

import java.util.UUID

class SubmissionProgramManifestSuite extends FunSuite:

  test("singleDefault uses deterministic main source key and source metadata") {
    val submissionUuid = UUID.fromString("11111111-1111-4111-8111-111111111111")
    val manifest = SubmissionProgramManifest.singleDefault(
      submissionUuid,
      SubmissionLanguage.Cpp17,
      SubmissionSourceCode("int main() {}\n")
    )

    val program = manifest.defaultProgram.toOption.get
    assertEquals(manifest.defaultProgramKey, "main")
    assertEquals(program.language, SubmissionLanguage.Cpp17)
    assertEquals(program.sourceKey, "submissions/11111111-1111-4111-8111-111111111111/programs/main/source")
    assertEquals(program.sizeBytes, 14L)
    assertEquals(program.sha256.length, 64)
  }

  test("manifest JSON round-trips with the reserved default program shape") {
    val manifest = SubmissionProgramManifest.singleDefault(
      UUID.fromString("22222222-2222-4222-8222-222222222222"),
      SubmissionLanguage.Python3,
      SubmissionSourceCode("print(1)\n")
    )

    assertEquals(decode[SubmissionProgramManifest](manifest.asJson.noSpaces), Right(manifest))
  }

  test("fromPrograms accepts text roles with txt suffix") {
    val manifest = SubmissionProgramManifest.fromPrograms(
      UUID.fromString("33333333-3333-4333-8333-333333333333"),
      Map(
        "chain.txt" -> (SubmissionLanguage.Text -> SubmissionSourceCode("42\n")),
        "main" -> (SubmissionLanguage.Cpp17 -> SubmissionSourceCode("int main() {}\n"))
      )
    )

    assertEquals(manifest.map(_.programs("chain.txt").language), Right(SubmissionLanguage.Text))
    assertEquals(manifest.map(_.programs("chain.txt").sizeBytes), Right(3L))
  }

  test("fromPrograms rejects invalid text role and language combinations") {
    val submissionUuid = UUID.fromString("44444444-4444-4444-8444-444444444444")

    assert(SubmissionProgramManifest.fromPrograms(submissionUuid, Map("main" -> (SubmissionLanguage.Text -> SubmissionSourceCode("42\n")))).isLeft)
    assert(SubmissionProgramManifest.fromPrograms(submissionUuid, Map("chain.txt" -> (SubmissionLanguage.Cpp17 -> SubmissionSourceCode("int main() {}\n")))).isLeft)
    assert(SubmissionProgramManifest.fromPrograms(submissionUuid, Map("a.b.txt" -> (SubmissionLanguage.Text -> SubmissionSourceCode("42\n")))).isLeft)
    assert(SubmissionProgramManifest.fromPrograms(submissionUuid, Map("dir/chain.txt" -> (SubmissionLanguage.Text -> SubmissionSourceCode("42\n")))).isLeft)
  }
