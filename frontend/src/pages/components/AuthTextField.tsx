import type { LucideIcon } from 'lucide-react'

import { cn } from '@/components/ui/class-names'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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
