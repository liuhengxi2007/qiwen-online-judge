import { Settings2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue } from '@/objects/user/DisplayName'
import { useI18n } from '@/system/i18n/use-i18n'

import { SiteManageUserSearchToolbar } from './SiteManageUserSearchToolbar'
import { SiteManageUserTable } from './SiteManageUserTable'
import type { SiteManageUserCardProps } from './types'

/**
 * 站点管理用户卡片，展示用户搜索、状态消息、用户表格和分页控件。
 */
export function SiteManageUserCard({
  model,
  siteManagerSession,
  queryInput,
  hasActiveQuery,
  onQueryInputChange,
  onApplyQuery,
  onClearQuery,
  currentPage,
  totalPages,
  onPageChange,
}: SiteManageUserCardProps) {
  // 保留扁平 props：用户管理模型与 URL 查询/分页控制来自不同层，调用端分开传入能体现所有权边界。
  const { t } = useI18n()
  const statusMessage =
    model.notice?.kind === 'permissions_updated'
      ? t('siteManage.message.updatePermissionsSucceeded', {
          username: displayNameValue(model.notice.displayName),
        })
      : model.notice?.kind === 'text'
        ? model.notice.message
        : ''

  return (
    <Card className="border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
            <Settings2 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-stone-950">{t('siteManage.userManagementTitle')}</CardTitle>
            <CardDescription>{t('siteManage.userManagementDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {statusMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{statusMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.userListError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.userListError}</AlertDescription>
          </Alert>
        ) : null}

        <SiteManageUserSearchToolbar
          queryInput={queryInput}
          onApplyQuery={onApplyQuery}
          onClearQuery={onClearQuery}
          onQueryInputChange={onQueryInputChange}
        />
        <SiteManageUserTable
          currentPage={currentPage}
          hasActiveQuery={hasActiveQuery}
          model={model}
          onPageChange={onPageChange}
          siteManagerSession={siteManagerSession}
          totalPages={totalPages}
        />
      </CardContent>
    </Card>
  )
}
