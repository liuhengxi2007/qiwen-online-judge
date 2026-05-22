package shared.model



import java.time.Instant

final case class AuditFields(createdAt: Instant, updatedAt: Instant)
