/**
 * 后端通用返回结构
 */
export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

/**
 * MyBatis Flex 分页结构
 */
export interface Page<T> {
  records: T[]
  pageNumber: number
  pageSize: number
  totalRow: number
  totalPage: number
}

/**
 * 已登录用户（脱敏）
 */
export interface LoginUserVO {
  id?: number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
  updateTime?: string
}

/**
 * 用户视图（脱敏）
 */
export interface UserVO {
  id: number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
}

/**
 * 用户完整信息（管理员）
 */
export interface UserType extends UserVO {
  updateTime?: string
}

export interface UserRegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

export interface UserLoginRequest {
  userAccount: string
  userPassword: string
}

export interface UserAddRequest {
  userAccount: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}

export interface UserUpdateRequest {
  id: number
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}

export interface UserQueryRequest {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  id?: number
  userAccount?: string
  userName?: string
  userRole?: string
}

/**
 * 通用删除请求
 */
export interface DeleteRequest {
  id: number
}

/**
 * 应用视图
 */
export interface AppVO {
  id: number
  appName?: string
  cover?: string
  initPrompt?: string
  codeGenType?: string
  deployKey?: string
  deployedTime?: string
  priority?: number
  userId?: number
  editTime?: string
  createTime?: string
  updateTime?: string
  user?: UserVO
}

export interface AppAddRequest {
  initPrompt: string
  codeGenType?: string
}

export interface AppUpdateRequest {
  id: number
  appName?: string
}

export interface AppAdminUpdateRequest {
  id: number
  appName?: string
  cover?: string
  priority?: number
}

export interface AppQueryRequest {
  current?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  id?: number
  appName?: string
  cover?: string
  initPrompt?: string
  codeGenType?: string
  deployKey?: string
  priority?: number
  userId?: number
}

/**
 * 代码生成模式
 */
export const CODE_GEN_TYPE = {
  HTML: 'html',
  MULTI_FILE: 'multi_file',
  VUE_PROJECT: 'vue_project',
} as const

/**
 * 对话历史消息
 */
export interface ChatHistoryVO {
  id: number
  appId?: number
  messageType: 'user' | 'ai' | 'error'
  content: string
  userId?: number
  createTime?: string
}

/**
 * 对话历史游标分页
 */
export interface ChatHistoryCursorPageVO {
  records: ChatHistoryVO[]
  hasMore: boolean
  nextCursor?: number | null
}

export interface ChatHistoryQueryRequest {
  appId: number
  pageSize?: number
  lastId?: number
}
