package database.utils

import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, GrantRole}

object AccessGrantSql:

  def subjectIdentity(subject: AccessSubject): (String, String) =
    (encodeSubjectKindColumn(subject), encodeSubjectKeyColumn(subject))

  def encodeGrantRoleColumn(grantRole: GrantRole): String =
    grantRole match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"

  def decodeGrantRoleColumn(value: String): Option[GrantRole] =
    GrantRole.parse(value).toOption

  def encodeSubjectKindColumn(subject: AccessSubject): String =
    subject match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  def encodeSubjectKeyColumn(subject: AccessSubject): String =
    subject match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  def decodeSubjectColumns(subjectKind: String, subjectKey: String, context: String): AccessSubject =
    subjectKind match
      case "user" => AccessSubject.User(AccessUsername.canonical(subjectKey))
      case "user_group" =>
        AccessUserGroupSlug
          .parse(subjectKey)
          .fold(
            message => throw IllegalStateException(s"Invalid $context subject slug: $message"),
            AccessSubject.UserGroup(_)
          )
      case other =>
        throw IllegalStateException(s"Invalid $context subject kind: $other")
