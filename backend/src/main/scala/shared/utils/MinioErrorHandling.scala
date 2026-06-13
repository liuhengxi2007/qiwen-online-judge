package shared.utils

import io.minio.errors.ErrorResponseException

/** Shared classification helpers for MinIO/S3 error responses. */
object MinioErrorHandling:

  def isObjectNotFound(error: ErrorResponseException): Boolean =
    errorCode(error).exists(code => Set("NoSuchKey", "NoSuchObject").contains(code))

  def errorCode(error: ErrorResponseException): Option[String] =
    Option(error.errorResponse()).flatMap(response => Option(response.code()))
