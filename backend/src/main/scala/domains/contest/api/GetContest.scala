package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

object GetContest extends AuthenticatedApi[ContestSlug, ContestDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ContestSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    slug: ContestSlug
  ): IO[ContestDetail] =
    for
      maybeContest <- ContestTable.findBySlug(connection, slug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      canManage = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
      includeProblems = canManage || !Instant.now().isBefore(contest.startAt)
      registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
    yield ContestDetail.fromContest(
      contest,
      registration.fold(ContestRegistrationStatus.notRegistered)(ContestRegistrationStatus.registeredAt),
      canManage,
      includeProblems
    )
