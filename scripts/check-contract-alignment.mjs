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
  const contractProblem = read('contracts/problem.ts')
  const contractProblemSet = read('contracts/problemset.ts')
  const contractSubmission = read('contracts/submission.ts')
  const contractUserGroup = read('contracts/usergroup.ts')

  const backendSharedError = read('backend/src/main/scala/domains/shared/model/ErrorResponse.scala')
  const backendSharedSuccess = read('backend/src/main/scala/domains/shared/model/SuccessResponse.scala')
  const backendSharedPagination = read('backend/src/main/scala/domains/shared/model/Pagination.scala')
  const backendSharedLifecycle = read('backend/src/main/scala/domains/shared/model/ResourceLifecycle.scala')
  const backendSharedAccess = read('backend/src/main/scala/domains/shared/access/AccessPolicy.scala')

  const authFiles = {
    AuthUserListItem: read('backend/src/main/scala/domains/auth/model/AuthUserListItem.scala'),
    LoginRequest: read('backend/src/main/scala/domains/auth/model/LoginRequest.scala'),
    LoginResponse: read('backend/src/main/scala/domains/auth/model/LoginResponse.scala'),
    RegisterRequest: read('backend/src/main/scala/domains/auth/model/RegisterRequest.scala'),
    SessionResponse: read('backend/src/main/scala/domains/auth/model/SessionResponse.scala'),
    UpdateOwnSettingsRequest: read('backend/src/main/scala/domains/auth/model/UpdateOwnSettingsRequest.scala'),
    UpdateManagedUserSettingsRequest: read('backend/src/main/scala/domains/auth/model/UpdateManagedUserSettingsRequest.scala'),
    UpdateUserPermissionsRequest: read('backend/src/main/scala/domains/auth/model/UpdateUserPermissionsRequest.scala'),
  }

  const problemModel = read('backend/src/main/scala/domains/problem/model/Problem.scala')
  const problemSetModel = read('backend/src/main/scala/domains/problemset/model/ProblemSet.scala')
  const submissionModel = read('backend/src/main/scala/domains/submission/model/Submission.scala')
  const userGroupModel = read('backend/src/main/scala/domains/usergroup/model/UserGroup.scala')

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
    extractScalaStringCases(backendSharedAccess, 'BaseAccess'),
    errors,
  )

  assertSameFields(
    'shared.ResourceAccessPolicy',
    extractTsObjectTypeFields(contractShared, 'ResourceAccessPolicy'),
    extractScalaCaseClassFields(backendSharedAccess, 'ResourceAccessPolicy'),
    errors,
  )

  const authMappings = [
    ['AuthUserListItem', 'AuthUserListItem'],
    ['LoginRequest', 'LoginRequest'],
    ['LoginResponse', 'LoginResponse'],
    ['RegisterRequest', 'RegisterRequest'],
    ['RegisterResponse', 'LoginResponse'],
    ['SessionResponse', 'SessionResponse'],
    ['UpdateOwnSettingsRequest', 'UpdateOwnSettingsRequest'],
    ['UpdateManagedUserSettingsRequest', 'UpdateManagedUserSettingsRequest'],
    ['UpdateUserPermissionsRequest', 'UpdateUserPermissionsRequest'],
  ]

  for (const [contractType, scalaType] of authMappings) {
    assertSameFields(
      `auth.${contractType}`,
      extractTsObjectTypeFields(contractAuth, contractType),
      extractScalaCaseClassFields(authFiles[scalaType], scalaType),
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
      extractScalaCaseClassFields(problemModel, scalaType),
      errors,
    )
  }

  const problemSetMappings = [
    ['CreateProblemSetRequest', 'CreateProblemSetRequest'],
    ['UpdateProblemSetRequest', 'UpdateProblemSetRequest'],
    ['LinkProblemRequest', 'AddProblemToProblemSetRequest'],
    ['ProblemSetProblemSummary', 'ProblemSetProblemSummary'],
    ['ProblemSetSummary', 'ProblemSetSummary'],
    ['ProblemSetDetail', 'ProblemSetDetail'],
  ]

  for (const [contractType, scalaType] of problemSetMappings) {
    assertSameFields(
      `problemset.${contractType}`,
      extractTsObjectTypeFields(contractProblemSet, contractType),
      extractScalaCaseClassFields(problemSetModel, scalaType),
      errors,
    )
  }

  assertSameFields(
    'submission.SubmissionLanguage',
    extractTsUnionLiterals(contractSubmission, 'SubmissionLanguage'),
    extractScalaStringCases(submissionModel, 'SubmissionLanguage'),
    errors,
  )

  assertSameFields(
    'submission.SubmissionStatus',
    extractTsUnionLiterals(contractSubmission, 'SubmissionStatus'),
    extractScalaStringCases(submissionModel, 'SubmissionStatus'),
    errors,
  )

  assertSameFields(
    'submission.SubmissionVerdict',
    extractTsUnionLiterals(contractSubmission, 'SubmissionVerdict'),
    extractScalaStringCases(submissionModel, 'SubmissionVerdict'),
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
      extractScalaCaseClassFields(submissionModel, scalaType),
      errors,
    )
  }

  assertSameFields(
    'usergroup.UserGroupRole',
    extractTsUnionLiterals(contractUserGroup, 'UserGroupRole'),
    extractScalaStringCases(userGroupModel, 'UserGroupRole'),
    errors,
  )

  assertSameFields(
    'usergroup.AddUserGroupMemberRole',
    extractTsUnionLiterals(contractUserGroup, 'AddUserGroupMemberRole'),
    extractScalaStringCases(userGroupModel, 'AddUserGroupMemberRole'),
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
      extractScalaCaseClassFields(userGroupModel, scalaType),
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
