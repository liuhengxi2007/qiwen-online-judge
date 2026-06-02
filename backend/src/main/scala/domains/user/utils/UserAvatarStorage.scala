package domains.user.utils

import cats.effect.IO

trait UserAvatarStorage:
  def writeObject(objectKey: String, bytes: Array[Byte], contentType: String): IO[Unit]
  def readObject(objectKey: String): IO[Option[Array[Byte]]]
  def deleteObject(objectKey: String): IO[Unit]
