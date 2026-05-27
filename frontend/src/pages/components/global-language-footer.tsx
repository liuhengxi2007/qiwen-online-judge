import { LanguageSelect } from '@/pages/components/language-select'

export function GlobalLanguageFooter() {
  return (
    <footer className="bg-transparent px-4 py-6">
      <div className="mx-auto flex max-w-6xl justify-center">
        <LanguageSelect className="w-[8.5rem] rounded-full border-slate-300 bg-white/90" />
      </div>
    </footer>
  )
}
