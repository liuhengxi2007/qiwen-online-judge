package domains.contest.objects.response

import domains.contest.objects.*
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

final case class ContestDetail(
  id: ContestId,
  slug: ContestSlug,
  title: ContestTitle,
  description: ContestDescription,
  startAt: Instant,
  endAt: Instant,
  problems: List[ContestProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

object ContestDetail:
  given Encoder[ContestDetail] = deriveEncoder[ContestDetail]
  given Decoder[ContestDetail] = deriveDecoder[ContestDetail]

  def fromContest(contest: Contest): ContestDetail =
    ContestDetail(
      id = contest.id,
      slug = contest.slug,
      title = contest.title,
      description = contest.description,
      startAt = contest.startAt,
      endAt = contest.endAt,
      problems = contest.problems,
      accessPolicy = contest.accessPolicy,
      author = contest.author,
      createdAt = contest.createdAt,
      updatedAt = contest.updatedAt
    )
