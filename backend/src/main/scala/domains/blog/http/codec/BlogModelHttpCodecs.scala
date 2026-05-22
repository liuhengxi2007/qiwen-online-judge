package domains.blog.http.codec

import domains.blog.model.*
import domains.problem.http.codec.ProblemModelHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object BlogModelHttpCodecs:
  given Encoder[BlogId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogId] = Decoder.decodeLong.emap { value =>
    if value > 0 then Right(BlogId(value)) else Left("Blog id must be a positive integer.")
  }

  given Encoder[BlogCommentId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogCommentId] = Decoder.decodeLong.emap { value =>
    if value > 0 then Right(BlogCommentId(value)) else Left("Blog comment id must be a positive integer.")
  }

  given Encoder[BlogTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogTitle] = Decoder.decodeString.emap(BlogTitle.parse)

  given Encoder[BlogContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogContent] = Decoder.decodeString.emap(BlogContent.parse)

  given Encoder[BlogCommentContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogCommentContent] = Decoder.decodeString.emap(BlogCommentContent.parse)

  given Encoder[BlogVisibility] = Encoder.encodeString.contramap(encodeBlogVisibility)
  given Decoder[BlogVisibility] = Decoder.decodeString.emap(BlogVisibility.parse)

  given Encoder[BlogVote] = Encoder.encodeString.contramap(encodeBlogVote)
  given Decoder[BlogVote] = Decoder.decodeString.emap(BlogVote.parse)

  given Encoder[BlogProblemReference] = deriveEncoder[BlogProblemReference]
  given Decoder[BlogProblemReference] = deriveDecoder[BlogProblemReference]

  private def encodeBlogVisibility(value: BlogVisibility): String =
    value match
      case BlogVisibility.Public => "public"
      case BlogVisibility.Private => "private"

  private def encodeBlogVote(value: BlogVote): String =
    value match
      case BlogVote.Up => "up"
      case BlogVote.Down => "down"
