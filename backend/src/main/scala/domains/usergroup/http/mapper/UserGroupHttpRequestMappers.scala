package domains.usergroup.http.mapper

import domains.user.model.Username
import domains.usergroup.model.UserGroupSlug
import domains.usergroup.model.request.{AddUserGroupMemberRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import shared.http.utils.PageRequestQuerySupport
import shared.model.PageRequest

object UserGroupHttpRequestMappers:

  def userGroupSlug(rawGroupSlug: String): Either[String, UserGroupSlug] =
    UserGroupSlug.parse(rawGroupSlug)

  def username(rawUsername: String): Username =
    Username.canonical(rawUsername)

  def listUserGroupsRequest(queryParams: Map[String, String]): PageRequest =
    PageRequestQuerySupport.parsePageRequest(queryParams)

  def addMemberInput(
    rawGroupSlug: String,
    body: AddUserGroupMemberRequest
  ): Either[String, (UserGroupSlug, AddUserGroupMemberRequest)] =
    UserGroupSlug.parse(rawGroupSlug).map(groupSlug => (groupSlug, body))

  def removeMemberInput(rawGroupSlug: String, rawUsername: String): Either[String, (UserGroupSlug, Username)] =
    UserGroupSlug.parse(rawGroupSlug).map(groupSlug => (groupSlug, username(rawUsername)))

  def updateGroupInput(
    rawGroupSlug: String,
    body: UpdateUserGroupRequest
  ): Either[String, (UserGroupSlug, UpdateUserGroupRequest)] =
    UserGroupSlug.parse(rawGroupSlug).map(groupSlug => (groupSlug, body))

  def updateMemberRoleInput(
    rawGroupSlug: String,
    rawUsername: String,
    body: UpdateUserGroupMemberRoleRequest
  ): Either[String, (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)] =
    UserGroupSlug.parse(rawGroupSlug).map(groupSlug => (groupSlug, username(rawUsername), body))
