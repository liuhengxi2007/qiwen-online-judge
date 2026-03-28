package auth

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

  def hashPassword(password: String): String =
    val salt = new Array[Byte](saltLengthBytes)
    random.nextBytes(salt)
    val hash = pbkdf2(password, salt, iterations, keyLength)
    s"${hashPrefix}$$${iterations}$$${encode(salt)}$$${encode(hash)}"

  def verifyPassword(password: String, encodedHash: String): Boolean =
    parseHash(encodedHash) match
      case Some(parsedHash) =>
        val derivedHash = pbkdf2(password, parsedHash.salt, parsedHash.iterations, keyLength)
        java.security.MessageDigest.isEqual(derivedHash, parsedHash.hash)
      case None =>
        false

  private def pbkdf2(password: String, salt: Array[Byte], iterations: Int, keyLength: Int): Array[Byte] =
    val spec = PBEKeySpec(password.toCharArray, salt, iterations, keyLength)
    try SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded
    finally spec.clearPassword()

  private def encode(bytes: Array[Byte]): String =
    Base64.getEncoder.encodeToString(bytes)

  private def decode(value: String): Array[Byte] =
    Base64.getDecoder.decode(value)

  private def parseHash(encodedHash: String): Option[ParsedHash] =
    encodedHash.split("\\$", -1).toList match
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
