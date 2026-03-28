package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SaveDemoNoteRequest(
  title: NoteTitle,
  body: NoteBody,
  status: NoteStatus
)

object SaveDemoNoteRequest:
  given Encoder[SaveDemoNoteRequest] = deriveEncoder[SaveDemoNoteRequest]
  given Decoder[SaveDemoNoteRequest] = deriveDecoder[SaveDemoNoteRequest]
