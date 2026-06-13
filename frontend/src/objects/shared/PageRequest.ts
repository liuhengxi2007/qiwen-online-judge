/** 分页请求参数；由列表 API 使用，调用方负责保证页码和页大小符合后端限制。 */
export type PageRequest = {
  page: number
  pageSize: number
}
