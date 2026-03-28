export interface AgentFeedbackInput {
  pageName: string
  route: string
  componentOrEventName: string
  interactionName: string
  selectedMockResult: string
  feedbackText: string
  prioritySuggestion: string
}

interface AgentFeedbackResult {
  ok: boolean
  message: string
}

const CONTROL_SERVER_ORIGIN = 'http://127.0.0.1:4311'
const WORKSPACE_TURN_ENDPOINT = `${CONTROL_SERVER_ORIGIN}/api/workspace/turn`

function buildPrompt(input: AgentFeedbackInput) {
  return [
    `frontend -> ${WORKSPACE_TURN_ENDPOINT} -> control server -> codex agent`,
    `页面名称: ${input.pageName}`,
    `当前路由: ${input.route}`,
    `组件名称或事件名称: ${input.componentOrEventName}`,
    `交互名称: ${input.interactionName}`,
    `用户选择的 mock 结果: ${input.selectedMockResult}`,
    `用户输入的反馈文本: ${input.feedbackText}`,
    `建议 agent 优先处理的问题: ${input.prioritySuggestion}`,
  ].join('\n')
}

export async function sendAgentFeedback(input: AgentFeedbackInput): Promise<AgentFeedbackResult> {
  try {
    const response = await fetch(WORKSPACE_TURN_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        prompt: buildPrompt(input),
        mode: 'mock',
      }),
    })

    if (!response.ok) {
      return {
        ok: false,
        message: 'control server 当前不可用，反馈尚未发送给 agent',
      }
    }

    return {
      ok: true,
      message: '反馈已发送给 agent，正在等待进一步处理',
    }
  } catch {
    return {
      ok: false,
      message: 'control server 当前不可用，反馈尚未发送给 agent',
    }
  }
}
