package shared.access

enum AccessSubject:
  case User(username: AccessUsername)
  case UserGroup(slug: AccessUserGroupSlug)

object AccessSubject:
  def subjectKind(value: AccessSubject): String =
    value match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  def subjectKey(value: AccessSubject): String =
    value match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value
