import { Table, TableBody, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { usernameValue } from '@/objects/user/Username'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { useI18n } from '@/system/i18n/use-i18n'

import { SiteManageUserRow } from './SiteManageUserRow'
import type { SiteManageModel } from './objects/SiteManageModel'

type SiteManageUserTableProps = {
  model: SiteManageModel
  siteManagerSession: boolean
  hasActiveQuery: boolean
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

/**
 * 站点用户表格区域，处理加载/空状态、用户行和分页展示。
 */
export function SiteManageUserTable({
  model,
  siteManagerSession,
  hasActiveQuery,
  currentPage,
  totalPages,
  onPageChange,
}: SiteManageUserTableProps) {
  const { t } = useI18n()

  if (model.isLoadingUsers) {
    return <p className="text-sm text-stone-500">{t('siteManage.loadingUsers')}</p>
  }

  if (model.users.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
        <p className="text-base font-medium text-stone-900">
          {t(hasActiveQuery ? 'siteManage.noMatchingUsersTitle' : 'siteManage.emptyUsersTitle')}
        </p>
        <p className="mt-2 text-sm leading-7 text-stone-600">
          {t(hasActiveQuery ? 'siteManage.noMatchingUsersDescription' : 'siteManage.emptyUsersDescription')}
        </p>
      </div>
    )
  }

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('common.username')}</TableHead>
            <TableHead>{t('common.displayName')}</TableHead>
            <TableHead>{t('common.email')}</TableHead>
            <TableHead>{t('common.settings')}</TableHead>
            <TableHead>{t('siteManage.siteManager')}</TableHead>
            <TableHead>{t('siteManage.problemManager')}</TableHead>
            <TableHead>{t('siteManage.contestManager')}</TableHead>
            <TableHead className="text-right">{t('common.actions')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {model.users.map((listedUser) => (
            <SiteManageUserRow
              key={usernameValue(listedUser.username)}
              listedUser={listedUser}
              model={model}
              siteManagerSession={siteManagerSession}
            />
          ))}
        </TableBody>
      </Table>
      {totalPages > 1 ? (
        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          previousLabel={t('common.pagination.previous')}
          nextLabel={t('common.pagination.next')}
          tone="stone"
          onPageChange={onPageChange}
        />
      ) : null}
    </>
  )
}
