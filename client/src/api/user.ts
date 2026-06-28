import request from '@/request'
import type {
  BaseResponse,
  LoginUserVO,
  Page,
  UserAddRequest,
  UserLoginRequest,
  UserQueryRequest,
  UserRegisterRequest,
  UserType,
  UserUpdateRequest,
  UserVO,
} from './types'

/**
 * 用户注册
 */
export async function userRegister(body: UserRegisterRequest) {
  const res = await request.post<BaseResponse<number>>('/user/register', body)
  return res.data
}

/**
 * 用户登录
 */
export async function userLogin(body: UserLoginRequest) {
  const res = await request.post<BaseResponse<LoginUserVO>>('/user/login', body)
  return res.data
}

/**
 * 用户注销
 */
export async function userLogout() {
  const res = await request.post<BaseResponse<boolean>>('/user/logout')
  return res.data
}

/**
 * 获取当前登录用户
 */
export async function getCurrentUser() {
  const res = await request.get<BaseResponse<LoginUserVO>>('/user/current')
  return res.data
}

/**
 * 创建用户（管理员）
 */
export async function addUser(body: UserAddRequest) {
  const res = await request.post<BaseResponse<number>>('/user', body)
  return res.data
}

/**
 * 删除用户（管理员）
 */
export async function deleteUser(id: number) {
  const res = await request.delete<BaseResponse<boolean>>(`/user/${id}`)
  return res.data
}

/**
 * 更新用户（管理员）
 */
export async function updateUser(body: UserUpdateRequest) {
  const res = await request.put<BaseResponse<boolean>>('/user', body)
  return res.data
}

/**
 * 根据 id 获取用户（管理员）
 */
export async function getUserById(id: number) {
  const res = await request.get<BaseResponse<UserType>>(`/user/${id}`)
  return res.data
}

/**
 * 分页查询用户（管理员）
 */
export async function listUserVoByPage(body: UserQueryRequest) {
  const res = await request.post<BaseResponse<Page<UserVO>>>('/user/list/page', body)
  return res.data
}
