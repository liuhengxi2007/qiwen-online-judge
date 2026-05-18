import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = process.cwd()

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function normalizeFieldName(field) {
  return field.trim()
}

function extractTsObjectTypeFields(source, typeName) {
  const pattern = new RegExp(`export type ${typeName} = \\{([\\s\\S]*?)\\n\\}`, 'm')
  const match = source.match(pattern)
  if (match) {
    return match[1]
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('//'))
      .map((line) => normalizeFieldName(line.split(':')[0]))
  }

  const aliasPattern = new RegExp(`export type ${typeName} = ([A-Za-z0-9_<>,]+)`, 'm')
  const aliasMatch = source.match(aliasPattern)
  if (!aliasMatch) {
    throw new Error(`Unable to find TypeScript type ${typeName}`)
  }

  return extractTsObjectTypeFields(source, aliasMatch[1])
}

function extractTsUnionLiterals(source, typeName) {
  const pattern = new RegExp(`export type ${typeName} =\\s*([\\s\\S]*?)(?=\\nexport type|$)`)
  const match = source.match(pattern)
  if (!match) {
    throw new Error(`Unable to find TypeScript union ${typeName}`)
  }

  return [...match[1].matchAll(/'([^']+)'/g)].map((entry) => entry[1])
}

function extractScalaCaseClassFields(source, className) {
  const pattern = new RegExp(`final case class ${className}\\(([^)]*)\\)`, 'm')
  const match = source.match(pattern)
  if (!match) {
    throw new Error(`Unable to find Scala case class ${className}`)
  }

  return match[1]
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => normalizeFieldName(part.split(':')[0]))
}

function extractScalaStringCases(source, enumName) {
  const pattern = new RegExp(`object ${enumName}:[\\s\\S]*?def fromDatabase\\(value: String\\): Option\\[${enumName}\\] =[\\s\\S]*?value match([\\s\\S]*?)case _ => None`, 'm')
  const match = source.match(pattern)
  if (!match) {
    throw new Error(`Unable to find Scala enum mappings for ${enumName}`)
  }

  return [...match[1].matchAll(/case "([^"]+)"/g)].map((entry) => entry[1])
}

function extractScalaParseStringCases(source, enumName) {
  const pattern = new RegExp(`object ${enumName}:[\\s\\S]*?def parse\\(raw: String\\): Either\\[String, ${enumName}\\] =[\\s\\S]*?raw match([\\s\\S]*?)case other =>`, 'm')
  const match = source.match(pattern)
  if (!match) {
    throw new Error(`Unable to find Scala parse mappings for ${enumName}`)
  }

  return [...match[1].matchAll(/case "([^"]+)"/g)].map((entry) => entry[1])
}

function extractScalaNestedPayloadFields(source, className) {
  return ['kind', ...extractScalaCaseClassFields(source, className)]
}

function extractScalaPageResponseFields(source) {
  const pattern = /final case class PageResponse\[A\]\(([^)]*)\)/m
  const match = source.match(pattern)
  if (!match) {
    throw new Error('Unable to find Scala PageResponse')
  }

  return match[1]
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => normalizeFieldName(part.split(':')[0]))
}

function assertSameFields(context, expected, actual, errors) {
  const expectedJoined = expected.join(', ')
  const actualJoined = actual.join(', ')
  if (expectedJoined !== actualJoined) {
    errors.push(`${context}: expected [${expectedJoined}] but found [${actualJoined}]`)
  }
}

