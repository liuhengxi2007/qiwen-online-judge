import { Link } from 'react-router-dom'
import { Settings2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useI18n } from '@/system/i18n/use-i18n'

import type { ContestManagePageModel } from './types'

type ContestManageFormCardProps = {
  model: ContestManagePageModel
}

/**
 * 保留表单卡片 props 解构：卡片只消费比赛管理模型，内部按 draft/model 读取字段。
 */
export function ContestManageFormCard({ model }: ContestManageFormCardProps) {
  const { t } = useI18n()
  const draft = model.draft
  if (!draft) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
            <Settings2 className="size-5" />
          </div>
          <CardTitle className="text-xl text-slate-950">{t('contest.manage.basicInfo')}</CardTitle>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="contest-title">{t('contest.create.titleLabel')}</Label>
          <Input id="contest-title" value={draft.title} onChange={(event) => model.setTitle(event.target.value)} />
        </div>

        <div className="grid gap-5 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="contest-start-at">{t('contest.create.startAt')}</Label>
            <Input id="contest-start-at" type="datetime-local" value={draft.startAt} onChange={(event) => model.setStartAt(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="contest-end-at">{t('contest.create.endAt')}</Label>
            <Input id="contest-end-at" type="datetime-local" value={draft.endAt} onChange={(event) => model.setEndAt(event.target.value)} />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="contest-description">{t('contest.create.descriptionLabel')}</Label>
          <MarkdownEditorTabs
            textareaId="contest-description"
            value={draft.description}
            tab={model.descriptionTab}
            onTabChange={model.setDescriptionTab}
            onValueChange={model.setDescription}
            textareaClassName="min-h-48 font-mono"
          />
        </div>

        <ResourceAccessEditor
          accessPolicy={model.accessPolicy}
          onBaseAccessChange={model.setBaseAccess}
          viewer={{
            usersInput: draft.grantedUsersInput,
            groupsInput: draft.grantedGroupsInput,
            onGrantedUsersInputChange: model.setGrantedUsersInput,
            onGrantedGroupsInputChange: model.setGrantedGroupsInput,
          }}
          manager={{
            usersInput: draft.grantedManagerUsersInput,
            groupsInput: draft.grantedManagerGroupsInput,
            onGrantedUsersInputChange: model.setGrantedManagerUsersInput,
            onGrantedGroupsInputChange: model.setGrantedManagerGroupsInput,
          }}
        />

        {model.saveErrorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.saveErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.saveSuccessMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.saveSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            disabled={model.isSaving}
            onClick={() => {
              void model.save()
            }}
          >
            {model.isSaving ? t('contest.manage.saving') : t('contest.manage.save')}
          </Button>
          {model.contest ? (
            <Button asChild type="button" variant="outline" className="rounded-2xl">
              <Link to={`/contests/${contestSlugValue(model.contest.slug)}`}>{t('contest.manage.backToContest')}</Link>
            </Button>
          ) : null}
        </div>
      </CardContent>
    </Card>
  )
}
