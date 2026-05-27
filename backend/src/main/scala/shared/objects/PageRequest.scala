package shared.objects

final case class PageRequest(page: Int = 1, pageSize: Int = 20):
  def normalized: PageRequest =
    PageRequest(
      page = math.max(1, page),
      pageSize = math.max(1, pageSize)
    )
