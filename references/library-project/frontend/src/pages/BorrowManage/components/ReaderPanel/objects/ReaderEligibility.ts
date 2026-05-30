export type ReaderEligibilityTone = 'ready' | 'blocked'

export interface ReaderEligibility {
  readonly tone: ReaderEligibilityTone
  readonly title: string
  readonly description: string
}
