import type {
  CreateUserGroupRequest,
  UserGroupDetail,
} from '@/features/usergroup/domain/usergroup'
import {
  fromUserGroupDetailContract,
  toCreateUserGroupRequestContract,
} from '@/features/usergroup/domain/usergroup'
import { postJson } from '@/shared/api/http-client'

export async function createUserGroup(request: CreateUserGroupRequest): Promise<UserGroupDetail> {
  return postJson('/api/user-groups', fromUserGroupDetailContract, toCreateUserGroupRequestContract(request))
}
