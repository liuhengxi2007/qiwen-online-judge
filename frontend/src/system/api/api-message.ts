import { requestJson, type ResponseDecoder } from '@/system/api/http-client'

/**
 * 前端 API message 层允许的 HTTP 方法；当前只承载查询和命令两类请求。
 */
export type APIMethod = 'GET' | 'POST'

/**
 * 领域 API 客户端的消息协议，封装路径、方法和可序列化请求体，响应类型只在类型层使用。
 */
export type APIMessage<Response> = {
  readonly responseType?: Response
  readonly apiPath: string
  readonly method: APIMethod
  readonly decodeResponse?: ResponseDecoder<Response>
  body(): unknown | undefined
}

/**
 * 需要登录态的 API 消息别名；运行时仍由同源 cookie 携带会话，类型名表达调用边界。
 */
export type APIWithSessionMessage<Response> = APIMessage<Response>

/**
 * 规范化 API 路径，接受带或不带 /api 前缀的消息路径，输出可直接交给 fetch 的绝对路径。
 */
export function apiPath(message: APIMessage<unknown>): string {
  const normalizedPath = message.apiPath.replace(/^\/+/, '')
  return normalizedPath.startsWith('api/') ? `/${normalizedPath}` : `/api/${normalizedPath}`
}

/**
 * 将 API 消息转换为 fetch 初始化参数；有 body 时序列化为 JSON，并始终携带同源凭据。
 */
export function apiRequest(message: APIMessage<unknown>): RequestInit {
  const body = message.body()
  if (body === undefined) {
    return {
      method: message.method,
      credentials: 'same-origin',
    }
  }

  return {
    method: message.method,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  }
}

/**
 * 发送普通 JSON API 消息；网络错误和 HTTP 错误由底层 requestJson 统一转换。
 */
export function sendAPI<Response>(message: APIMessage<Response>): Promise<Response> {
  return requestJson(apiPath(message), apiRequest(message), message.decodeResponse)
}

/**
 * 发送带 FormData 的 API 消息；用于文件上传等 multipart 请求，返回调用方声明的响应类型。
 */
export function sendMultipartAPI<Response>(
  message: APIMessage<Response>,
  body: FormData,
): Promise<Response> {
  return requestJson(apiPath(message), {
    method: message.method,
    credentials: 'same-origin',
    body,
  }, message.decodeResponse)
}
