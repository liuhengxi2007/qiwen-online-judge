import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useI18n } from '@/shared/i18n/use-i18n'

export function LanguageSelect({
  className = 'w-[8.5rem] rounded-full border-slate-300 bg-white',
}: {
  className?: string
}) {
  const { locale, setLocale, t } = useI18n()

  return (
    <Select value={locale} onValueChange={(value) => setLocale(value === 'zh-CN' ? 'zh-CN' : 'en')}>
      <SelectTrigger aria-label={t('common.language')} className={className}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="en">{t('common.language.en')}</SelectItem>
        <SelectItem value="zh-CN">{t('common.language.zh-CN')}</SelectItem>
      </SelectContent>
    </Select>
  )
}
