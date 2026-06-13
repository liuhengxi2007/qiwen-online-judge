package domains.auth.utils



import cats.effect.IO
import domains.auth.objects.SessionToken
import domains.user.objects.Username

import java.time.Instant

/** 会话缓存值，保存用户名和服务端会话过期时间。 */
final case class CachedSession(
  username: Username,
  expiresAt: Instant
)

/** 会话缓存抽象，允许用 Redis 或空实现加速令牌查找。 */
trait SessionCache:
  /** 根据令牌读取缓存会话，缓存缺失返回 None。 */
  def get(token: SessionToken): IO[Option[CachedSession]]
  /** 写入缓存会话，过期策略由具体实现决定。 */
  def put(token: SessionToken, session: CachedSession): IO[Unit]
  /** 删除缓存令牌，令牌不存在也应视为成功。 */
  def delete(token: SessionToken): IO[Unit]

/** 会话缓存工厂和空实现。 */
object SessionCache:
  val noop: SessionCache = new SessionCache:
    override def get(token: SessionToken): IO[Option[CachedSession]] = IO.pure(None)
    override def put(token: SessionToken, session: CachedSession): IO[Unit] = IO.unit
    override def delete(token: SessionToken): IO[Unit] = IO.unit
