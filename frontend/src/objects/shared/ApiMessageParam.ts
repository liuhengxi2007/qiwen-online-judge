export type ApiMessageParam =
  | { kind: 'text'; value: string }
  | { kind: 'int'; value: number }
  | { kind: 'long'; value: number }
  | { kind: 'bool'; value: boolean }