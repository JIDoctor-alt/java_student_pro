/** API Key 完全未配置 */
export const API_KEY_NOT_CONFIGURED_MESSAGE =
  'AI 模型 API Key 未配置，请前往「模型接入」页面填写 API Key 并选择模型后再试'

/** API Key 已填写但鉴权失败 */
export const API_KEY_INVALID_MESSAGE =
  'AI 模型 API Key 无效或已过期，请前往「模型接入」页面检查并更新 API Key'

/** @deprecated */
export const MODEL_NOT_CONFIGURED_MESSAGE = API_KEY_NOT_CONFIGURED_MESSAGE

function extractJsonObject(message: string): string | null {
  const trimmed = message.trim()
  if (trimmed.startsWith('{')) return trimmed
  const start = message.indexOf('{')
  const end = message.lastIndexOf('}')
  if (start >= 0 && end > start) return message.substring(start, end + 1)
  return null
}

function extractProviderAuthError(message: string): boolean {
  const json = extractJsonObject(message)
  if (!json) return false
  try {
    const root = JSON.parse(json) as { error?: { code?: string; message?: string } }
    const code = (root.error?.code ?? '').toLowerCase()
    const errMsg = (root.error?.message ?? '').toLowerCase()
    if (code.includes('api_key') || code.includes('apikey') || code.includes('auth')) return true
    if (errMsg.includes('api key') || errMsg.includes('apikey') || errMsg.includes('authentication')) {
      return true
    }
  } catch {
    // ignore
  }
  return false
}

function isApiKeyNotConfiguredSignal(message: string): boolean {
  const lower = message.toLowerCase()
  return (
    lower.includes('api key 未配置') ||
    lower.includes('未配置 api key') ||
    lower.includes('no api key provided') ||
    lower.includes("you didn't provide an api key") ||
    lower.includes('missing api key') ||
    lower.includes('请先配置 api key') ||
    lower.includes('deepseek_api_key') ||
    lower.includes('dashscope_api_key')
  )
}

function isApiKeyInvalidSignal(message: string): boolean {
  const lower = message.toLowerCase()
  return (
    lower.includes('incorrect api key') ||
    lower.includes('invalid_api_key') ||
    lower.includes('invalidapikey') ||
    lower.includes('apikey-error') ||
    lower.includes('authentication fails') ||
    lower.includes('invalid api key') ||
    lower.includes('api key not valid') ||
    lower.includes('unauthorized') ||
    lower.includes('401') ||
    lower.includes('403 forbidden') ||
    lower.includes('invalid_request_error') ||
    lower.includes('bearer sk-') ||
    extractProviderAuthError(message)
  )
}

function resolveModelConfigReason(message: string): string | null {
  if (isApiKeyNotConfiguredSignal(message)) return API_KEY_NOT_CONFIGURED_MESSAGE
  if (isApiKeyInvalidSignal(message)) return API_KEY_INVALID_MESSAGE
  return null
}

function looksLikeTechnicalError(message: string): boolean {
  const trimmed = message.trim()
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    return !extractProviderAuthError(message)
  }
  return (
    trimmed.includes('"error"') ||
    trimmed.includes('request_id') ||
    trimmed.includes('Exception') ||
    trimmed.includes('at com.') ||
    trimmed.includes('at java.')
  )
}

/** 将 AI 相关错误转为用户可读提示 */
export function formatAiErrorMessage(message?: string): string {
  if (!message?.trim()) return '生成失败，请稍后重试'
  const text = message.replace(/^生成失败[：:]\s*/, '')
  const modelReason = resolveModelConfigReason(text)
  if (modelReason) return modelReason
  if (looksLikeTechnicalError(text)) return '生成失败，请稍后重试'
  return message
}
