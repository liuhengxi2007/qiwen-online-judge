package domains.contest.objects

import domains.user.objects.UserIdentity
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

/** 比赛聚合根，承载基础信息、赛题列表、访问策略、作者和审计时间。 */
final case class Contest(
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
