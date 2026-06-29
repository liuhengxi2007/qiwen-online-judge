package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.{UserAcceptedProblem, Username}
import domains.user.table.user_profile.{UserProfileQueryTable, UserProfileTable}
import domains.user.utils.UserApiRules
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 用户已通过题目分页查询输入，包含路径用户名和查询分页。 */
final case class ListUserAcceptedProblemsInput(
  targetUsername: Username,
  pageRequest: PageRequest
)

/** 用户已通过题目 API，按最近通过时间分页返回题目列表。 */
object ListUserAcceptedProblems extends AuthenticatedApi[ListUserAcceptedProblemsInput, PageResponse[UserAcceptedProblem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/accepted-problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserAcceptedProblem]] = summon[Encoder[PageResponse[UserAcceptedProblem]]]

  /** 从路径读取目标用户名，从查询参数读取页码；页大小固定由用户 API 规则控制。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ListUserAcceptedProblemsInput] =
    HttpApiError.fromEitherBadRequest {
      for
        rawUsername <- pathParams.require("targetUsername")
        pageRequest <- PageRequestQuerySupport.parsePageRequest(request.uri.query.params, defaultPageSize = UserApiRules.ranklistPageSize)
      yield ListUserAcceptedProblemsInput(
        targetUsername = Username.canonical(rawUsername),
        pageRequest = PageRequest(page = pageRequest.page, pageSize = UserApiRules.ranklistPageSize)
      )
    }

  /** 确认目标用户存在后查询已通过题目分页；当前不按 actor 做额外过滤。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: ListUserAcceptedProblemsInput
  ): IO[PageResponse[UserAcceptedProblem]] =
    val _ = actor
    UserProfileTable.findSettingsByUsername(connection, input.targetUsername).flatMap {
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      case Some(_) => UserProfileQueryTable.listAcceptedProblems(connection, input.targetUsername, input.pageRequest)
    }
