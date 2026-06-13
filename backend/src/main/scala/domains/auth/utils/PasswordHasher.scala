package domains.auth.utils



import cats.effect.IO
import domains.auth.objects.{PasswordHash, PlaintextPassword}

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** 密码哈希工具，使用 PBKDF2-SHA256 生成和验证可持久化密码哈希。 */
object PasswordHasher:

  private val random = SecureRandom()
  private val iterations = 65_536
  private val keyLength = 256
  private val algorithm = "PBKDF2WithHmacSHA256"
  private val saltLengthBytes = 16
  private val hashPrefix = "pbkdf2-sha256"

  /** 为明文密码生成随机盐和 PBKDF2 哈希，输出包含算法、迭代次数、盐和哈希。 */
  def hashPassword(password: PlaintextPassword): IO[PasswordHash] =
    for
      salt <- generateSalt
      hash <- pbkdf2(password, salt, iterations, keyLength)
    yield PasswordHash(s"${hashPrefix}$$${iterations}$$${encode(salt)}$$${encode(hash)}")

  /** 验证明文密码是否匹配持久化哈希；格式非法时返回 false。 */
  def verifyPassword(password: PlaintextPassword, encodedHash: PasswordHash): IO[Boolean] =
    parseHash(encodedHash) match
      case Some(parsedHash) =>
        pbkdf2(password, parsedHash.salt, parsedHash.iterations, keyLength).map(derivedHash =>
          java.security.MessageDigest.isEqual(derivedHash, parsedHash.hash)
        )
      case None =>
        IO.pure(false)

  private def generateSalt: IO[Array[Byte]] =
    IO.delay {
      val salt = new Array[Byte](saltLengthBytes)
      random.nextBytes(salt)
      salt
    }

  private def pbkdf2(
    password: PlaintextPassword,
    salt: Array[Byte],
    iterations: Int,
    keyLength: Int
  ): IO[Array[Byte]] =
    IO.blocking {
      val spec = PBEKeySpec(password.value.toCharArray, salt, iterations, keyLength)
      try SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded
      finally spec.clearPassword()
    }

  private def encode(bytes: Array[Byte]): String =
    Base64.getEncoder.encodeToString(bytes)

  private def decode(value: String): Array[Byte] =
    Base64.getDecoder.decode(value)

  private def parseHash(encodedHash: PasswordHash): Option[ParsedHash] =
    encodedHash.value.split("\\$", -1).toList match
      case prefix :: iterationValue :: saltValue :: hashValue :: Nil if prefix == hashPrefix =>
        /** FIXME-CN: salt/hash 的 Base64 解码异常未被捕获，畸形持久化哈希可能让登录校验抛 500 而不是返回 false。 */
        iterationValue.toIntOption.map(iteration =>
          ParsedHash(iteration, decode(saltValue), decode(hashValue))
        )
      case _ =>
        None

  private final case class ParsedHash(
    iterations: Int,
    salt: Array[Byte],
    hash: Array[Byte]
  )
