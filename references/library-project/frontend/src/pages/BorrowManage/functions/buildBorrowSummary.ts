import type { BorrowManageSummary } from '../objects/BorrowManageSummary'
import type { BorrowRuleCheck } from '../objects/BorrowRuleCheck'

interface BuildBorrowSummaryInput {
  readonly readerName: string
  readonly draftCount: number
  readonly activeBorrowCount: number
  readonly ruleChecks: BorrowRuleCheck[]
}

export function buildBorrowSummary({
  readerName,
  draftCount,
  activeBorrowCount,
  ruleChecks,
}: BuildBorrowSummaryInput): BorrowManageSummary {
  return {
    readerName: readerName.trim() || '未选择读者',
    draftCount,
    activeBorrowCount,
    blockedRuleCount: ruleChecks.filter((check) => check.severity === 'block').length,
    warningRuleCount: ruleChecks.filter((check) => check.severity === 'warning').length,
  }
}
