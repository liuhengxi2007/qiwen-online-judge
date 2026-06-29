package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import database.DatabaseSession
import domains.auth.api.SessionStoreContext
import domains.contest.routes.ContestRouter
import domains.problem.routes.ProblemRouter
import domains.submission.api.SubmissionProgramStorageContext
import munit.CatsEffectSuite
import org.http4s.{Method, Request, Status, Uri}
import shared.api.ApiPath

class ProblemDataApiPathSuite extends CatsEffectSuite:

  private val problemDataStorage = null.asInstanceOf[ProblemDataStorageContext]
  private val submissionProgramStorage = null.asInstanceOf[SubmissionProgramStorageContext]

  test("problem data APIs expose the renamed route surface") {
    val actualPaths = List(
      "ListProblemDataFiles" -> ListProblemDataFiles(problemDataStorage).path,
      "ListManageableProblemSuggestions" -> ListManageableProblemSuggestions.path,
      "UploadProblemDataFile" -> UploadProblemDataFile(problemDataStorage).path,
      "ListProblemDataTree" -> ListProblemDataTree.path,
      "DownloadProblemDataPath" -> DownloadProblemDataPath(problemDataStorage).path,
      "DownloadProblemDataArchive" -> DownloadProblemDataArchive(problemDataStorage).path,
      "DeleteProblemDataPath" -> DeleteProblemDataPath(problemDataStorage).path,
      "UploadProblemDataArchive" -> UploadProblemDataArchive(problemDataStorage).path,
      "ClearProblemData" -> ClearProblemData(problemDataStorage).path,
      "SetProblemDataReady" -> SetProblemDataReady(problemDataStorage).path,
      "RejudgeProblemSubmissions" -> RejudgeProblemSubmissions.path,
    )

    assertEquals(
      actualPaths,
      List(
        "ListProblemDataFiles" -> ApiPath("/api/problems/:problemSlug/data/files"),
        "ListManageableProblemSuggestions" -> ApiPath("/api/problem-suggestions/manageable"),
        "UploadProblemDataFile" -> ApiPath("/api/problems/:problemSlug/data/files"),
        "ListProblemDataTree" -> ApiPath("/api/problems/:problemSlug/data/files/tree"),
        "DownloadProblemDataPath" -> ApiPath("/api/problems/:problemSlug/data/files/download"),
        "DownloadProblemDataArchive" -> ApiPath("/api/problems/:problemSlug/data/archive-downloads"),
        "DeleteProblemDataPath" -> ApiPath("/api/problems/:problemSlug/data/files/delete"),
        "UploadProblemDataArchive" -> ApiPath("/api/problems/:problemSlug/data/archive-imports"),
        "ClearProblemData" -> ApiPath("/api/problems/:problemSlug/data/files/delete-all"),
        "SetProblemDataReady" -> ApiPath("/api/problems/:problemSlug/data/ready-state"),
        "RejudgeProblemSubmissions" -> ApiPath("/api/problems/:problemSlug/submissions/rejudge"),
      )
    )
  }

  test("problem router no longer matches legacy problem data routes") {
    val routes = ProblemRouter
      .routes(
        null.asInstanceOf[DatabaseSession],
        null.asInstanceOf[SessionStoreContext],
        problemDataStorage,
        submissionProgramStorage
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
      val request = Request[IO](method = method, uri = uri(path))
      routes.run(request).map(response => assertEquals(response.status, Status.NotFound, s"$method $path"))
    }
  }

  test("contest router no longer matches problem-owned mutation and data routes") {
    val routes = ContestRouter
      .routes(
        null.asInstanceOf[DatabaseSession],
        null.asInstanceOf[SessionStoreContext],
        submissionProgramStorage,
        problemDataStorage
      )
      .orNotFound

    val removedRequests = List(
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/update",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/delete-problem",
      Method.GET -> "/api/contests/sample-contest/problem-suggestions?q=two",
      Method.GET -> "/api/contests/sample-contest/problems/two-sum/data/files/tree",
      Method.GET -> "/api/contests/sample-contest/problems/two-sum/data/files/download?path=main.in",
      Method.GET -> "/api/contests/sample-contest/problems/two-sum/data/archive-downloads",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/data/files",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/data/archive-imports",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/data/files/delete",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/data/files/delete-all",
      Method.POST -> "/api/contests/sample-contest/problems/two-sum/data/ready-state",
    )

    removedRequests.traverse_ { case (method, path) =>
      val request = Request[IO](method = method, uri = uri(path))
      routes.run(request).map(response => assertEquals(response.status, Status.NotFound, s"$method $path"))
    }
  }

  private def uri(value: String): Uri =
    Uri.fromString(value).fold(error => fail(error.toString), identity)
