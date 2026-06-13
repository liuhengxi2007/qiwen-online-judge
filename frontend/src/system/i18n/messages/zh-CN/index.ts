import { zhCnCommonMessages } from '@/system/i18n/messages/zh-CN/common'
import { zhCnNavMessages } from '@/system/i18n/messages/zh-CN/nav'
import { zhCnDashboardMessages } from '@/system/i18n/messages/zh-CN/dashboard'
import { zhCnAuthMessages } from '@/system/i18n/messages/zh-CN/auth'
import { zhCnProblemMessages } from '@/system/i18n/messages/zh-CN/problem'
import { zhCnBlogMessages } from '@/system/i18n/messages/zh-CN/blog'
import { zhCnProblemSetMessages } from '@/system/i18n/messages/zh-CN/problemSet'
import { zhCnSubmissionMessages } from '@/system/i18n/messages/zh-CN/submission'
import { zhCnSiteManageMessages } from '@/system/i18n/messages/zh-CN/siteManage'
import { zhCnUserGroupMessages } from '@/system/i18n/messages/zh-CN/userGroup'
import { zhCnUserSettingsMessages } from '@/system/i18n/messages/zh-CN/userSettings'
import { zhCnUserProfileMessages } from '@/system/i18n/messages/zh-CN/userProfile'
import { zhCnForbiddenMessages } from '@/system/i18n/messages/zh-CN/forbidden'
import { zhCnRanklistMessages } from '@/system/i18n/messages/zh-CN/ranklist'
import { zhCnRatingManageMessages } from '@/system/i18n/messages/zh-CN/ratingManage'
import { zhCnResourceAccessMessages } from '@/system/i18n/messages/zh-CN/resourceAccess'
import { zhCnApiMessages } from '@/system/i18n/messages/zh-CN/api'
import { zhCnMessageMessages } from '@/system/i18n/messages/zh-CN/message'
import { zhCnNotificationMessages } from '@/system/i18n/messages/zh-CN/notification'
import { zhCnContestMessages } from '@/system/i18n/messages/zh-CN/contest'
import { zhCnHackMessages } from '@/system/i18n/messages/zh-CN/hack'

/**
 * 简体中文完整消息表，按领域消息字典展开合并后供运行时翻译查找。
 */
export const zhCnMessages: Record<string, string> = {
  ...zhCnCommonMessages,
  ...zhCnNavMessages,
  ...zhCnDashboardMessages,
  ...zhCnAuthMessages,
  ...zhCnProblemMessages,
  ...zhCnBlogMessages,
  ...zhCnProblemSetMessages,
  ...zhCnSubmissionMessages,
  ...zhCnSiteManageMessages,
  ...zhCnUserGroupMessages,
  ...zhCnUserSettingsMessages,
  ...zhCnUserProfileMessages,
  ...zhCnForbiddenMessages,
  ...zhCnRanklistMessages,
  ...zhCnRatingManageMessages,
  ...zhCnResourceAccessMessages,
  ...zhCnApiMessages,
  ...zhCnMessageMessages,
  ...zhCnNotificationMessages,
  ...zhCnContestMessages,
  ...zhCnHackMessages,
}
