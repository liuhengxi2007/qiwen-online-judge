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
  registrationStatus: ContestRegistrationStatus,
  canManage: Boolean,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)

object ContestDetail:
  given Encoder[ContestDetail] = deriveEncoder[ContestDetail]
  given Decoder[ContestDetail] = deriveDecoder[ContestDetail]

  def fromContest(contest: Contest, registrationStatus: ContestRegistrationStatus, canManage: Boolean, includeProblems: Boolean): ContestDetail =
    ContestDetail(
      id = contest.id,
      slug = contest.slug,
      title = contest.title,
      description = contest.description,
      startAt = contest.startAt,
      endAt = contest.endAt,
      problems = if includeProblems then contest.problems else Nil,
      accessPolicy = contest.accessPolicy,
      registrationStatus = registrationStatus,
      canManage = canManage,
      author = contest.author,
      createdAt = contest.createdAt,
      updatedAt = contest.updatedAt
    )
