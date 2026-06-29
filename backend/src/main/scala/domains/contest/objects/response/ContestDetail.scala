package domains.contest.objects.response

import domains.contest.objects.*
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

/** 比赛详情响应，包含访问策略、报名状态、管理标记和可选赛题列表。 */
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

/** 提供比赛详情 JSON codec，以及从比赛聚合构造响应的转换。 */
object ContestDetail:
  given Encoder[ContestDetail] = deriveEncoder[ContestDetail]
  given Decoder[ContestDetail] = deriveDecoder[ContestDetail]

  /** 将比赛聚合转换为接口详情响应，includeProblems 控制是否隐藏赛题列表。 */
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
