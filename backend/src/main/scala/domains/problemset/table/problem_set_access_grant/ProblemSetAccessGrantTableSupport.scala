package domains.problemset.table.problem_set_access_grant

import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, GrantRole, ResourceAccessPolicy}

object ProblemSetAccessGrantTableSupport:

  def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => (encodeSubjectKindColumn(subject), encodeSubjectKeyColumn(subject))),
      managerGrants = policy.managerGrants.distinctBy(subject => (encodeSubjectKindColumn(subject), encodeSubjectKeyColumn(subject)))
    )

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

  def decodeSubjectColumns(subjectKind: String, subjectKey: String): AccessSubject =
    subjectKind match
      case "user" => AccessSubject.User(AccessUsername.canonical(subjectKey))
      case "user_group" =>
        AccessUserGroupSlug
          .parse(subjectKey)
          .fold(
            message => throw IllegalStateException(s"Invalid problem set access subject slug: $message"),
            AccessSubject.UserGroup(_)
          )
      case other =>
        throw IllegalStateException(s"Invalid problem set access subject kind: $other")
