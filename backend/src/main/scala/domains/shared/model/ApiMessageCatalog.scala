package domains.shared.model

object ApiMessageCatalog:
  private val legacyErrors: Map[String, ApiMessage] = Map(
    "Invalid username or password." -> ApiMessages.invalidCredentials,
    "Current password is incorrect." -> ApiMessages.invalidCurrentPassword,
    "Authentication required." -> ApiMessages.authenticationRequired,
    "Site manager permission required." -> ApiMessages.siteManagerRequired,
    "The admin account permissions cannot be modified." -> ApiMessages.adminPermissionsImmutable,
    "The admin account cannot be deleted." -> ApiMessages.adminDeleteForbidden,
    "You cannot delete your own account." -> ApiMessages.cannotDeleteSelf,
    "User not found." -> ApiMessages.userNotFound,
    "User owns existing resources and cannot be deleted." -> ApiMessages.userHasOwnedResources,
    "Username already exists, including case-only variations." -> ApiMessages.usernameExists,
    "Username conflicts with an existing user group slug." -> ApiMessages.usernameConflictsWithGroup,
    "Problem not found." -> ApiMessages.problemNotFound,
    "Problem manager permission required." -> ApiMessages.problemManagerRequired,
    "Problem slug already exists." -> ApiMessages.problemSlugExists,
    "Problem slug conflicts with an existing problem set slug." -> ApiMessages.problemSlugConflictsWithProblemSet,
    "Problem data file not found." -> ApiMessages.problemDataFileNotFound,
    "Problem set not found." -> ApiMessages.problemSetNotFound,
    "Problem set slug already exists." -> ApiMessages.problemSetSlugExists,
    "Problem set slug conflicts with an existing problem slug." -> ApiMessages.problemSetSlugConflictsWithProblem,
    "Problem is already linked to this problem set." -> ApiMessages.problemAlreadyLinkedToProblemSet,
    "Problem is not linked to this problem set." -> ApiMessages.problemNotLinkedToProblemSet,
    "Submission not found." -> ApiMessages.submissionNotFound,
    "User group not found." -> ApiMessages.userGroupNotFound,
    "User group creation is not allowed." -> ApiMessages.userGroupCreationForbidden,
    "User group slug already exists." -> ApiMessages.userGroupSlugExists,
    "User group slug conflicts with an existing username." -> ApiMessages.userGroupSlugConflictsWithUsername,
    "User is already a member of this group." -> ApiMessages.userAlreadyMemberOfGroup,
    "Group member not found." -> ApiMessages.groupMemberNotFound,
    "The current owner cannot be modified directly. Transfer ownership instead." -> ApiMessages.userGroupOwnerModifyForbidden,
    "Ownership transfer is required." -> ApiMessages.ownershipTransferRequired,
    "The owner cannot be removed from the group." -> ApiMessages.userGroupOwnerRemoveForbidden,
    "Blog not found." -> ApiMessages.blogNotFound,
    "Problem or owned public blog not found." -> ApiMessages.problemOrOwnedPublicBlogNotFound,
    "You cannot manage this problem's blog links." -> ApiMessages.problemBlogLinkManageForbidden,
    "Problem or public blog not found." -> ApiMessages.problemOrPublicBlogNotFound,
    "Pending problem blog submission not found." -> ApiMessages.pendingProblemBlogSubmissionNotFound,
    "Problem blog link not found." -> ApiMessages.problemBlogLinkNotFound,
    "Blog comment not found." -> ApiMessages.blogCommentNotFound,
    "Judge token is invalid." -> ApiMessages.judgeTokenInvalid,
    "Judger not found or lease expired." -> ApiMessages.judgerNotFoundOrExpired
  )

  private val legacySuccesses: Map[String, ApiMessage] = Map(
    "Logged out." -> ApiMessages.loggedOut,
    "User deleted." -> ApiMessages.userDeleted,
    "Problem deleted." -> ApiMessages.problemDeleted,
    "Problem set deleted." -> ApiMessages.problemSetDeleted,
    "Submission deleted." -> ApiMessages.submissionDeleted,
    "User group deleted." -> ApiMessages.userGroupDeleted,
    "Blog deleted." -> ApiMessages.blogDeleted,
    "Blog submitted to problem." -> ApiMessages.blogSubmittedToProblem,
    "Blog linked to problem." -> ApiMessages.blogLinkedToProblem,
    "Problem blog submission accepted." -> ApiMessages.problemBlogSubmissionAccepted,
    "Blog unlinked from problem." -> ApiMessages.blogUnlinkedFromProblem,
    "Judger heartbeat recorded." -> ApiMessages.judgerHeartbeatRecorded,
    "Judge result recorded." -> ApiMessages.judgeResultRecorded
  )

  def legacyError(message: String): Option[ApiMessage] =
    legacyErrors.get(message)

  def legacySuccess(message: String): Option[ApiMessage] =
    legacySuccesses.get(message)
