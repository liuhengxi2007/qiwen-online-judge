package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRegistrationStatus
import domains.contest.table.contest.ContestTable
import domains.contest.table.contest.ContestTable.UnregisterTableResult
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

object UnregisterContest extends AuthenticatedApi[ContestSlug, ContestRegistrationStatus]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/unregister")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestRegistrationStatus] = summon[Encoder[ContestRegistrationStatus]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ContestSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    slug: ContestSlug
  ): IO[ContestRegistrationStatus] =
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
      _ <- HttpApiError.ensure(
        Instant.now().isBefore(contest.startAt),
        HttpApiError.badRequest(ApiMessages.contestRegistrationLocked)
      )
      _ <- ContestTable.unregister(connection, contest.id, actor.username).flatMap {
        case UnregisterTableResult.Unregistered => IO.unit
        case UnregisterTableResult.NotRegistered =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.contestNotRegistered))
      }
    yield ContestRegistrationStatus.notRegistered
