package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}

/** 一个 role 对应的提交程序，可能包含 stub 和头文件依赖。 */
final case class JudgeTaskProgram(
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  stub: Option[JudgeTaskFileRef] = None,
  headers: List[JudgeTaskFileRef] = Nil
)

/** 负责程序协议编解码，并兼容旧 payload 中缺失 headers 的情况。 */
object JudgeTaskProgram:
  given Encoder[JudgeTaskProgram] = deriveEncoder[JudgeTaskProgram]
  given Decoder[JudgeTaskProgram] = Decoder.instance { cursor =>
    for
      language <- cursor.get[SubmissionLanguage]("language")
      sourceCode <- cursor.get[SubmissionSourceCode]("sourceCode")
      stub <- cursor.get[Option[JudgeTaskFileRef]]("stub")
      headers <- cursor.get[Option[List[JudgeTaskFileRef]]]("headers").map(_.getOrElse(Nil))
    yield JudgeTaskProgram(language, sourceCode, stub, headers)
  }
