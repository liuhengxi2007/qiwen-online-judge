package domains.contest.api

import cats.effect.IO
import domains.auth.api.{AuthenticatedApi, AuthenticatedResponseApi}
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.api.*
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.request.DeleteProblemDataPathRequest
import domains.problem.objects.response.{ProblemDataTreeResponse, ProblemDataUploadResult, ProblemDetail}
import domains.problem.utils.ProblemDataStorage
import fs2.text
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListContestProblemDataTree extends AuthenticatedApi[(ContestSlug, ProblemSlug), ProblemDataTreeResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/files/tree")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataTreeResponse] = summon[Encoder[ProblemDataTreeResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    ContestProblemDataApis.decodeContestProblemPath(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ProblemDataTreeResponse] =
    val (contestSlug, problemSlug) = input
    for
      problem <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      response <- ListProblemDataTree.listManagedProblemDataTree(connection, problem)
    yield response

final case class DownloadContestProblemDataPath(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedResponseApi[(ContestSlug, ProblemSlug, ProblemDataPath)]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/files/download")

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, ProblemDataPath)] =
    HttpApiError.fromEitherBadRequest(
      for
        contestSlug <- pathParams.require("contestSlug").flatMap(ContestSlug.parse)
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        rawPath <- request.uri.query.params.get("path").toRight("Missing query parameter: path.")
        path <- ProblemDataPath.parse(rawPath)
      yield (contestSlug, problemSlug, path)
    )

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, ProblemDataPath)
  ): IO[Response[IO]] =
    val (contestSlug, problemSlug, path) = input
    for
      problem <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      response <- DownloadProblemDataPath(problemDataStorage).downloadManagedProblemDataPath(problem, path)
    yield response

final case class DeleteContestProblemDataPath(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug, DeleteProblemDataPathRequest), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/files/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, DeleteProblemDataPathRequest)] =
    for
      contestProblem <- ContestProblemDataApis.decodeContestProblemPath(request, pathParams)
      (contestSlug, problemSlug) = contestProblem
      body <- request.as[DeleteProblemDataPathRequest]
    yield (contestSlug, problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, DeleteProblemDataPathRequest)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug, request) = input
    for
      _ <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      problem <- DeleteProblemDataPath(problemDataStorage).deleteManagedProblemDataPath(connection, problemSlug, request)
    yield problem

final case class ClearContestProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug), ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/files/delete-all")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    ContestProblemDataApis.decodeContestProblemPath(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug) = input
    for
      _ <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      problem <- ClearProblemData(problemDataStorage).clearManagedProblemData(connection, problemSlug)
    yield problem

final case class SetContestProblemDataReady(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug, SetProblemReadyRequest), ProblemDetail]:

  private given Decoder[SetProblemReadyRequest] = deriveDecoder[SetProblemReadyRequest]

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/ready-state")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, SetProblemReadyRequest)] =
    for
      contestProblem <- ContestProblemDataApis.decodeContestProblemPath(request, pathParams)
      (contestSlug, problemSlug) = contestProblem
      body <- request.as[SetProblemReadyRequest]
    yield (contestSlug, problemSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, SetProblemReadyRequest)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug, request) = input
    for
      _ <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      problem <- SetProblemDataReady(problemDataStorage).setManagedProblemDataReady(connection, problemSlug, request.ready)
    yield problem

final case class UploadContestProblemDataFile(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug, ProblemDataPath, Array[Byte]), ProblemDataUploadResult]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/files")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, ProblemDataPath, Array[Byte])] =
    for
      contestProblem <- ContestProblemDataApis.decodeContestProblemPath(request, pathParams)
      (contestSlug, problemSlug) = contestProblem
      multipart <- request.as[Multipart[IO]]
      file <- ContestProblemDataApis.extractNamedBinaryPart(multipart, "file").flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart file field 'file' is required."))
      }
      (filePart, bytes) = file
      maybePath <- ContestProblemDataApis.extractOptionalPathField(multipart, "path")
      resolvedPath <- maybePath.orElse(filePart.filename.flatMap(name => ProblemDataPath.parse(name).toOption)) match
        case Some(path) => IO.pure(path)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart upload requires a valid 'path' field or uploaded filename."))
    yield (contestSlug, problemSlug, resolvedPath, bytes)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, ProblemDataPath, Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (contestSlug, problemSlug, path, bytes) = input
    for
      _ <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      result <- UploadProblemDataFile(problemDataStorage).uploadManagedProblemDataFile(connection, problemSlug, path, bytes)
    yield result

final case class UploadContestProblemDataArchive(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[(ContestSlug, ProblemSlug, Option[ProblemDataPath], Array[Byte]), ProblemDataUploadResult]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug/data/archive-imports")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataUploadResult] = summon[Encoder[ProblemDataUploadResult]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug, Option[ProblemDataPath], Array[Byte])] =
    for
      contestProblem <- ContestProblemDataApis.decodeContestProblemPath(request, pathParams)
      (contestSlug, problemSlug) = contestProblem
      multipart <- request.as[Multipart[IO]]
      file <- ContestProblemDataApis.extractNamedBinaryPart(multipart, "file").flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart file field 'file' is required."))
      }
      (filePart, bytes) = file
      _ <- HttpApiError.ensure(
        filePart.filename.exists(_.toLowerCase.endsWith(".zip")),
        HttpApiError.badRequest("Multipart archive upload requires a .zip file.")
      )
      targetDirectory <- ContestProblemDataApis.extractOptionalPathField(multipart, "targetDir")
    yield (contestSlug, problemSlug, targetDirectory, bytes)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug, Option[ProblemDataPath], Array[Byte])
  ): IO[ProblemDataUploadResult] =
    val (contestSlug, problemSlug, targetDirectory, bytes) = input
    for
      _ <- ContestProblemApiSupport.requireManageLinkedProblem(connection, actor, contestSlug, problemSlug)
      result <- UploadProblemDataArchive(problemDataStorage).uploadManagedProblemDataArchive(connection, problemSlug, targetDirectory, bytes)
    yield result

object ContestProblemDataApis:

  def decodeContestProblemPath(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  def extractNamedBinaryPart(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[(Part[IO], Array[Byte])]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      case Some(part) => part.body.compile.to(Array).map(bytes => Some((part, bytes)))

  def extractOptionalPathField(
    multipart: Multipart[IO],
    fieldName: String
  ): IO[Option[ProblemDataPath]] =
    multipart.parts.find(_.name.contains(fieldName)) match
      case None => IO.pure(None)
      case Some(part) =>
        decodeTextPart(part).flatMap { rawValue =>
          val normalized = rawValue.trim
          if normalized.isEmpty then IO.pure(None)
          else HttpApiError.fromEitherBadRequest(ProblemDataPath.parse(normalized).map(Some(_)))
        }

  private def decodeTextPart(part: Part[IO]): IO[String] =
    part.body.through(text.utf8.decode).compile.string
