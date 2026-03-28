export type MockBadgeTone = 'success' | 'warning' | 'error' | 'info'
export type NoticeTone = 'success' | 'error' | 'info'

export interface MockResultOption<
  TId extends string = string,
  TPayload = unknown,
> {
  id: TId
  title: string
  description: string
  badge: MockBadgeTone
  noticeMessage?: string
  payload?: TPayload
}

export interface MockDialogRequest<
  TOption extends MockResultOption<any, any> = MockResultOption,
> {
  pageName: string
  route: string
  componentName?: string
  eventName?: string
  interactionName: string
  title: string
  description: string
  prioritySuggestion?: string
  options: TOption[]
  onSelect: (option: TOption) => void
}

export interface FeedbackDialogRequest {
  pageName: string
  route: string
  componentName?: string
  eventName?: string
  interactionName: string
  title: string
  description: string
  prioritySuggestion: string
  selectedMockResult?: string
}

export interface MockPageDefinition {
  id: string
  label: string
  route: string
  description: string
}

export interface PageEventDefinition<
  TOption extends MockResultOption<any, any> = MockResultOption,
> {
  id: string
  label: string
  description: string
  dialog: MockDialogRequest<TOption>
}

export interface NoticeState {
  tone: NoticeTone
  message: string
}
