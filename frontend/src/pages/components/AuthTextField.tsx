import type { LucideIcon } from 'lucide-react'

import { cn } from '@/components/ui/class-names'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

/**
 * 认证表单文本字段属性，包含标签、图标、值和输入变更回调。
 */
type AuthTextFieldProps = {
  id: string
  label: string
  value: string
  icon: LucideIcon
  onValueChange: (value: string) => void
  type?: string
  autoComplete?: string
  labelClassName?: string
  inputClassName?: string
  iconClassName?: string
}

/**
 * 认证页带图标文本输入组件，统一标签、图标定位和输入值回调。
 * 保留扁平 props 是为了贴近单个输入控件的 JSX 使用方式，调用端能直接看到 id、label、value 和图标。
 */
export function AuthTextField({
  id,
  label,
  value,
  icon: Icon,
  onValueChange,
  type = 'text',
  autoComplete,
  labelClassName,
  inputClassName,
  iconClassName,
}: AuthTextFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id} className={labelClassName}>
        {label}
      </Label>
      <div className="relative">
        <Icon className={cn('pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2', iconClassName)} />
        <Input
          id={id}
          type={type}
          autoComplete={autoComplete}
          value={value}
          className={inputClassName}
          onChange={(event) => onValueChange(event.target.value)}
        />
      </div>
    </div>
  )
}
