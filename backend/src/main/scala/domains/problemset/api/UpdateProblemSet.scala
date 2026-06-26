package domains.problemset.api

import cats.effect.IO
import domains.auth.api.{AuthenticatedApi, ResolveAccountUsername}
import domains.auth.objects.internal.AuthenticatedUser

import domains.problemset.objects.*
import domains.problemset.objects.request.UpdateProblemSetRequest
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 更新题单信息和访问策略的认证 API，仅题目管理员可调用。 */
object UpdateProblemSet extends AuthenticatedApi[(ProblemSetSlug, UpdateProblemSetRequest), ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  /** 从路径解析题单 slug 并读取更新请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSetSlug, UpdateProblemSetRequest)] =
    for
      problemSetSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))
      body <- request.as[UpdateProblemSetRequest]
    yield (problemSetSlug, body)

  /** 校验管理权、标题描述、作者账号和授权主体后更新题单并返回最新详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemSetSlug, UpdateProblemSetRequest)
  ): IO[ProblemSetDetail] =
    val (problemSetSlug, request) = input
    for
      /** 注意：非题目管理员返回 404，用于隐藏题单管理入口和题单存在性。 */
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      title <- HttpApiError.fromEitherBadRequest(ProblemSetTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ProblemSetDescription.parse(request.description.value))
      validRequest = request.copy(title = title, description = description)
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      _ <- validateAuthorUsername(connection, validRequest.authorUsername)
      _ <- ProblemSetAccessPolicyValidation.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      _ <- ProblemSetTable.update(connection, problemSet.id, ProblemSetAccessPolicyValidation.sanitizePolicy(validRequest))
      updatedProblemSet <- ProblemSetTable.findBySlug(connection, problemSet.slug).flatMap {
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.internal("Problem set disappeared after update."))
      }
    yield ProblemSetDetail.fromProblemSet(updatedProblemSet)

  /** 校验可选展示作者账号存在；None 表示清空作者引用。 */
  private def validateAuthorUsername(connection: Connection, authorUsername: Option[domains.user.objects.Username]): IO[Unit] =
    authorUsername match
      case Some(username) =>
        ResolveAccountUsername.plan(connection, username).flatMap { user =>
          HttpApiError.ensure(user.username.nonEmpty, HttpApiError.badRequest(ApiMessages.userNotFound))
        }
      case None =>
        IO.unit
