package domains.blog.http.codec

import domains.blog.objects.request.*
import domains.blog.objects.response.*
import domains.blog.http.codec.BlogModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import shared.http.codec.SharedHttpCodecs
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object BlogHttpCodecs:
  export BlogModelHttpCodecs.given
  export SharedHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[CreateBlogRequest] = deriveEncoder[CreateBlogRequest]
  given Decoder[CreateBlogRequest] = deriveDecoder[CreateBlogRequest]
  given Encoder[UpdateBlogRequest] = deriveEncoder[UpdateBlogRequest]
  given Decoder[UpdateBlogRequest] = deriveDecoder[UpdateBlogRequest]
  given Encoder[VoteBlogRequest] = deriveEncoder[VoteBlogRequest]
  given Decoder[VoteBlogRequest] = deriveDecoder[VoteBlogRequest]
  given Encoder[CreateBlogCommentRequest] = deriveEncoder[CreateBlogCommentRequest]
  given Decoder[CreateBlogCommentRequest] = deriveDecoder[CreateBlogCommentRequest]
  given Encoder[UpdateBlogCommentRequest] = deriveEncoder[UpdateBlogCommentRequest]
  given Decoder[UpdateBlogCommentRequest] = deriveDecoder[UpdateBlogCommentRequest]
  given Encoder[VoteBlogCommentRequest] = deriveEncoder[VoteBlogCommentRequest]
  given Decoder[VoteBlogCommentRequest] = deriveDecoder[VoteBlogCommentRequest]

  given Encoder[BlogCommentSummary] = deriveEncoder[BlogCommentSummary]
  given Decoder[BlogCommentSummary] = deriveDecoder[BlogCommentSummary]
  given Encoder[BlogSummary] = deriveEncoder[BlogSummary]
  given Decoder[BlogSummary] = deriveDecoder[BlogSummary]
  given Encoder[BlogDetail] = deriveEncoder[BlogDetail]
  given Decoder[BlogDetail] = deriveDecoder[BlogDetail]
