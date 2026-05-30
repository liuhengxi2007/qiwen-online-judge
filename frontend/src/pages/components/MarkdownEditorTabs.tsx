import { useDeferredValue } from 'react'

import { Textarea } from '@/components/ui/textarea'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useI18n } from '@/system/i18n/use-i18n'

import { MarkdownDocument } from './MarkdownDocument'

export type MarkdownEditorTab = 'write' | 'preview'

type MarkdownEditorTabsProps = {
  textareaId: string
  value: string
  tab: MarkdownEditorTab
  onTabChange: (tab: MarkdownEditorTab) => void
  onValueChange: (value: string) => void
  textareaClassName?: string
}

export function MarkdownEditorTabs({
  textareaId,
  value,
  tab,
  onTabChange,
  onValueChange,
  textareaClassName,
}: MarkdownEditorTabsProps) {
  const { t } = useI18n()
  const deferredValue = useDeferredValue(value)

  return (
    <Tabs value={tab} onValueChange={(nextTab) => onTabChange(nextTab as MarkdownEditorTab)}>
      <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
        <TabsTrigger value="write" className="rounded-xl">
          {t('common.write')}
        </TabsTrigger>
        <TabsTrigger value="preview" className="rounded-xl">
          {t('common.preview')}
        </TabsTrigger>
      </TabsList>
      <TabsContent value="write" className="mt-3">
        <Textarea
          id={textareaId}
          value={value}
          className={textareaClassName}
          onChange={(event) => onValueChange(event.target.value)}
        />
      </TabsContent>
      <TabsContent value="preview" className="mt-3">
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
          {deferredValue.trim() ? (
            <MarkdownDocument content={deferredValue} />
          ) : (
            <p className="text-sm text-slate-500">{t('common.nothingToPreview')}</p>
          )}
        </div>
      </TabsContent>
    </Tabs>
  )
}
