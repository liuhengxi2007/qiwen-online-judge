package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 获取题单详情的认证 API，会按题单访问策略隐藏不可见题单。 */
object GetProblemSet extends AuthenticatedApi[ProblemSetSlug, ProblemSetDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  /** 从路径解析题单 slug，非法 slug 转为 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSetSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))

  /** 读取题单和调用者用户组，未满足查看条件时返回 404。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    slug: ProblemSetSlug
  ): IO[ProblemSetDetail] =
    ProblemSetTable.findBySlug(connection, slug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      case Some(problemSet) =>
        ListUserGroupSlugsForMember.plan(connection, actor.username).flatMap { actorGroupSlugs =>
          /** 注意：无权查看题单返回 404，是隐藏受限题单存在性的边界策略。 */
          if ProblemSetAccessRules.canViewProblemSet(actor, problemSet, actorGroupSlugs.slugs.toSet) then
            IO.pure(ProblemSetDetail.fromProblemSet(problemSet))
          else HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
        }
    }
