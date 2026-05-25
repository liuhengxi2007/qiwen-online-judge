import type { CreateUserGroupRequest } from '@/features/usergroup/http/request/CreateUserGroupRequest'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import {
  fromUserGroupDetailContract,
  toCreateUserGroupRequestContract,
} from '@/features/usergroup/http/codec/UserGroupHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createUserGroup(request: CreateUserGroupRequest): Promise<UserGroupDetail> {
  return postJson('/api/user-groups', fromUserGroupDetailContract, toCreateUserGroupRequestContract(request))
}
