package domains.submission.utils

import domains.auth.objects.AuthUser
import domains.problem.objects.OthersSubmissionAccess
import domains.user.objects.Username

object SubmissionAccessRules:
  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canViewOwnOrWithGlobalOverride(actor: AuthUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value

  def canViewDetailOfOthers(othersSubmissionAccess: OthersSubmissionAccess): Boolean =
    othersSubmissionAccess match
      case OthersSubmissionAccess.None => false
      case OthersSubmissionAccess.Summary => false
      case OthersSubmissionAccess.Detail => true
