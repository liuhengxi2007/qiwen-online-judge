package domains.user.utils

import cats.effect.IO

/** 用户头像对象存储抽象，隐藏具体 MinIO/S3 读写实现。 */
trait UserAvatarStorage:
  /** 写入头像对象，调用方负责先完成大小和内容类型校验。 */
  def writeObject(objectKey: String, bytes: Array[Byte], contentType: String): IO[Unit]
  /** 读取头像对象字节，缺失时返回 None。 */
  def readObject(objectKey: String): IO[Option[Array[Byte]]]
  /** 删除头像对象，具体实现应允许对象不存在。 */
  def deleteObject(objectKey: String): IO[Unit]
