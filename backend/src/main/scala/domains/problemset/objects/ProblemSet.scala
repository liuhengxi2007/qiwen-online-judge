package domains.problemset.objects



import domains.user.objects.UserIdentity
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

/** 题单聚合根，承载基础信息、题目列表、访问策略、作者和审计时间。 */
final case class ProblemSet(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)
