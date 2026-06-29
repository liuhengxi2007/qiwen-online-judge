/**
 * 题目数据中判题配置文件的固定路径。
 */
export const judgeConfigPath = 'judge.yaml' as const

/**
 * 新建题目数据时提供的 judge.yaml v2 模板，包含传统题、限制、checker 和示例测试点。
 */
export const judgeConfigTemplate = `version: 2
hack: false
roundingScale: 6

mode:
  type: traditional
  role: main

limits:
  timeMs: 1000
  memoryMb: 256

checker:
  type: builtin
  name: exact

aggregation:
  testcases: sum_max_max
  subtasks: sum_max_max

subtasks:
  - label: main
    testcases:
      - label: sample-1
        type: sample
        input: sample/1.in
        answer: sample/1.ans

      - label: "1"
        input: tests/1.in
        answer: tests/1.ans
`

/**
 * judge.yaml v2 支持的聚合策略集合，用于校验 testcase/subtask aggregation 字段。
 */
export const aggregations = new Set(['min_max_max', 'min_sum_max', 'sum_max_max', 'sum_sum_max'])

/**
 * 测试点类型白名单，区分主测试点、样例和 Hack 专用数据。
 */
export const testcaseTypes = new Set(['main', 'sample', 'hack'])

/**
 * 代码角色名格式，只允许 ASCII 字母、数字、下划线和连字符。
 */
export const codeRolePattern = /^[A-Za-z0-9_-]+$/

/**
 * 文本角色名格式，供传统模式 testcase.roles 兼容单个 .txt 后缀。
 */
export const textRolePattern = /^[A-Za-z0-9_-]+\.txt$/
