package domains.auth.application

import cats.effect.IO
import domains.auth.model.{SessionToken, Username}

import java.time.Instant

final case class CachedSession(
  username: Username,
  expiresAt: Instant
)

trait SessionCache:
  def get(token: SessionToken): IO[Option[CachedSession]]
  def put(token: SessionToken, session: CachedSession): IO[Unit]
  def delete(token: SessionToken): IO[Unit]

object SessionCache:
  val noop: SessionCache = new SessionCache:
    override def get(token: SessionToken): IO[Option[CachedSession]] = IO.pure(None)
    override def put(token: SessionToken, session: CachedSession): IO[Unit] = IO.unit
    override def delete(token: SessionToken): IO[Unit] = IO.unit
