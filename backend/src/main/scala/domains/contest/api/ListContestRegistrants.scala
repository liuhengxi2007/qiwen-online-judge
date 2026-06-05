package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.response.ContestRegistrant
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection
import java.time.Instant

object ListContestRegistrants extends AuthenticatedApi[(ContestSlug, PageRequest), PageResponse[ContestRegistrant]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/registrants")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ContestRegistrant]] = summon[Encoder[PageResponse[ContestRegistrant]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, PageRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
    yield (contestSlug, pageRequest)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, PageRequest)
  ): IO[PageResponse[ContestRegistrant]] =
    val (contestSlug, pageRequest) = input
    val normalizedPageRequest = pageRequest.normalized
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      isRegistered <- ContestTable.isRegistered(connection, contest.id, actor.username)
      now = Instant.now()
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContestDetail(actor, contest, actorGroupSlugs.slugs.toSet, isRegistered, now),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      registrants <- ContestTable.listRegistrants(connection, contest.id, normalizedPageRequest.page, normalizedPageRequest.pageSize)
    yield registrants
