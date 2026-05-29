package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser

import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.utils.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetProblemSet extends AuthenticatedApi[ProblemSetSlug, ProblemSetDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-sets/:problemSetSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSetSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSetSlug").flatMap(ProblemSetSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    slug: ProblemSetSlug
  ): IO[ProblemSetDetail] =
    ProblemSetTable.findBySlug(connection, slug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
      case Some(problemSet) =>
        ListUserGroupSlugsForMember.plan(connection, actor.username).flatMap { actorGroupSlugs =>
          if ProblemSetAccessRules.canViewProblemSet(actor, problemSet, actorGroupSlugs.slugs.toSet) then
            IO.pure(ProblemSetDetail.fromProblemSet(problemSet))
          else HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemSetNotFound))
        }
    }
