package domains.auth.application

import cats.effect.{IO, Ref}
import domains.auth.model.Username

import java.security.SecureRandom
import java.util.Base64

final class SessionStore private (
  sessionsRef: Ref[IO, Map[String, Username]],
  random: SecureRandom
):

  private val tokenLengthBytes = 32

  def createSession(username: Username): IO[String] =
    for
      token <- nextToken
      _ <- sessionsRef.update(_.updated(token, username))
    yield token

  def lookupUsername(token: String): IO[Option[Username]] =
    sessionsRef.get.map(_.get(token))

  def deleteSession(token: String): IO[Unit] =
    sessionsRef.update(_ - token)

  private def nextToken: IO[String] =
    IO.delay {
      val tokenBytes = new Array[Byte](tokenLengthBytes)
      random.nextBytes(tokenBytes)
      Base64.getUrlEncoder.withoutPadding().encodeToString(tokenBytes)
    }

object SessionStore:

  def create: IO[SessionStore] =
    Ref
      .of[IO, Map[String, Username]](Map.empty)
      .map(ref => new SessionStore(ref, SecureRandom()))
