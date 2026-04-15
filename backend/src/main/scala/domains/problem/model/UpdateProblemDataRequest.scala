package domains.problem.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.Base64
import scala.util.Try

final case class UpdateProblemDataRequest(
  filename: ProblemDataFilename,
  contentBase64: String
)

object UpdateProblemDataRequest:
  given Encoder[UpdateProblemDataRequest] = deriveEncoder[UpdateProblemDataRequest]
  given Decoder[UpdateProblemDataRequest] = deriveDecoder[UpdateProblemDataRequest]

  extension (request: UpdateProblemDataRequest)
    def decodedBytes: Either[String, Array[Byte]] =
      Try(Base64.getDecoder.decode(request.contentBase64))
        .toEither
        .left
        .map(_ => "Problem data content is not valid base64.")
