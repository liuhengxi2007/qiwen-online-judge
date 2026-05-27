package shared.objects



import java.time.Instant

final case class AuditFields(createdAt: Instant, updatedAt: Instant)
