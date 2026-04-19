package domains.shared.model

object ApiMessageCatalog:
  private val errorCodes: Map[String, String] = Map(
    "Invalid username or password." -> "api.error.auth.invalid_credentials",
    "Current password is incorrect." -> "api.error.auth.current_password_incorrect",
    "Authentication required." -> "api.error.auth.required",
    "Site manager permission required." -> "api.error.auth.site_manager_required",
    "The admin account permissions cannot be modified." -> "api.error.auth.admin_permissions_immutable",
    "The admin account cannot be deleted." -> "api.error.auth.admin_delete_forbidden",
    "You cannot delete your own account." -> "api.error.auth.cannot_delete_self",
    "User not found." -> "api.error.user.not_found",
    "User owns existing resources and cannot be deleted." -> "api.error.user.has_owned_resources",
    "Username already exists, including case-only variations." -> "api.error.auth.username_exists",
    "Username conflicts with an existing user group slug." -> "api.error.auth.username_conflicts_with_group",
    "Problem not found." -> "api.error.problem.not_found",
    "Problem manager permission required." -> "api.error.problem.manager_required",
    "Problem slug already exists." -> "api.error.problem.slug_exists",
    "Problem slug conflicts with an existing problem set slug." -> "api.error.problem.slug_conflicts_with_problem_set",
    "Problem data file not found." -> "api.error.problem.data_file_not_found",
    "Problem set not found." -> "api.error.problem_set.not_found",
    "Problem set slug already exists." -> "api.error.problem_set.slug_exists",
    "Problem set slug conflicts with an existing problem slug." -> "api.error.problem_set.slug_conflicts_with_problem",
    "Problem is already linked to this problem set." -> "api.error.problem_set.problem_already_linked",
    "Problem is not linked to this problem set." -> "api.error.problem_set.problem_not_linked",
    "Submission not found." -> "api.error.submission.not_found",
    "User group not found." -> "api.error.user_group.not_found",
    "User group creation is not allowed." -> "api.error.user_group.creation_forbidden",
    "User group slug already exists." -> "api.error.user_group.slug_exists",
    "User group slug conflicts with an existing username." -> "api.error.user_group.slug_conflicts_with_username",
    "User is already a member of this group." -> "api.error.user_group.user_already_member",
    "Group member not found." -> "api.error.user_group.member_not_found",
    "The current owner cannot be modified directly. Transfer ownership instead." -> "api.error.user_group.owner_modify_forbidden",
    "Ownership transfer is required." -> "api.error.user_group.transfer_required",
    "The owner cannot be removed from the group." -> "api.error.user_group.owner_remove_forbidden",
    "Blog not found." -> "api.error.blog.not_found",
    "Problem or owned public blog not found." -> "api.error.blog.problem_or_owned_public_not_found",
    "You cannot manage this problem's blog links." -> "api.error.blog.problem_link_manage_forbidden",
    "Problem or public blog not found." -> "api.error.blog.problem_or_public_not_found",
    "Pending problem blog submission not found." -> "api.error.blog.pending_submission_not_found",
    "Problem blog link not found." -> "api.error.blog.problem_link_not_found",
    "Blog comment not found." -> "api.error.blog.comment_not_found",
    "Judge token is invalid." -> "api.error.judge.token_invalid",
    "Judger not found or lease expired." -> "api.error.judger.not_found_or_expired"
  )

  private val successCodes: Map[String, String] = Map(
    "User deleted." -> "api.success.user.deleted",
    "Problem deleted." -> "api.success.problem.deleted",
    "Problem set deleted." -> "api.success.problem_set.deleted",
    "Submission deleted." -> "api.success.submission.deleted",
    "User group deleted." -> "api.success.user_group.deleted",
    "Blog deleted." -> "api.success.blog.deleted",
    "Blog submitted to problem." -> "api.success.blog.submitted_to_problem",
    "Blog linked to problem." -> "api.success.blog.linked_to_problem",
    "Problem blog submission accepted." -> "api.success.blog.problem_submission_accepted",
    "Blog unlinked from problem." -> "api.success.blog.unlinked_from_problem",
    "Judger heartbeat recorded." -> "api.success.judger.heartbeat_recorded",
    "Judge result recorded." -> "api.success.judge.result_recorded"
  )

  def errorCodeForMessage(message: String): Option[String] =
    errorCodes.get(message)

  def successCodeForMessage(message: String): Option[String] =
    successCodes.get(message)
