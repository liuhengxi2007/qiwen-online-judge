package domains.user.http.mapper

import domains.user.model.Username
import domains.user.model.request.{UpdateUserPermissionsRequest, UserListRequest, UserSearchQuery}
import shared.http.utils.PageRequestQuerySupport
import shared.model.PageRequest

object UserHttpRequestMappers:

  def username(rawUsername: String): Username =
    Username.canonical(rawUsername)

  def ranklistRequest(queryParams: Map[String, String]): PageRequest =
    PageRequest(page = queryParams.get("page").flatMap(_.toIntOption).getOrElse(1))

  def listUsersRequest(queryParams: Map[String, String]): UserListRequest =
    UserListRequest(
      query = queryParams.get("q").flatMap(rawQuery => UserSearchQuery.parse(rawQuery).toOption),
      pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
    )

  def userSearchQuery(queryParams: Map[String, String]): Either[String, UserSearchQuery] =
    UserSearchQuery.parse(queryParams.get("q").getOrElse(""))

  def updateUserPermissionsInput(
    rawUsername: String,
    body: UpdateUserPermissionsRequest
  ): (Username, UpdateUserPermissionsRequest) =
    (Username.canonical(rawUsername), body)
