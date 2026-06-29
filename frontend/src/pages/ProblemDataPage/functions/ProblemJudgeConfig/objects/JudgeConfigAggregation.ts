/**
 * 聚合策略配置，允许根级和 subtask 级分别声明 testcase/subtask 聚合方式。
 */
export type AggregationConfig = {
  testcases?: string
  subtasks?: string
}
