package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRanklistItem
import domains.contest.table.contest.{ContestRanklistTable, ContestTable}
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection
import java.time.Instant

object ListContestRanklist extends AuthenticatedApi[(ContestSlug, PageRequest), PageResponse[ContestRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/ranklist")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ContestRanklistItem]] = summon[Encoder[PageResponse[ContestRanklistItem]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, PageRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
    yield (contestSlug, pageRequest)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, PageRequest)
  ): IO[PageResponse[ContestRanklistItem]] =
    val (contestSlug, pageRequest) = input
    val normalizedPageRequest = pageRequest.normalized
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
      registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
      now = Instant.now()
      registeredBeforeStart = registration.exists(registeredAt => !registeredAt.isAfter(contest.startAt))
      canViewRanklist = canManageContest || now.isAfter(contest.endAt) || registeredBeforeStart
      _ <- HttpApiError.ensure(canViewRanklist, HttpApiError.forbidden(ApiMessages.contestNotRegistered))
      ranklist <- ContestRanklistTable.listForContest(connection, contest.id, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    yield ranklist
