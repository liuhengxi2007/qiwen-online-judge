package domains.shared.http

import io.circe.{Decoder, DecodingFailure, Encoder, Json}

sealed trait ApiMessageParam

object ApiMessageParam:
  final case class Text(value: String) extends ApiMessageParam
  final case class IntValue(value: Int) extends ApiMessageParam
  final case class LongValue(value: Long) extends ApiMessageParam
  final case class BoolValue(value: Boolean) extends ApiMessageParam

  given Encoder[ApiMessageParam] = Encoder.instance {
    case Text(value) =>
      Json.obj("kind" -> Json.fromString("text"), "value" -> Json.fromString(value))
    case IntValue(value) =>
      Json.obj("kind" -> Json.fromString("int"), "value" -> Json.fromInt(value))
    case LongValue(value) =>
      Json.obj("kind" -> Json.fromString("long"), "value" -> Json.fromLong(value))
    case BoolValue(value) =>
      Json.obj("kind" -> Json.fromString("bool"), "value" -> Json.fromBoolean(value))
  }

  given Decoder[ApiMessageParam] = Decoder.instance { cursor =>
    cursor.downField("kind").as[String].flatMap {
      case "text" => cursor.downField("value").as[String].map(Text(_))
      case "int" => cursor.downField("value").as[Int].map(IntValue(_))
      case "long" => cursor.downField("value").as[Long].map(LongValue(_))
      case "bool" => cursor.downField("value").as[Boolean].map(BoolValue(_))
      case other => Left(DecodingFailure(s"Unsupported ApiMessageParam kind: $other", cursor.history))
    }
  }

type ApiMessageParams = Map[String, ApiMessageParam]

final case class ApiMessage(
  code: String,
  params: ApiMessageParams = Map.empty
)

