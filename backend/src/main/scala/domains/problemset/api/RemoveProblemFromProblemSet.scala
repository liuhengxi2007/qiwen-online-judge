package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.api.ResolveProblemReference
import domains.problem.objects.ProblemSlug

import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 从题单移除题目的认证 API，仅题目管理员可调用。 */
object RemoveProblemFromProblemSet extends AuthenticatedApi[(ProblemSetSlug, ProblemSlug), ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug/problems/:problemSlug/remove")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  /** 从路径解析题单 slug 和题目 slug，移除入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ProblemSetSlug, ProblemSlug)] =
    val _ = request
    HttpApiError.fromEitherBadRequest(
      for
        problemSetSlug <- pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse)
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
      yield (problemSetSlug, problemSlug)
    )

  /** 校验全局题目管理权限和关联存在性后删除题单题目关联。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ProblemSetSlug, ProblemSlug)
  ): IO[ProblemSetDetail] =
    val (problemSetSlug, problemSlug) = input
    for
      /** 注意：非题目管理员返回 404，用于隐藏题单管理入口和题单存在性。 */
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.notFound(ApiMessages.problemSetNotFound)
      )
      maybeProblemSet <- ProblemSetTable.findBySlug(connection, problemSetSlug)
      problemSet <- maybeProblemSet match
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      maybeProblem <- ResolveProblemReference.plan(connection, problemSlug).map(_.problem)
      problem <- maybeProblem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ProblemSetTable.removeProblem(connection, problemSet.id, problem.id).flatMap {
        case ProblemSetTable.RemoveProblemTableResult.NotLinked =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotLinkedToProblemSet))
        case ProblemSetTable.RemoveProblemTableResult.Removed =>
          IO.unit
      }
      updatedProblemSet <- ProblemSetTable.findBySlug(connection, problemSet.slug).flatMap {
        case Some(problemSet) => IO.pure(problemSet)
        case None => HttpApiError.raise(HttpApiError.internal("Problem set disappeared after problem removal."))
      }
    yield ProblemSetDetail.fromProblemSet(updatedProblemSet)
