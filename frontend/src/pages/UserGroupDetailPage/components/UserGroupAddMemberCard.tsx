import { ShieldPlus } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { useUserGroupDetailPageModel } from '../hooks/useUserGroupDetailPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组详情页模型类型别名，供新增成员卡片读取新增草稿和操作状态。
 */
type UserGroupDetailPageModel = ReturnType<typeof useUserGroupDetailPageModel>

/**
 * 新增用户组成员卡片，仅对可管理用户展示用户名和角色输入。
 */
export function UserGroupAddMemberCard({ model }: { model: UserGroupDetailPageModel }) {
  const { t } = useI18n()

  if (!model.canManage) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
            <ShieldPlus className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('userGroup.detail.addMemberTitle')}</CardTitle>
            <CardDescription>{t('userGroup.detail.addMemberDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="member-username">{t('common.username')}</Label>
          <Input
            id="member-username"
            value={model.memberUsername}
            onChange={(event) => model.setMemberUsername(event.target.value.toLowerCase())}
          />
        </div>
        <div className="space-y-2">
          <Label>{t('userGroup.detail.roleLabel')}</Label>
          {/* 注意：新增成员只能选择 manager/member，SelectItem 字面值覆盖了这里的联合类型。 */}
          <Select value={model.memberRole} onValueChange={(value) => model.setMemberRole(value as 'manager' | 'member')}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="member">{t('userGroup.detail.role.member')}</SelectItem>
              <SelectItem value="manager">{t('userGroup.detail.role.manager')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <Button
          type="button"
          variant="outline"
          className="rounded-2xl border-slate-300 bg-white"
          disabled={model.isAddingMember}
          onClick={() => {
            void model.addMember()
          }}
        >
          {model.isAddingMember ? t('userGroup.detail.addingMember') : t('userGroup.detail.addMember')}
        </Button>
        {model.addMemberErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.addMemberErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.addMemberSuccessMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{model.addMemberSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}
      </CardContent>
    </Card>
  )
}
