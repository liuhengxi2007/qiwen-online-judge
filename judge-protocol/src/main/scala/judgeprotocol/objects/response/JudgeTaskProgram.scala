package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}

final case class JudgeTaskProgram(
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  stub: Option[JudgeTaskFileRef] = None,
  headers: List[JudgeTaskFileRef] = Nil
)

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