object ApiMessages:
  val invalidCredentials: ApiMessage = ApiMessage("api.error.auth.invalid_credentials")
  val invalidCurrentPassword: ApiMessage = ApiMessage("api.error.auth.current_password_incorrect")
  val authenticationRequired: ApiMessage = ApiMessage("api.error.auth.required")
  val siteManagerRequired: ApiMessage = ApiMessage("api.error.auth.site_manager_required")
  val adminPermissionsImmutable: ApiMessage = ApiMessage("api.error.auth.admin_permissions_immutable")
  val adminDeleteForbidden: ApiMessage = ApiMessage("api.error.auth.admin_delete_forbidden")
  val cannotDeleteSelf: ApiMessage = ApiMessage("api.error.auth.cannot_delete_self")
  val userNotFound: ApiMessage = ApiMessage("api.error.user.not_found")
  val userHasOwnedResources: ApiMessage = ApiMessage("api.error.user.has_owned_resources")
  val usernameExists: ApiMessage = ApiMessage("api.error.auth.username_exists")
  val usernameConflictsWithGroup: ApiMessage = ApiMessage("api.error.auth.username_conflicts_with_group")
  val problemNotFound: ApiMessage = ApiMessage("api.error.problem.not_found")
  val problemManagerRequired: ApiMessage = ApiMessage("api.error.problem.manager_required")
  val problemSlugExists: ApiMessage = ApiMessage("api.error.problem.slug_exists")
  val problemSlugConflictsWithProblemSet: ApiMessage = ApiMessage("api.error.problem.slug_conflicts_with_problem_set")
  val problemDataFileNotFound: ApiMessage = ApiMessage("api.error.problem.data_file_not_found")
  val problemSetNotFound: ApiMessage = ApiMessage("api.error.problem_set.not_found")
  val problemSetSlugExists: ApiMessage = ApiMessage("api.error.problem_set.slug_exists")
  val problemSetSlugConflictsWithProblem: ApiMessage = ApiMessage("api.error.problem_set.slug_conflicts_with_problem")
  val problemAlreadyLinkedToProblemSet: ApiMessage = ApiMessage("api.error.problem_set.problem_already_linked")
  val problemNotLinkedToProblemSet: ApiMessage = ApiMessage("api.error.problem_set.problem_not_linked")
  val submissionNotFound: ApiMessage = ApiMessage("api.error.submission.not_found")
  val userGroupNotFound: ApiMessage = ApiMessage("api.error.user_group.not_found")
  val userGroupCreationForbidden: ApiMessage = ApiMessage("api.error.user_group.creation_forbidden")
  val userGroupSlugExists: ApiMessage = ApiMessage("api.error.user_group.slug_exists")
  val userGroupSlugConflictsWithUsername: ApiMessage = ApiMessage("api.error.user_group.slug_conflicts_with_username")
  val userAlreadyMemberOfGroup: ApiMessage = ApiMessage("api.error.user_group.user_already_member")
  val groupMemberNotFound: ApiMessage = ApiMessage("api.error.user_group.member_not_found")
  val userGroupOwnerModifyForbidden: ApiMessage = ApiMessage("api.error.user_group.owner_modify_forbidden")
  val ownershipTransferRequired: ApiMessage = ApiMessage("api.error.user_group.transfer_required")
  val userGroupOwnerRemoveForbidden: ApiMessage = ApiMessage("api.error.user_group.owner_remove_forbidden")
  val blogNotFound: ApiMessage = ApiMessage("api.error.blog.not_found")
  val problemOrOwnedPublicBlogNotFound: ApiMessage = ApiMessage("api.error.blog.problem_or_owned_public_not_found")
  val problemBlogLinkManageForbidden: ApiMessage = ApiMessage("api.error.blog.problem_link_manage_forbidden")
  val problemOrPublicBlogNotFound: ApiMessage = ApiMessage("api.error.blog.problem_or_public_not_found")
  val pendingProblemBlogSubmissionNotFound: ApiMessage = ApiMessage("api.error.blog.pending_submission_not_found")
  val problemBlogLinkNotFound: ApiMessage = ApiMessage("api.error.blog.problem_link_not_found")
  val blogCommentNotFound: ApiMessage = ApiMessage("api.error.blog.comment_not_found")
  val directMessageConversationNotFound: ApiMessage = ApiMessage("api.error.message.conversation_not_found")
  val directMessageBlockedByRecipient: ApiMessage = ApiMessage("api.error.message.blocked_by_recipient")
  val directMessageSelfForbidden: ApiMessage = ApiMessage("api.error.message.self_forbidden")
  val directMessageBlockSelfForbidden: ApiMessage = ApiMessage("api.error.message.block_self_forbidden")
  val judgeTokenInvalid: ApiMessage = ApiMessage("api.error.judge.token_invalid")
  val judgerNotFoundOrExpired: ApiMessage = ApiMessage("api.error.judger.not_found_or_expired")

  val loggedOut: ApiMessage = ApiMessage("api.success.auth.logged_out")
  val userDeleted: ApiMessage = ApiMessage("api.success.user.deleted")
  val problemDeleted: ApiMessage = ApiMessage("api.success.problem.deleted")
  val problemSetDeleted: ApiMessage = ApiMessage("api.success.problem_set.deleted")
  val submissionDeleted: ApiMessage = ApiMessage("api.success.submission.deleted")
  val userGroupDeleted: ApiMessage = ApiMessage("api.success.user_group.deleted")
  val blogDeleted: ApiMessage = ApiMessage("api.success.blog.deleted")
  val blogSubmittedToProblem: ApiMessage = ApiMessage("api.success.blog.submitted_to_problem")
  val blogLinkedToProblem: ApiMessage = ApiMessage("api.success.blog.linked_to_problem")
  val problemBlogSubmissionAccepted: ApiMessage = ApiMessage("api.success.blog.problem_submission_accepted")
  val blogUnlinkedFromProblem: ApiMessage = ApiMessage("api.success.blog.unlinked_from_problem")
  val directMessageBlockRemoved: ApiMessage = ApiMessage("api.success.message.block_removed")
  val directMessagesMarkedRead: ApiMessage = ApiMessage("api.success.message.marked_read")
  val judgerHeartbeatRecorded: ApiMessage = ApiMessage("api.success.judger.heartbeat_recorded")
  val judgeResultRecorded: ApiMessage = ApiMessage("api.success.judge.result_recorded")
