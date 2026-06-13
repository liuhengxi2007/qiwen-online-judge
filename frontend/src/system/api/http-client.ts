import { messages, fallbackLocale, resolveLocale, translateMessage } from '@/system/i18n/messages'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'

/**
 * HTTP 客户端统一暴露的错误分类，用于页面层区分登录、权限、资源不存在和通用请求失败。
 */
export type HttpClientErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'http'

/**
 * API 调用方可提供的响应解码器；输入是未可信 JSON，输出是领域响应类型。
 */
export type ResponseDecoder<T> = (value: unknown) => T

const httpClientErrorName = 'HttpClientError'

/**
 * 带接口消息码和参数的请求错误；保留原生 Error 形态，额外携带可被页面或全局处理器识别的分类。
 */
export type HttpClientError = Error & {
  readonly kind: HttpClientErrorKind
  readonly code?: string
  readonly params?: Record<string, ApiMessageParam>
}

/**
 * 后端 API 消息响应的最小结构，既用于成功提示，也用于错误响应的本地化解码。
 */
type ApiMessageResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}

/**
 * 创建统一 HTTP 错误对象；输入为已归类的状态、用户可见文案和可选消息参数，输出可被类型守卫识别。
 */
export function createHttpClientError(
  kind: HttpClientErrorKind,
  message: string,
  code?: string,
  params?: Record<string, ApiMessageParam>,
): HttpClientError {
  const error = Object.assign(Error(message), {
    name: httpClientErrorName,
    kind,
    code,
    params,
  })

  return error as HttpClientError
}

/**
 * 判断未知异常是否来自本客户端；只依赖 Error 名称和受控 kind，避免误吞普通运行时错误。
 */
export function isHttpClientError(error: unknown): error is HttpClientError {
  return error instanceof Error && isRecord(error) && error.name === httpClientErrorName && isHttpClientErrorKind(error.kind)
}

/**
 * 收窄 HTTP 错误分类字符串，防止外部对象伪造任意 kind 进入页面错误分支。
 */
function isHttpClientErrorKind(value: unknown): value is HttpClientErrorKind {
  return value === 'unauthorized' || value === 'forbidden' || value === 'not-found' || value === 'http'
}

/**
 * 从失败响应中读取用户可见错误文案；会克隆 Response 读取 JSON 和文本，失败时回退到调用方提供的默认文案。
 */
export async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const jsonResponse = response.clone()
  const textResponse = response.clone()
  const errorData = decodeApiMessageResponse(await jsonResponse.json().catch(() => null))

  if (errorData) {
    const translated = translateApiMessage(errorData)
    if (translated) {
      return translated
    }
  }

  const text = (await textResponse.text().catch(() => '')).trim()
  if (text) {
    return text
  }

  return `${fallback} (HTTP ${response.status})`
}

/**
 * 将未知 JSON 解码为后端消息结构；输入不可信时返回 null，不抛错，供请求错误处理路径安全调用。
 */
export function decodeApiMessageResponse(value: unknown): ApiMessageResponse | null {
  if (!isRecord(value)) {
    return null
  }

  const code = value.code === null || typeof value.code === 'string' ? value.code : undefined
  const message = value.message === null || typeof value.message === 'string' ? value.message : undefined
  const params =
    isRecord(value.params) && Object.values(value.params).every(isApiMessageParam)
      ? (value.params as Record<string, ApiMessageParam>)
      : undefined

  if (code === undefined || message === undefined || params === undefined) {
    return null
  }

  return { code, message, params }
}

/**
 * 判断响应是否只有后端成功消息的三个字段，用于避免把业务对象误当作成功提示包装体。
 */
function isExactSuccessResponse(value: unknown): boolean {
  if (!isRecord(value)) {
    return false
  }

  const keys = Object.keys(value).sort()
  return keys.length === 3 && keys[0] === 'code' && keys[1] === 'message' && keys[2] === 'params'
}

/**
 * 将未知值收窄为可索引对象；只做运行时形态判断，不代表字段已经可信。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

/**
 * 校验 API 消息插值参数，确保后端返回的 kind/value 组合能安全交给本地化模板。
 */
function isApiMessageParam(value: unknown): value is ApiMessageParam {
  if (!isRecord(value) || typeof value.kind !== 'string' || !('value' in value)) {
    return false
  }

  switch (value.kind) {
    case 'text':
      return typeof value.value === 'string'
    case 'int':
    case 'long':
      return typeof value.value === 'number' && Number.isSafeInteger(value.value)
    case 'bool':
      return typeof value.value === 'boolean'
    default:
      return false
  }
}

