package shared.access

import domains.user.model.Username
import domains.usergroup.model.UserGroupSlug

enum AccessSubject:
  case User(username: Username)
  case UserGroup(slug: UserGroupSlug)

object AccessSubject:
  def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value
