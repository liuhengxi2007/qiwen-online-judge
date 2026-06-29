import { enCommonMessages } from '@/system/i18n/messages/en/common'
import { enNavMessages } from '@/system/i18n/messages/en/nav'
import { enDashboardMessages } from '@/system/i18n/messages/en/dashboard'
import { enAuthMessages } from '@/system/i18n/messages/en/auth'
import { enProblemMessages } from '@/system/i18n/messages/en/problem'
import { enBlogMessages } from '@/system/i18n/messages/en/blog'
import { enProblemSetMessages } from '@/system/i18n/messages/en/problemSet'
import { enSubmissionMessages } from '@/system/i18n/messages/en/submission'
import { enSiteManageMessages } from '@/system/i18n/messages/en/siteManage'
import { enUserGroupMessages } from '@/system/i18n/messages/en/userGroup'
import { enUserSettingsMessages } from '@/system/i18n/messages/en/userSettings'
import { enUserProfileMessages } from '@/system/i18n/messages/en/userProfile'
import { enForbiddenMessages } from '@/system/i18n/messages/en/forbidden'
import { enRanklistMessages } from '@/system/i18n/messages/en/ranklist'
import { enRatingManageMessages } from '@/system/i18n/messages/en/ratingManage'
import { enResourceAccessMessages } from '@/system/i18n/messages/en/resourceAccess'
import { enApiMessages } from '@/system/i18n/messages/en/api'
import { enMessageMessages } from '@/system/i18n/messages/en/message'
import { enNotificationMessages } from '@/system/i18n/messages/en/notification'
import { enContestMessages } from '@/system/i18n/messages/en/contest'
import { enHackMessages } from '@/system/i18n/messages/en/hack'

/**
 * 英文完整消息表，按领域消息字典展开合并后供运行时翻译查找。
 */
export const enMessages: Record<string, string> = {
  ...enCommonMessages,
  ...enNavMessages,
  ...enDashboardMessages,
  ...enAuthMessages,
  ...enProblemMessages,
  ...enBlogMessages,
  ...enProblemSetMessages,
  ...enSubmissionMessages,
  ...enSiteManageMessages,
  ...enUserGroupMessages,
  ...enUserSettingsMessages,
  ...enUserProfileMessages,
  ...enForbiddenMessages,
  ...enRanklistMessages,
  ...enRatingManageMessages,
  ...enResourceAccessMessages,
  ...enApiMessages,
  ...enMessageMessages,
  ...enNotificationMessages,
  ...enContestMessages,
  ...enHackMessages,
}
