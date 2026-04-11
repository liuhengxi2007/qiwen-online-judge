package domains.problem.application

import domains.auth.model.AuthUser

object ProblemPolicy:

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def hasGlobalManageOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canCreate(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canEdit(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canDelete(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager
