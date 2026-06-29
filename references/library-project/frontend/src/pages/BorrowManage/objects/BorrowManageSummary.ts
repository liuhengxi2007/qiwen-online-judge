export interface BorrowManageSummary {
  readonly readerName: string
  readonly draftCount: number
  readonly activeBorrowCount: number
  readonly blockedRuleCount: number
  readonly warningRuleCount: number
}
