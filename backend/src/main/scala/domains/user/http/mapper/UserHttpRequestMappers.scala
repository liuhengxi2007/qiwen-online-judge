package domains.user.http.mapper

import domains.user.objects.Username
import domains.user.objects.request.{UserListRequest, UserSearchQuery}
import shared.http.utils.PageRequestQuerySupport
import shared.objects.PageRequest

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
