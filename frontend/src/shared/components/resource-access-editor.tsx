import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { resourceAccessSummary, type BaseAccess } from '@/shared/domain/resource-lifecycle'

type ResourceAccessEditorProps = {
  accessPolicy: {
    baseAccess: BaseAccess
    viewerGrants: Array<{ kind: 'user'; username: string } | { kind: 'user_group'; slug: string }>
  }
  grantedUsersInput: string
  grantedGroupsInput: string
  onBaseAccessChange: (value: BaseAccess) => void
  onGrantedUsersInputChange: (value: string) => void
  onGrantedGroupsInputChange: (value: string) => void
}

export function ResourceAccessEditor({
  accessPolicy,
  grantedUsersInput,
  grantedGroupsInput,
  onBaseAccessChange,
  onGrantedUsersInputChange,
  onGrantedGroupsInputChange,
}: ResourceAccessEditorProps) {
  return (
    <div className="space-y-5 rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <Label htmlFor="resource-public-access">Public access</Label>
          <p className="text-sm text-slate-600">
            Turn this on to make the resource visible to all signed-in users.
          </p>
        </div>
        <Switch
          id="resource-public-access"
          checked={accessPolicy.baseAccess === 'public'}
          onCheckedChange={(checked) => onBaseAccessChange(checked ? 'public' : 'owner_only')}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="resource-granted-groups">Granted user groups</Label>
        <Textarea
          id="resource-granted-groups"
          value={grantedGroupsInput}
          placeholder={'round-123-testers\ngraph-team'}
          className="min-h-24"
          onChange={(event) => onGrantedGroupsInputChange(event.target.value.toLowerCase())}
        />
        <p className="text-xs text-slate-500">Use commas or new lines. Group slugs must already exist.</p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="resource-granted-users">Granted users</Label>
        <Textarea
          id="resource-granted-users"
          value={grantedUsersInput}
          placeholder={'alice\nbob'}
          className="min-h-24"
          onChange={(event) => onGrantedUsersInputChange(event.target.value.toLowerCase())}
        />
        <p className="text-xs text-slate-500">Use commas or new lines. Usernames must already exist.</p>
      </div>

      <p className="text-sm text-slate-600">{resourceAccessSummary(accessPolicy)}</p>
    </div>
  )
}
