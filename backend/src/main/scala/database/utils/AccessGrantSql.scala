package database.utils

import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, GrantRole}

/** 访问授权相关的 SQL 列编解码工具，负责 subject/role 与数据库字符串互转。 */
object AccessGrantSql:

  /** 返回授权主体在数据库中的 kind/key 二元组。 */
  def subjectIdentity(subject: AccessSubject): (String, String) =
    (encodeSubjectKindColumn(subject), encodeSubjectKeyColumn(subject))

  /** 将授权角色编码为数据库列值。 */
  def encodeGrantRoleColumn(grantRole: GrantRole): String =
    grantRole match
      case GrantRole.Viewer => "viewer"
      case GrantRole.Manager => "manager"

  /** 从数据库列值解码授权角色，非法值返回 None 交由调用方处理。 */
  def decodeGrantRoleColumn(value: String): Option[GrantRole] =
    GrantRole.parse(value).toOption

  /** 将授权主体类型编码为数据库 kind 列值。 */
  def encodeSubjectKindColumn(subject: AccessSubject): String =
    subject match
      case AccessSubject.User(_) => "user"
      case AccessSubject.UserGroup(_) => "user_group"

  /** 将授权主体标识编码为数据库 key 列值。 */
  def encodeSubjectKeyColumn(subject: AccessSubject): String =
    subject match
      case AccessSubject.User(username) => username.value
      case AccessSubject.UserGroup(slug) => slug.value

  /** 从数据库 kind/key 列还原授权主体；数据库中出现非法值时抛出状态异常。 */
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
