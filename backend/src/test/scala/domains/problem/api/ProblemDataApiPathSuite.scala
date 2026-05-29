package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.problem.routes.ProblemRouter
import domains.problem.utils.ProblemDataStorage
import munit.CatsEffectSuite
import org.http4s.{Method, Request, Status, Uri}
import shared.api.ApiPath

class ProblemDataApiPathSuite extends CatsEffectSuite:

  private val problemDataStorage = null.asInstanceOf[ProblemDataStorage]

  test("problem data APIs expose the renamed route surface") {
    val actualPaths = List(
      "ListProblemDataFiles" -> ListProblemDataFiles(problemDataStorage).path,
      "UploadProblemDataFile" -> UploadProblemDataFile(problemDataStorage).path,
      "ListProblemDataTree" -> ListProblemDataTree.path,
      "DownloadProblemDataPath" -> DownloadProblemDataPath(problemDataStorage).path,
      "DeleteProblemDataPath" -> DeleteProblemDataPath(problemDataStorage).path,
      "UploadProblemDataArchive" -> UploadProblemDataArchive(problemDataStorage).path,
      "ClearProblemData" -> ClearProblemData(problemDataStorage).path,
      "SetProblemDataReady" -> SetProblemDataReady(problemDataStorage).path,
    )

    assertEquals(
      actualPaths,
      List(
        "ListProblemDataFiles" -> ApiPath("/api/problems/:problemSlug/data/files"),
        "UploadProblemDataFile" -> ApiPath("/api/problems/:problemSlug/data/files"),
        "ListProblemDataTree" -> ApiPath("/api/problems/:problemSlug/data/files/tree"),
        "DownloadProblemDataPath" -> ApiPath("/api/problems/:problemSlug/data/files/download"),
        "DeleteProblemDataPath" -> ApiPath("/api/problems/:problemSlug/data/files/delete"),
        "UploadProblemDataArchive" -> ApiPath("/api/problems/:problemSlug/data/archive-imports"),
        "ClearProblemData" -> ApiPath("/api/problems/:problemSlug/data/files/delete-all"),
        "SetProblemDataReady" -> ApiPath("/api/problems/:problemSlug/data/ready-state"),
      )
    )
  }

  test("problem router no longer matches legacy problem data routes") {
    val routes = ProblemRouter
      .routes(
        null.asInstanceOf[DatabaseSession],
        null.asInstanceOf[SessionStore],
        problemDataStorage
      )
      .orNotFound

    val legacyRequests = List(
      Method.GET -> "/api/problems/example/data",
      Method.GET -> "/api/problems/example/data/tree",
      Method.GET -> "/api/problems/example/data/file?path=main.in",
      Method.POST -> "/api/problems/example/data/file/delete",
      Method.POST -> "/api/problems/example/data/archive",
      Method.POST -> "/api/problems/example/data/clear",
      Method.POST -> "/api/problems/example/data/ready",
      Method.GET -> "/api/problems/example/data/main.in",
      Method.POST -> "/api/problems/example/data/main.in/delete",
    )

    legacyRequests.traverse_ { case (method, path) =>
      val request = Request[IO](method = method, uri = Uri.unsafeFromString(path))
      routes.run(request).map(response => assertEquals(response.status, Status.NotFound, s"$method $path"))
    }
  }
