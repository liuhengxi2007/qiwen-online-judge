package domains.problem.application

import domains.auth.model.AuthUser

object ProblemPolicy:

  def canCreate(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canEdit(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canDelete(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager
