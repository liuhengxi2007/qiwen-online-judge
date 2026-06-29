/**
 * 应用模块主题名；新增主题时需要在 appModuleThemes 中补齐静态 Tailwind class。
 */
export type AppModuleTone =
  | 'amber'
  | 'cyan'
  | 'emerald'
  | 'fuchsia'
  | 'indigo'
  | 'orange'
  | 'red'
  | 'rose'
  | 'sky'

/**
 * 应用模块主题样式，集中维护导航、图标和按钮色彩。
 */
export type AppModuleTheme = {
  navActive: string
  navIdle: string
  icon: string
  button: string
}

/**
 * 模块主题静态 class 表；避免 Tailwind 无法扫描动态拼接出的颜色类。
 */
export const appModuleThemes: Record<AppModuleTone, AppModuleTheme> = {
  amber: {
    navActive: 'bg-amber-300 text-amber-950 hover:bg-amber-400',
    navIdle: 'text-amber-800 hover:bg-amber-50',
    icon: 'bg-amber-100 text-amber-700',
    button: 'bg-amber-300 text-amber-950 hover:bg-amber-400',
  },
  cyan: {
    navActive: 'bg-cyan-300 text-cyan-950 hover:bg-cyan-400',
    navIdle: 'text-cyan-800 hover:bg-cyan-50',
    icon: 'bg-cyan-100 text-cyan-700',
    button: 'bg-cyan-300 text-cyan-950 hover:bg-cyan-400',
  },
  emerald: {
    navActive: 'bg-emerald-300 text-emerald-950 hover:bg-emerald-400',
    navIdle: 'text-emerald-800 hover:bg-emerald-50',
    icon: 'bg-emerald-100 text-emerald-700',
    button: 'bg-emerald-300 text-emerald-950 hover:bg-emerald-400',
  },
  fuchsia: {
    navActive: 'bg-fuchsia-300 text-fuchsia-950 hover:bg-fuchsia-400',
    navIdle: 'text-fuchsia-800 hover:bg-fuchsia-50',
    icon: 'bg-fuchsia-100 text-fuchsia-700',
    button: 'bg-fuchsia-300 text-fuchsia-950 hover:bg-fuchsia-400',
  },
  indigo: {
    navActive: 'bg-indigo-300 text-indigo-950 hover:bg-indigo-400',
    navIdle: 'text-indigo-800 hover:bg-indigo-50',
    icon: 'bg-indigo-100 text-indigo-700',
    button: 'bg-indigo-300 text-indigo-950 hover:bg-indigo-400',
  },
  orange: {
    navActive: 'bg-orange-300 text-orange-950 hover:bg-orange-400',
    navIdle: 'text-orange-800 hover:bg-orange-50',
    icon: 'bg-orange-100 text-orange-700',
    button: 'bg-orange-300 text-orange-950 hover:bg-orange-400',
  },
  red: {
    navActive: 'bg-red-300 text-red-950 hover:bg-red-400',
    navIdle: 'text-red-800 hover:bg-red-50',
    icon: 'bg-red-100 text-red-700',
    button: 'bg-red-300 text-red-950 hover:bg-red-400',
  },
  rose: {
    navActive: 'bg-rose-300 text-rose-950 hover:bg-rose-400',
    navIdle: 'text-rose-800 hover:bg-rose-50',
    icon: 'bg-rose-100 text-rose-700',
    button: 'bg-rose-300 text-rose-950 hover:bg-rose-400',
  },
  sky: {
    navActive: 'bg-sky-300 text-sky-950 hover:bg-sky-400',
    navIdle: 'text-sky-800 hover:bg-sky-50',
    icon: 'bg-sky-100 text-sky-700',
    button: 'bg-sky-300 text-sky-950 hover:bg-sky-400',
  },
}
