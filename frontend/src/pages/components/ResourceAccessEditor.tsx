import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'
import { resourceAccessSummary } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 资源访问编辑器属性，包含当前策略、四类授权输入和对应变更回调。
 */
type ResourceAccessEditorProps = {
  accessPolicy: ResourceVisibilityPolicy
  grantedUsersInput: string
  grantedGroupsInput: string
  grantedManagerUsersInput?: string
  grantedManagerGroupsInput?: string
  onBaseAccessChange: (value: BaseAccess) => void
  onGrantedUsersInputChange: (value: string) => void
  onGrantedGroupsInputChange: (value: string) => void
  onGrantedManagerUsersInputChange?: (value: string) => void
  onGrantedManagerGroupsInputChange?: (value: string) => void
}

/**
 * 资源访问编辑器组件，渲染公开开关、查看授权和可选管理授权输入区。
 */
export function ResourceAccessEditor({
  accessPolicy,
  grantedUsersInput,
  grantedGroupsInput,
  grantedManagerUsersInput,
  grantedManagerGroupsInput,
  onBaseAccessChange,
  onGrantedUsersInputChange,
  onGrantedGroupsInputChange,
  onGrantedManagerUsersInputChange,
  onGrantedManagerGroupsInputChange,
}: ResourceAccessEditorProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-5 rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <Label htmlFor="resource-public-access">{t('resourceAccess.public')}</Label>
          <p className="text-sm text-slate-600">{t('resourceAccess.publicDescription')}</p>
        </div>
        <Switch
          id="resource-public-access"
          checked={accessPolicy.baseAccess === 'public'}
          onCheckedChange={(checked) => onBaseAccessChange(checked ? 'public' : 'restricted')}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="resource-granted-groups">{t('resourceAccess.groups')}</Label>
        <Textarea
          id="resource-granted-groups"
          value={grantedGroupsInput}
          className="min-h-24"
          spellCheck={false}
          onChange={(event) => onGrantedGroupsInputChange(event.target.value.toLowerCase())}
        />
        <p className="text-xs text-slate-500">{t('resourceAccess.groupsHint')}</p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="resource-granted-users">{t('resourceAccess.users')}</Label>
        <Textarea
          id="resource-granted-users"
          value={grantedUsersInput}
          className="min-h-24"
          spellCheck={false}
          onChange={(event) => onGrantedUsersInputChange(event.target.value.toLowerCase())}
        />
        <p className="text-xs text-slate-500">{t('resourceAccess.usersHint')}</p>
      </div>

      {grantedManagerGroupsInput !== undefined && onGrantedManagerGroupsInputChange ? (
        <div className="space-y-2">
          <Label htmlFor="resource-manager-groups">{t('resourceAccess.managerGroups')}</Label>
          <Textarea
            id="resource-manager-groups"
            value={grantedManagerGroupsInput}
            className="min-h-24"
            spellCheck={false}
            onChange={(event) => onGrantedManagerGroupsInputChange(event.target.value.toLowerCase())}
          />
          <p className="text-xs text-slate-500">{t('resourceAccess.managerGroupsHint')}</p>
        </div>
      ) : null}

      {grantedManagerUsersInput !== undefined && onGrantedManagerUsersInputChange ? (
        <div className="space-y-2">
          <Label htmlFor="resource-manager-users">{t('resourceAccess.managerUsers')}</Label>
          <Textarea
            id="resource-manager-users"
            value={grantedManagerUsersInput}
            className="min-h-24"
            spellCheck={false}
            onChange={(event) => onGrantedManagerUsersInputChange(event.target.value.toLowerCase())}
          />
          <p className="text-xs text-slate-500">{t('resourceAccess.managerUsersHint')}</p>
        </div>
      ) : null}

      <p className="text-sm text-slate-600">{resourceAccessSummary(accessPolicy, t)}</p>
    </div>
  )
}
