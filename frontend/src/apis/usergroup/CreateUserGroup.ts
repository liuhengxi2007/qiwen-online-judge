import type { CreateUserGroupRequest } from '@/objects/usergroup/request/CreateUserGroupRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import {
  fromUserGroupDetailContract,
  toCreateUserGroupRequestContract,
} from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function createUserGroup(request: CreateUserGroupRequest): Promise<UserGroupDetail> {
  return postJson('/api/user-groups', fromUserGroupDetailContract, toCreateUserGroupRequestContract(request))
}
