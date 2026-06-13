package domains.submission.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.OtherUserSubmissionAccess
import domains.user.objects.Username

/** 提交访问规则；封装本人、全局题目管理员和题目他人提交可见级别的判断。 */
object SubmissionAccessRules:
  /** 题目管理员可全局查看提交摘要和详情。 */
  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  /** 判断用户是否可因本人身份或全局权限查看提交详情。 */
  def canViewOwnOrWithGlobalOverride(actor: AuthenticatedUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value

  /** 根据题目策略判断普通其他用户是否可查看完整提交详情。 */
  def canViewDetailOfOthers(otherUserSubmissionAccess: OtherUserSubmissionAccess): Boolean =
    otherUserSubmissionAccess match
      case OtherUserSubmissionAccess.None => false
      case OtherUserSubmissionAccess.Summary => false
      case OtherUserSubmissionAccess.Detail => true
