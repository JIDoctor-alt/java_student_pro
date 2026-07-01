import request from '@/request'
import type {
  AiModelConfigUpdateRequest,
  AiModelConfigVO,
  AiModelProviderVO,
  BaseResponse,
} from './types'

/**
 * 获取支持的模型提供商
 */
export async function listAiModelProviders() {
  const res = await request.get<BaseResponse<AiModelProviderVO[]>>('/admin/ai-model/providers')
  return res.data
}

/**
 * 获取当前模型接入配置
 */
export async function getAiModelConfig() {
  const res = await request.get<BaseResponse<AiModelConfigVO>>('/admin/ai-model/config')
  return res.data
}

/**
 * 保存模型接入配置
 */
export async function saveAiModelConfig(body: AiModelConfigUpdateRequest) {
  const res = await request.post<BaseResponse<boolean>>('/admin/ai-model/config', body)
  return res.data
}

/**
 * 测试模型连接
 */
export async function testAiModelConnection(body: {
  providerId: string
  baseUrl?: string
  apiKey?: string
  modelName?: string
}) {
  const res = await request.post<BaseResponse<boolean>>('/admin/ai-model/test', body)
  return res.data
}
