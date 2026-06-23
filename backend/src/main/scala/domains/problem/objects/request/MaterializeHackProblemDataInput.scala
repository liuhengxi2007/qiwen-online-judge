package domains.problem.objects.request

import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

/** 成功 hack 物化为题目数据的内部输入；携带目标题目、子任务、生成文件路径和待写入文本。 */
final case class MaterializeHackProblemDataInput(
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  subtaskIndex: Int,
  inputPath: ProblemDataPath,
  answerPath: Option[ProblemDataPath],
  testcaseLabel: String,
  inputText: String,
  answerText: Option[String],
  createdAt: Instant
)

/** MaterializeHackProblemDataInput 的 JSON 编解码器，Instant 以 ISO-8601 字符串表示。 */
object MaterializeHackProblemDataInput:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[MaterializeHackProblemDataInput] = deriveEncoder[MaterializeHackProblemDataInput]
  given Decoder[MaterializeHackProblemDataInput] = deriveDecoder[MaterializeHackProblemDataInput]
