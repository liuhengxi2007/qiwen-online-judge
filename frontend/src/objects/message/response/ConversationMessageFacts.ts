/** 会话消息事实；用于前端判断引导状态和自动已读边界。 */
export type ConversationMessageFacts = {
  viewerHasSentMessage: boolean
  otherParticipantMessageCount: number
}
