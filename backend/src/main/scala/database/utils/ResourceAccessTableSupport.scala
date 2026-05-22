package database.utils

import shared.access.{AccessSubject, BaseAccess, ResourceAccessGrant, ResourceAccessPolicy}

object ResourceAccessTableSupport:

  def policyFrom(
    baseAccess: BaseAccess,
    viewerGrants: List[ResourceAccessGrant],
    managerGrants: List[ResourceAccessGrant]
  ): ResourceAccessPolicy =
    ResourceAccessPolicy(baseAccess = baseAccess, viewerGrants = viewerGrants.map(_.subject), managerGrants = managerGrants.map(_.subject))

  def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject))),
      managerGrants = policy.managerGrants.distinctBy(subject => (AccessSubject.subjectKind(subject), AccessSubject.subjectKey(subject)))
    )

  def toLegacyVisibility(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Public => "public"
      case BaseAccess.OwnerOnly => "private"

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseColumn[A](columnName: String, rawValue: Int, parse: Int => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseColumn[A](columnName: String, rawValue: Option[String], parse: Option[String] => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")
