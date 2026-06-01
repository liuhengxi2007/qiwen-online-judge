package domains.submission.utils

import munit.FunSuite

class SubmissionProgramStorageConfigSuite extends FunSuite:

  private val completeEnv = Map(
    "MINIO_ENDPOINT" -> "http://localhost:9000",
    "MINIO_ACCESS_KEY" -> "access",
    "MINIO_SECRET_KEY" -> "secret",
    "MINIO_BUCKET" -> "judge-data"
  )

  test("fromEnvironment loads complete MinIO configuration") {
    val config = SubmissionProgramStorageConfig.fromEnvironment(completeEnv)

    assertEquals(
      config.minio,
      MinioSubmissionProgramStorageConfig(
        endpoint = "http://localhost:9000",
        accessKey = "access",
        secretKey = "secret",
        bucket = "judge-data",
        secure = true
      )
    )
  }

  test("fromEnvironment honors MINIO_SECURE=false") {
    val config = SubmissionProgramStorageConfig.fromEnvironment(completeEnv.updated("MINIO_SECURE", "false"))

    assertEquals(config.minio.secure, false)
  }

  test("fromEnvironment rejects each missing required MinIO value") {
    List("MINIO_ENDPOINT", "MINIO_ACCESS_KEY", "MINIO_SECRET_KEY", "MINIO_BUCKET").foreach { missingName =>
      val error = intercept[IllegalStateException] {
        SubmissionProgramStorageConfig.fromEnvironment(completeEnv - missingName)
      }

      assert(error.getMessage.contains(missingName), s"Expected message to name $missingName, got: ${error.getMessage}")
    }
  }

  test("fromEnvironment rejects blank required MinIO values") {
    val error = intercept[IllegalStateException] {
      SubmissionProgramStorageConfig.fromEnvironment(completeEnv.updated("MINIO_ENDPOINT", "  "))
    }

    assert(error.getMessage.contains("MINIO_ENDPOINT"))
  }

  test("fromEnvironment ignores obsolete local storage variables") {
    val config = SubmissionProgramStorageConfig.fromEnvironment(
      completeEnv ++ Map(
        "SUBMISSION_PROGRAM_STORAGE_BACKEND" -> "local",
        "PROBLEM_DATA_STORAGE_BACKEND" -> "local",
        "SUBMISSION_PROGRAM_LOCAL_ROOT" -> "/tmp/submission-programs",
        "PROBLEM_DATA_LOCAL_ROOT" -> "/tmp/problems"
      )
    )

    assertEquals(config.minio.bucket, "judge-data")
  }
