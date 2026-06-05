package domains.submission.objects

import domains.contest.objects.{ContestSlug, ContestTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SubmissionSource(
  contestSlug: Option[ContestSlug],
  contestTitle: Option[ContestTitle]
)

object SubmissionSource:
  val FromProblemSet: SubmissionSource = SubmissionSource(contestSlug = None, contestTitle = None)

  def fromContest(slug: ContestSlug, title: ContestTitle): SubmissionSource =
    SubmissionSource(contestSlug = Some(slug), contestTitle = Some(title))

  given Encoder[SubmissionSource] = deriveEncoder[SubmissionSource]
  given Decoder[SubmissionSource] = deriveDecoder[SubmissionSource]