/**
 * 将后端强类型消息参数转为前端翻译模板支持的值；布尔值转字符串以保持模板替换稳定。
 */
function toTranslationParams(params: Record<string, ApiMessageParam>): Record<string, string | number> {
  return Object.fromEntries(
    Object.entries(params).map(([key, value]) => {
      switch (value.kind) {
        case 'text':
        case 'int':
        case 'long':
          return [key, value.value]
        case 'bool':
          return [key, String(value.value)]
      }
    }),
  )
}

/**
 * 解码通用成功响应；无有效 code/message 时抛出格式错误，避免页面展示无意义成功提示。
 */
export function decodeSuccessResponse(value: unknown): SuccessResponse {
  const response = decodeApiMessageResponse(value)
  if (!response || (!response.code && !response.message)) {
    throw new Error(translateMessage('common.error.invalidSuccessPayload'))
  }

  const data: ApiMessageResponse = {
    code: response.code,
    message: response.message,
    params: response.params,
  }

  return {
    message: translateApiMessage(data) ?? data.message ?? translateMessage('common.success.generic'),
    code: response.code,
    params: response.params,
  }
}

/**
 * 检查当前语言或回退语言是否存在指定翻译键，避免把未知后端 code 当作可展示文案。
 */
function hasTranslation(key: string): boolean {
  const locale = resolveLocale()
  return key in messages[locale] || key in messages[fallbackLocale]
}

/**
 * 将 API 消息响应转成用户可见文案；优先使用本地翻译，缺失时才展示后端 message。
 */
function translateApiMessage(data: ApiMessageResponse): string | null {
  if (data.code && hasTranslation(data.code)) {
    return translateMessage(data.code, toTranslationParams(data.params))
  }

  if (data.message) {
    return data.message
  }

  return null
}

/**
 * 发送带同源凭据的 JSON 请求并统一处理 HTTP 状态；成功时返回业务数据或成功提示，失败时抛出 HttpClientError。
 */
export async function requestJson<T>(
  input: RequestInfo,
  init?: RequestInit,
  decodeResponse?: ResponseDecoder<T>,
): Promise<T> {
  const response = await fetch(input, {
    credentials: 'same-origin',
    ...init,
  })

  if (response.status === 401) {
    const message = await readErrorMessage(response, translateMessage('common.error.authRequired'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw createHttpClientError('unauthorized', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 403) {
    const message = await readErrorMessage(response, translateMessage('common.error.forbidden'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw createHttpClientError('forbidden', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 404) {
    const message = await readErrorMessage(response, translateMessage('common.error.notFound'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw createHttpClientError('not-found', message, data?.code ?? undefined, data?.params)
  }

  if (!response.ok) {
    const message = await readErrorMessage(response, translateMessage('common.error.requestFailed'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw createHttpClientError('http', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 204) {
    // 注意：204/空响应由调用方响应类型约定为 void 或 undefined，这里保留泛型返回接口。
    return undefined as T
  }

  const text = await response.text()
  if (!text.trim()) {
    // 注意：部分命令接口可能返回空 body，调用方需要以 void/undefined 响应类型接收。
    return undefined as T
  }

  const value = JSON.parse(text) as unknown
  const responseValue = isExactSuccessResponse(value) ? decodeSuccessResponse(value) : value

  if (decodeResponse) {
    try {
      return decodeResponse(responseValue)
    } catch (error) {
      const message = error instanceof Error ? error.message : translateMessage('common.error.invalidResponsePayload')
      throw createHttpClientError('http', message)
    }
  }

  return responseValue as T
}

/**
 * 发送 JSON POST 请求；输入体为 undefined 时不写入 body，返回值由调用方指定响应类型。
 */
export function postJson<TResponse>(
  input: RequestInfo,
  body?: unknown,
  decodeResponse?: ResponseDecoder<TResponse>,
): Promise<TResponse> {
  return requestJson<TResponse>(input, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  }, decodeResponse)
}

/**
 * 发送 multipart POST 请求；不设置 Content-Type，让浏览器为 FormData 自动生成边界。
 */
export function postMultipart<TResponse>(
  input: RequestInfo,
  body: FormData,
  decodeResponse?: ResponseDecoder<TResponse>,
): Promise<TResponse> {
  return requestJson<TResponse>(input, {
    method: 'POST',
    body,
  }, decodeResponse)
}
