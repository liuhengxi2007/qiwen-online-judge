package domains.auth.utils



import cats.effect.IO
import domains.auth.objects.{PasswordHash, PlaintextPassword}

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher:

  private val random = SecureRandom()
  private val iterations = 65_536
  private val keyLength = 256
  private val algorithm = "PBKDF2WithHmacSHA256"
  private val saltLengthBytes = 16
  private val hashPrefix = "pbkdf2-sha256"

  def hashPassword(password: PlaintextPassword): IO[PasswordHash] =
    for
      salt <- generateSalt
      hash <- pbkdf2(password, salt, iterations, keyLength)
    yield PasswordHash(s"${hashPrefix}$$${iterations}$$${encode(salt)}$$${encode(hash)}")

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
