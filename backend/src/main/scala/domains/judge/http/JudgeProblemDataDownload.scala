package domains.judge.http

import cats.effect.IO
import domains.problem.application.ProblemDataStorage
import domains.problem.model.{ProblemDataPath, ProblemSlug}
import domains.shared.http.ApiMessages
import domains.shared.http.HttpResponseSupport.errorResponse
import fs2.Stream
import org.http4s.{Header, Response, Status}
import org.typelevel.ci.CIString

object JudgeProblemDataDownload:

  def response(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Response[IO]] =
    ProblemDataStorage.readPath(problemSlug, path).flatMap {
      case None =>
        errorResponse(Status.NotFound, ApiMessages.problemDataFileNotFound)
      case Some((storedPath, bytes)) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .putHeaders(
              Header.Raw(CIString("Content-Type"), "application/octet-stream"),
              Header.Raw(CIString("Content-Disposition"), s"""attachment; filename="${storedPath.fileName}""""),
              Header.Raw(CIString("Content-Length"), bytes.length.toString)
            )
            .withBodyStream(Stream.emits(bytes).covary[IO])
        )
    }
