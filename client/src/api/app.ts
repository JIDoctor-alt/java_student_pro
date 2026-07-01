import request from '@/request'
import type {
  AppAddRequest,
  AppAdminUpdateRequest,
  AppQueryRequest,
  AppUpdateRequest,
  AppVO,
  BaseResponse,
  ChatHistoryCursorPageVO,
  ChatHistoryQueryRequest,
  DeleteRequest,
  Page,
  PromptOptimizeRequest,
} from './types'

/**
 * 后端基础地址（用于 SSE，EventSource 不走 axios 实例）
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

/**
 * 创建应用
 */
export async function addApp(body: AppAddRequest) {
  const res = await request.post<BaseResponse<number>>('/app/add', body)
  return res.data
}

/**
 * 优化应用描述提示词
 */
export async function optimizePrompt(body: PromptOptimizeRequest) {
  const res = await request.post<BaseResponse<string>>('/app/prompt/optimize', body)
  return res.data
}

/**
 * 更新自己的应用
 */
export async function updateApp(body: AppUpdateRequest) {
  const res = await request.post<BaseResponse<boolean>>('/app/update', body)
  return res.data
}

/**
 * 删除自己的应用
 */
export async function deleteApp(body: DeleteRequest) {
  const res = await request.post<BaseResponse<boolean>>('/app/delete', body)
  return res.data
}

/**
 * 查看应用详情
 */
export async function getAppVoById(id: number) {
  const res = await request.get<BaseResponse<AppVO>>('/app/get/vo', { params: { id } })
  return res.data
}

/**
 * 分页查询自己的应用列表
 */
export async function listMyAppVoByPage(body: AppQueryRequest) {
  const res = await request.post<BaseResponse<Page<AppVO>>>('/app/my/list/page/vo', body)
  return res.data
}

/**
 * 分页查询精选应用列表
 */
export async function listGoodAppVoByPage(body: AppQueryRequest) {
  const res = await request.post<BaseResponse<Page<AppVO>>>('/app/good/list/page/vo', body)
  return res.data
}

/**
 * 部署应用，返回可访问地址
 */
export async function deployApp(body: { appId: number }) {
  const res = await request.post<BaseResponse<string>>('/app/deploy', body)
  return res.data
}

/**
 * 游标分页查询应用对话历史
 */
export async function listChatHistory(params: ChatHistoryQueryRequest) {
  const res = await request.get<BaseResponse<ChatHistoryCursorPageVO>>('/app/chat/history', { params })
  return res.data
}

/**
 * 检查应用预览是否就绪
 */
export async function checkPreviewReady(appId: number) {
  const res = await request.get<BaseResponse<boolean>>('/app/preview/ready', { params: { appId } })
  return res.data
}

/**
 * 手动生成应用封面（Selenium 截图预览页）
 */
export async function generateAppCover(appId: number) {
  const res = await request.post<BaseResponse<string>>('/app/cover/generate', null, { params: { appId } })
  return res.data
}

import type { VisualEditContext } from '@/utils/visualEdit'

/**
 * 构造应用维度的 AI 生成 SSE 地址
 */
export function buildChatGenUrl(appId: number, message: string, visualContext?: VisualEditContext | null) {
  let url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`
  if (visualContext?.tagName) {
    url += `&visualContext=${encodeURIComponent(JSON.stringify(visualContext))}`
  }
  return url
}

/**
 * 构造「实时查看应用效果」的预览地址（后端静态服务生成目录）
 */
export function buildPreviewUrl(codeGenType: string, appId: number) {
  if (codeGenType === 'vue_project') {
    return `${API_BASE_URL}/preview/vue_project_${appId}/dist/index.html`
  }
  return `${API_BASE_URL}/preview/${codeGenType}_${appId}/index.html`
}

/**
 * 删除任意应用（管理员）
 */
export async function deleteAppByAdmin(body: DeleteRequest) {
  const res = await request.post<BaseResponse<boolean>>('/app/admin/delete', body)
  return res.data
}

/**
 * 更新任意应用（管理员）
 */
export async function updateAppByAdmin(body: AppAdminUpdateRequest) {
  const res = await request.post<BaseResponse<boolean>>('/app/admin/update', body)
  return res.data
}

/**
 * 分页查询应用列表（管理员）
 */
export async function listAppVoByPageByAdmin(body: AppQueryRequest) {
  const res = await request.post<BaseResponse<Page<AppVO>>>('/app/admin/list/page/vo', body)
  return res.data
}

/**
 * 查看应用详情（管理员）
 */
export async function getAppVoByIdByAdmin(id: number) {
  const res = await request.get<BaseResponse<AppVO>>('/app/admin/get/vo', { params: { id } })
  return res.data
}
