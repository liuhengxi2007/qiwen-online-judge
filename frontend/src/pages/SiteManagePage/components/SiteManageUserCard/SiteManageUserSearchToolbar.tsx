import { type KeyboardEvent } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useI18n } from '@/system/i18n/use-i18n'

type SiteManageUserSearchToolbarProps = {
  queryInput: string
  onQueryInputChange: (value: string) => void
  onApplyQuery: () => void
  onClearQuery: () => void
}

/**
 * 用户管理搜索工具栏，负责查询输入和搜索/清空动作。
 */
export function SiteManageUserSearchToolbar({
  queryInput,
  onQueryInputChange,
  onApplyQuery,
  onClearQuery,
}: SiteManageUserSearchToolbarProps) {
  const { t } = useI18n()

  function applyQueryOnEnter(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') {
      return
    }

    event.preventDefault()
    onApplyQuery()
  }

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
      <div className="flex-1 space-y-2">
        <Label htmlFor="site-manage-user-search">{t('siteManage.userSearchLabel')}</Label>
        <Input
          id="site-manage-user-search"
          value={queryInput}
          placeholder={t('siteManage.userSearchPlaceholder')}
          onChange={(event) => {
            onQueryInputChange(event.target.value)
          }}
          onKeyDown={applyQueryOnEnter}
        />
      </div>
      <div className="flex gap-3">
        <Button type="button" className="rounded-2xl bg-stone-950 text-white hover:bg-stone-800" onClick={onApplyQuery}>
          {t('siteManage.userSearchApply')}
        </Button>
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-stone-300 bg-white"
          onClick={onClearQuery}
        >
          {t('siteManage.userSearchClear')}
        </Button>
      </div>
    </div>
  )
}
