package shared.objects

final case class PageResponse[A](items: List[A], page: Int, pageSize: Int, totalItems: Long)
