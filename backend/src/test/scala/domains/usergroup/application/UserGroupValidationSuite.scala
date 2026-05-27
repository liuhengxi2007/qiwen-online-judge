package domains.usergroup.application

import domains.user.objects.Username
import domains.usergroup.objects.{AddUserGroupMemberRole, UserGroupDescription, UserGroupName, UserGroupSlug}
import domains.usergroup.objects.request.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupRequest}
import munit.FunSuite

class UserGroupValidationSuite extends FunSuite:

  test("validateCreate trims slug name and description") {
    val request = CreateUserGroupRequest(
      slug = UserGroupSlug(" sample-group "),
      name = UserGroupName(" Sample Group "),
      description = UserGroupDescription(" Description ")
    )

    val result = UserGroupValidation.validateCreate(request)

    assertEquals(
      result,
      Right(
        request.copy(
          slug = UserGroupSlug("sample-group"),
          name = UserGroupName("Sample Group"),
          description = UserGroupDescription("Description")
        )
      )
    )
  }

  test("validateUpdate rejects empty names") {
    val request = UpdateUserGroupRequest(
      name = UserGroupName("   "),
      description = UserGroupDescription("description")
    )

    val result = UserGroupValidation.validateUpdate(request)

    assertEquals(result, Left("User group name is required."))
  }

  test("validateAddMember canonicalizes valid usernames") {
    val request = AddUserGroupMemberRequest(
      username = Username("  Alice_01 "),
      role = AddUserGroupMemberRole.Member
    )

    val result = UserGroupValidation.validateAddMember(request)

    assertEquals(result, Right(request.copy(username = Username("alice_01"))))
  }
