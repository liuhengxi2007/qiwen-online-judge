package shared.model



final case class PageRequest(page: Int = 1, pageSize: Int = 20):
  def normalized: PageRequest =
    PageRequest(
      page = math.max(1, page),
      pageSize = math.max(1, pageSize)
    )

final case class PageResponse[A](items: List[A], page: Int, pageSize: Int, totalItems: Long)
