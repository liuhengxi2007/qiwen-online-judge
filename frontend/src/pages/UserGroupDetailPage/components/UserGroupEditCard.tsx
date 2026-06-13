import { PencilLine } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组详情页模型类型别名，供编辑卡片读取草稿、权限和保存状态。
 */
type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

/**
 * 用户组基本信息编辑卡片，仅对可管理用户展示名称和描述表单。
 */
export function UserGroupEditCard({ model }: { model: UserGroupDetailPageModel }) {
  const { t } = useI18n()

  if (!model.canManage) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
            <PencilLine className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.editTitle')}</CardTitle>
            <CardDescription>{t('userGroup.detail.editDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="user-group-name">{t('userGroup.create.name')}</Label>
          <Input id="user-group-name" value={model.name} onChange={(event) => model.setName(event.target.value)} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="user-group-description">{t('userGroup.create.description')}</Label>
          <Textarea
            id="user-group-description"
            value={model.description}
            onChange={(event) => model.setDescription(event.target.value)}
          />
        </div>
        <Button
          type="button"
          className="rounded-2xl bg-sky-300 text-sky-950 hover:bg-sky-400"
          disabled={model.isSaving}
          onClick={() => {
            void model.save()
          }}
        >
          {model.isSaving ? t('userGroup.detail.saving') : t('userGroup.detail.save')}
        </Button>
        {model.saveErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.saveErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.saveSuccessMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{model.saveSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}
      </CardContent>
    </Card>
  )
}