function run() {
  const errors = []

  const contractShared = read('contracts/shared.ts')
  const contractAuth = read('contracts/auth.ts')
  const contractBlog = read('contracts/blog.ts')
  const contractJudger = read('contracts/judger.ts')
  const contractProblem = read('contracts/problem.ts')
  const contractProblemSet = read('contracts/problemset.ts')
  const contractSubmission = read('contracts/submission.ts')
  const contractUserGroup = read('contracts/usergroup.ts')
  const contractNotification = read('contracts/notification.ts')

  const backendSharedError = read('backend/src/main/scala/domains/shared/model/ErrorResponse.scala')
  const backendSharedSuccess = read('backend/src/main/scala/domains/shared/model/SuccessResponse.scala')
  const backendSharedPagination = read('backend/src/main/scala/domains/shared/model/Pagination.scala')
  const backendSharedLifecycle = read('backend/src/main/scala/domains/shared/model/ResourceLifecycle.scala')
  const backendSharedBaseAccess = read('backend/src/main/scala/domains/shared/access/BaseAccess.scala')
  const backendSharedResourceAccessPolicy = read('backend/src/main/scala/domains/shared/access/ResourceAccessPolicy.scala')

  const authFiles = {
    LoginRequest: read('backend/src/main/scala/domains/auth/model/LoginRequest.scala'),
    LoginResponse: read('backend/src/main/scala/domains/auth/model/LoginResponse.scala'),
    RegisterRequest: read('backend/src/main/scala/domains/auth/model/RegisterRequest.scala'),
    RegisterResponse: read('backend/src/main/scala/domains/auth/model/RegisterResponse.scala'),
    SessionResponse: read('backend/src/main/scala/domains/auth/model/SessionResponse.scala'),
  }

  const userFiles = {
    AuthUserListItem: read('backend/src/main/scala/domains/user/model/AuthUserListItem.scala'),
    UpdateOwnProfileRequest: read('backend/src/main/scala/domains/user/model/UpdateOwnProfileRequest.scala'),
    UpdateOwnPreferencesRequest: read('backend/src/main/scala/domains/user/model/UpdateOwnPreferencesRequest.scala'),
    UpdateOwnAccountRequest: read('backend/src/main/scala/domains/user/model/UpdateOwnAccountRequest.scala'),
    UpdateManagedUserProfileRequest: read('backend/src/main/scala/domains/user/model/UpdateManagedUserProfileRequest.scala'),
    UpdateManagedUserPreferencesRequest: read('backend/src/main/scala/domains/user/model/UpdateManagedUserPreferencesRequest.scala'),
    UpdateManagedUserAccountRequest: read('backend/src/main/scala/domains/user/model/UpdateManagedUserAccountRequest.scala'),
    UpdateUserPermissionsRequest: read('backend/src/main/scala/domains/user/model/UpdateUserPermissionsRequest.scala'),
    UserProfileResponse: read('backend/src/main/scala/domains/user/model/UserProfileResponse.scala'),
  }

  const judgerFiles = {
    RegisteredJudgerListItem: read('backend/src/main/scala/domains/judger/model/RegisteredJudgerListItem.scala'),
  }

  const blogFiles = {
    CreateBlogRequest: read('backend/src/main/scala/domains/blog/model/CreateBlogRequest.scala'),
    UpdateBlogRequest: read('backend/src/main/scala/domains/blog/model/UpdateBlogRequest.scala'),
    VoteBlogRequest: read('backend/src/main/scala/domains/blog/model/VoteBlogRequest.scala'),
    CreateBlogCommentRequest: read('backend/src/main/scala/domains/blog/model/CreateBlogCommentRequest.scala'),
    UpdateBlogCommentRequest: read('backend/src/main/scala/domains/blog/model/UpdateBlogCommentRequest.scala'),
    VoteBlogCommentRequest: read('backend/src/main/scala/domains/blog/model/VoteBlogCommentRequest.scala'),
    BlogCommentSummary: read('backend/src/main/scala/domains/blog/model/BlogCommentSummary.scala'),
    BlogSummary: read('backend/src/main/scala/domains/blog/model/BlogSummary.scala'),
    BlogDetail: read('backend/src/main/scala/domains/blog/model/BlogDetail.scala'),
  }

  const problemFiles = {
    CreateProblemRequest: read('backend/src/main/scala/domains/problem/model/CreateProblemRequest.scala'),
    UpdateProblemRequest: read('backend/src/main/scala/domains/problem/model/UpdateProblemRequest.scala'),
    ProblemSummary: read('backend/src/main/scala/domains/problem/model/ProblemSummary.scala'),
    ProblemDetail: read('backend/src/main/scala/domains/problem/model/ProblemDetail.scala'),
  }

  const problemSetFiles = {
    CreateProblemSetRequest: read('backend/src/main/scala/domains/problemset/model/CreateProblemSetRequest.scala'),
    UpdateProblemSetRequest: read('backend/src/main/scala/domains/problemset/model/UpdateProblemSetRequest.scala'),
    AddProblemToProblemSetRequest: read('backend/src/main/scala/domains/problemset/model/AddProblemToProblemSetRequest.scala'),
    ProblemSetProblemSummary: read('backend/src/main/scala/domains/problemset/model/ProblemSetProblemSummary.scala'),
    ProblemSetSummary: read('backend/src/main/scala/domains/problemset/model/ProblemSetSummary.scala'),
    ProblemSetDetail: read('backend/src/main/scala/domains/problemset/model/ProblemSetDetail.scala'),
  }

  const submissionFiles = {
    SubmissionLanguage: read('backend/src/main/scala/domains/submission/model/SubmissionLanguage.scala'),
    SubmissionStatus: read('backend/src/main/scala/domains/submission/model/SubmissionStatus.scala'),
    SubmissionVerdict: read('backend/src/main/scala/domains/submission/model/SubmissionVerdict.scala'),
    CreateSubmissionRequest: read('backend/src/main/scala/domains/submission/model/CreateSubmissionRequest.scala'),
    SubmissionSummary: read('backend/src/main/scala/domains/submission/model/SubmissionSummary.scala'),
    SubmissionDetail: read('backend/src/main/scala/domains/submission/model/SubmissionDetail.scala'),
  }

  const userGroupFiles = {
    UserGroupRole: read('backend/src/main/scala/domains/usergroup/model/UserGroupRole.scala'),
    AddUserGroupMemberRole: read('backend/src/main/scala/domains/usergroup/model/AddUserGroupMemberRole.scala'),
    CreateUserGroupRequest: read('backend/src/main/scala/domains/usergroup/model/CreateUserGroupRequest.scala'),
    UpdateUserGroupRequest: read('backend/src/main/scala/domains/usergroup/model/UpdateUserGroupRequest.scala'),
    AddUserGroupMemberRequest: read('backend/src/main/scala/domains/usergroup/model/AddUserGroupMemberRequest.scala'),
    UpdateUserGroupMemberRoleRequest: read('backend/src/main/scala/domains/usergroup/model/UpdateUserGroupMemberRoleRequest.scala'),
    UserGroupMember: read('backend/src/main/scala/domains/usergroup/model/UserGroupMember.scala'),
    UserGroupSummary: read('backend/src/main/scala/domains/usergroup/model/UserGroupSummary.scala'),
    UserGroupDetail: read('backend/src/main/scala/domains/usergroup/model/UserGroupDetail.scala'),
  }

  const notificationFiles = {
    NotificationKind: read('backend/src/main/scala/domains/notification/model/NotificationKind.scala'),
    NotificationPayload: read('backend/src/main/scala/domains/notification/model/NotificationPayload.scala'),
    NotificationSummary: read('backend/src/main/scala/domains/notification/model/NotificationSummary.scala'),
    NotificationListResponse: read('backend/src/main/scala/domains/notification/model/NotificationListResponse.scala'),
    NotificationUnreadCountResponse: read('backend/src/main/scala/domains/notification/model/NotificationUnreadCountResponse.scala'),
  }

  assertSameFields(
    'shared.ErrorResponse',
    extractTsObjectTypeFields(contractShared, 'ErrorResponse'),
    extractScalaCaseClassFields(backendSharedError, 'ErrorResponse'),
    errors,
  )

  assertSameFields(
    'shared.SuccessResponse',
    extractTsObjectTypeFields(contractShared, 'SuccessResponse'),
    extractScalaCaseClassFields(backendSharedSuccess, 'SuccessResponse'),
    errors,
  )

  assertSameFields(
    'shared.PageResponse',
    extractTsObjectTypeFields(contractShared, 'PageResponse<TItem>'),
    extractScalaPageResponseFields(backendSharedPagination),
    errors,
  )

  assertSameFields(
    'shared.BaseAccess',
    extractTsUnionLiterals(contractShared, 'BaseAccess'),
    extractScalaStringCases(backendSharedBaseAccess, 'BaseAccess'),
    errors,
  )

  assertSameFields(
    'shared.ResourceAccessPolicy',
    extractTsObjectTypeFields(contractShared, 'ResourceAccessPolicy'),
    extractScalaCaseClassFields(backendSharedResourceAccessPolicy, 'ResourceAccessPolicy'),
    errors,
  )

  const authMappings = [
    ['LoginRequest', 'LoginRequest'],
    ['LoginResponse', 'LoginResponse'],
    ['RegisterRequest', 'RegisterRequest'],
    ['RegisterResponse', 'RegisterResponse'],
    ['SessionResponse', 'SessionResponse'],
  ]

  for (const [contractType, scalaType] of authMappings) {
    assertSameFields(
      `auth.${contractType}`,
      extractTsObjectTypeFields(contractAuth, contractType),
      extractScalaCaseClassFields(authFiles[scalaType], scalaType),
      errors,
    )
  }

  const userMappings = [
    ['AuthUserListItem', 'AuthUserListItem'],
    ['UpdateOwnProfileRequest', 'UpdateOwnProfileRequest'],
    ['UpdateOwnPreferencesRequest', 'UpdateOwnPreferencesRequest'],
    ['UpdateOwnAccountRequest', 'UpdateOwnAccountRequest'],
    ['UpdateManagedUserProfileRequest', 'UpdateManagedUserProfileRequest'],
    ['UpdateManagedUserPreferencesRequest', 'UpdateManagedUserPreferencesRequest'],
    ['UpdateManagedUserAccountRequest', 'UpdateManagedUserAccountRequest'],
    ['UpdateUserPermissionsRequest', 'UpdateUserPermissionsRequest'],
    ['UserProfileResponse', 'UserProfileResponse'],
  ]

  for (const [contractType, scalaType] of userMappings) {
    assertSameFields(
      `user.${contractType}`,
      extractTsObjectTypeFields(contractAuth, contractType),
      extractScalaCaseClassFields(userFiles[scalaType], scalaType),
      errors,
    )
  }

  assertSameFields(
    'judger.RegisteredJudgerListItem',
    extractTsObjectTypeFields(contractJudger, 'RegisteredJudgerListItem'),
    extractScalaCaseClassFields(judgerFiles.RegisteredJudgerListItem, 'RegisteredJudgerListItem'),
    errors,
  )

  const blogMappings = [
    ['CreateBlogRequest', 'CreateBlogRequest'],
    ['UpdateBlogRequest', 'UpdateBlogRequest'],
    ['VoteBlogRequest', 'VoteBlogRequest'],
    ['CreateBlogCommentRequest', 'CreateBlogCommentRequest'],
    ['UpdateBlogCommentRequest', 'UpdateBlogCommentRequest'],
    ['VoteBlogCommentRequest', 'VoteBlogCommentRequest'],
    ['BlogCommentSummary', 'BlogCommentSummary'],
    ['BlogSummary', 'BlogSummary'],
    ['BlogDetail', 'BlogDetail'],
  ]

  for (const [contractType, scalaType] of blogMappings) {
    assertSameFields(
      `blog.${contractType}`,
      extractTsObjectTypeFields(contractBlog, contractType),
      extractScalaCaseClassFields(blogFiles[scalaType], scalaType),
      errors,
    )
  }

  const problemMappings = [
    ['CreateProblemRequest', 'CreateProblemRequest'],
    ['UpdateProblemRequest', 'UpdateProblemRequest'],
    ['ProblemSummary', 'ProblemSummary'],
    ['ProblemDetail', 'ProblemDetail'],
  ]

  for (const [contractType, scalaType] of problemMappings) {
    assertSameFields(
      `problem.${contractType}`,
      extractTsObjectTypeFields(contractProblem, contractType),
      extractScalaCaseClassFields(problemFiles[scalaType], scalaType),
      errors,
    )
  }

  const problemSetMappings = [
    ['CreateProblemSetRequest', 'CreateProblemSetRequest'],
    ['UpdateProblemSetRequest', 'UpdateProblemSetRequest'],
    ['AddProblemToProblemSetRequest', 'AddProblemToProblemSetRequest'],
    ['ProblemSetProblemSummary', 'ProblemSetProblemSummary'],
    ['ProblemSetSummary', 'ProblemSetSummary'],
    ['ProblemSetDetail', 'ProblemSetDetail'],
  ]

  for (const [contractType, scalaType] of problemSetMappings) {
    assertSameFields(
      `problemset.${contractType}`,
      extractTsObjectTypeFields(contractProblemSet, contractType),
      extractScalaCaseClassFields(problemSetFiles[scalaType], scalaType),
      errors,
    )
  }

  assertSameFields(
    'submission.SubmissionLanguage',
    extractTsUnionLiterals(contractSubmission, 'SubmissionLanguage'),
    extractScalaStringCases(submissionFiles.SubmissionLanguage, 'SubmissionLanguage'),
    errors,
  )

  assertSameFields(
    'submission.SubmissionStatus',
    extractTsUnionLiterals(contractSubmission, 'SubmissionStatus'),
    extractScalaStringCases(submissionFiles.SubmissionStatus, 'SubmissionStatus'),
    errors,
  )

  assertSameFields(
    'submission.SubmissionVerdict',
    extractTsUnionLiterals(contractSubmission, 'SubmissionVerdict'),
    extractScalaStringCases(submissionFiles.SubmissionVerdict, 'SubmissionVerdict'),
    errors,
  )

  const submissionMappings = [
    ['CreateSubmissionRequest', 'CreateSubmissionRequest'],
    ['SubmissionSummary', 'SubmissionSummary'],
    ['SubmissionDetail', 'SubmissionDetail'],
  ]

  for (const [contractType, scalaType] of submissionMappings) {
    assertSameFields(
      `submission.${contractType}`,
      extractTsObjectTypeFields(contractSubmission, contractType),
      extractScalaCaseClassFields(submissionFiles[scalaType], scalaType),
      errors,
    )
  }

  assertSameFields(
    'usergroup.UserGroupRole',
    extractTsUnionLiterals(contractUserGroup, 'UserGroupRole'),
    extractScalaStringCases(userGroupFiles.UserGroupRole, 'UserGroupRole'),
    errors,
  )

  assertSameFields(
    'usergroup.AddUserGroupMemberRole',
    extractTsUnionLiterals(contractUserGroup, 'AddUserGroupMemberRole'),
    extractScalaStringCases(userGroupFiles.AddUserGroupMemberRole, 'AddUserGroupMemberRole'),
    errors,
  )

  const userGroupMappings = [
    ['CreateUserGroupRequest', 'CreateUserGroupRequest'],
    ['UpdateUserGroupRequest', 'UpdateUserGroupRequest'],
    ['AddUserGroupMemberRequest', 'AddUserGroupMemberRequest'],
    ['UpdateUserGroupMemberRoleRequest', 'UpdateUserGroupMemberRoleRequest'],
    ['UserGroupMember', 'UserGroupMember'],
    ['UserGroupSummary', 'UserGroupSummary'],
    ['UserGroupDetail', 'UserGroupDetail'],
  ]

  for (const [contractType, scalaType] of userGroupMappings) {
    assertSameFields(
      `usergroup.${contractType}`,
      extractTsObjectTypeFields(contractUserGroup, contractType),
      extractScalaCaseClassFields(userGroupFiles[scalaType], scalaType),
      errors,
    )
  }

  assertSameFields(
    'notification.NotificationKind',
    extractTsUnionLiterals(contractNotification, 'NotificationKind'),
    extractScalaParseStringCases(notificationFiles.NotificationKind, 'NotificationKind'),
    errors,
  )

  assertSameFields(
    'notification.BlogReplyNotificationPayload',
    extractTsObjectTypeFields(contractNotification, 'BlogReplyNotificationPayload'),
    extractScalaNestedPayloadFields(notificationFiles.NotificationPayload, 'BlogReply'),
    errors,
  )

  const notificationMappings = [
    ['NotificationSummary', 'NotificationSummary'],
    ['NotificationListResponse', 'NotificationListResponse'],
    ['NotificationUnreadCountResponse', 'NotificationUnreadCountResponse'],
  ]

  for (const [contractType, scalaType] of notificationMappings) {
    assertSameFields(
      `notification.${contractType}`,
      extractTsObjectTypeFields(contractNotification, contractType),
      extractScalaCaseClassFields(notificationFiles[scalaType], scalaType),
      errors,
    )
  }

  if (errors.length > 0) {
    console.error('Contract alignment check failed:\n')
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exitCode = 1
    return
  }

  console.log('Contract alignment check passed.')
}

run()
