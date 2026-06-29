export type BorrowRuleSeverity = 'pass' | 'warning' | 'block'

export interface BorrowRuleCheck {
  readonly id: string
  readonly title: string
  readonly description: string
  readonly severity: BorrowRuleSeverity
}
