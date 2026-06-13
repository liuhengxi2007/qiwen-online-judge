import { Slot } from '@radix-ui/react-slot'
import {
  Controller,
  FormProvider,
  useFormContext,
  useFormState,
  type ControllerProps,
  type FieldPath,
  type FieldValues,
} from 'react-hook-form'
import type { ComponentProps, HTMLAttributes } from 'react'
import { createContext, useContext, useId } from 'react'

import { Label } from '@/components/ui/label'
import { cn } from '@/components/ui/class-names'

/**
 * react-hook-form Provider 的本地别名，让表单复合组件保持统一导入入口。
 */
const Form = FormProvider

/**
 * 表单字段上下文值，记录当前 FormField 绑定的字段名。
 */
type FormFieldContextValue<
  TFieldValues extends FieldValues = FieldValues,
  TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
> = {
  name: TName
}

/**
 * 当前表单字段名的 React Context；由 FormField 写入，供标签、控件和错误消息读取。
 * FIXME-CN: 默认值通过类型断言伪装成有效上下文，导致 useFormField 里的缺失 Provider 检查无法触发。
 */
const FormFieldContext = createContext<FormFieldContextValue>({} as FormFieldContextValue)

/**
 * 表单字段控制器包装，向子组件提供字段名并委托 react-hook-form Controller 管理值和校验。
 */
function FormField<
  TFieldValues extends FieldValues = FieldValues,
  TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
>({ ...props }: ControllerProps<TFieldValues, TName>) {
  return (
    <FormFieldContext.Provider value={{ name: props.name }}>
      <Controller {...props} />
    </FormFieldContext.Provider>
  )
}

/**
 * 表单项上下文值，保存由 useId 生成的可访问性 id 前缀。
 */
type FormItemContextValue = {
  id: string
}

/**
 * 当前表单项的可访问性 id Context；由 FormItem 写入并贯穿 label/description/message。
 * FIXME-CN: 默认值通过类型断言伪装成有效上下文，FormItem 缺失时会继续生成 undefined 派生 id。
 */
const FormItemContext = createContext<FormItemContextValue>({} as FormItemContextValue)

/**
 * 表单项容器，生成稳定 id 并为标签、说明和错误消息建立关联边界。
 */
function FormItem({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  const id = useId()

  return (
    <FormItemContext.Provider value={{ id }}>
      <div className={cn('space-y-2', className)} {...props} />
    </FormItemContext.Provider>
  )
}

/**
 * 读取当前表单字段状态和可访问性 id；必须位于 FormField/FormItem 组合内部。
 */
function useFormField() {
  const fieldContext = useContext(FormFieldContext)
  const itemContext = useContext(FormItemContext)
  const { getFieldState } = useFormContext()
  const formState = useFormState({ name: fieldContext.name })
  const fieldState = getFieldState(fieldContext.name, formState)

  if (!fieldContext) {
    throw new Error('useFormField should be used within <FormField>')
  }

  const { id } = itemContext

  return {
    id,
    name: fieldContext.name,
    formItemId: `${id}-form-item`,
    formDescriptionId: `${id}-form-item-description`,
    formMessageId: `${id}-form-item-message`,
    ...fieldState,
  }
}

/**
 * 表单标签组件，根据字段错误状态切换危险色并绑定输入控件 id。
 */
function FormLabel({ className, ...props }: ComponentProps<typeof Label>) {
  const { error, formItemId } = useFormField()

  return (
    <Label
      className={cn(error && 'text-destructive', className)}
      htmlFor={formItemId}
      {...props}
    />
  )
}

/**
 * 表单控件插槽，为实际输入组件注入 id、aria-describedby 和 aria-invalid。
 */
function FormControl({ ...props }: ComponentProps<typeof Slot>) {
  const { error, formItemId, formDescriptionId, formMessageId } = useFormField()

  return (
    <Slot
      id={formItemId}
      aria-describedby={!error ? formDescriptionId : `${formDescriptionId} ${formMessageId}`}
      aria-invalid={!!error}
      {...props}
    />
  )
}

/**
 * 表单说明文本组件；无内容时不渲染，避免空描述污染 aria 关联。
 */
function FormDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  const { formDescriptionId } = useFormField()

  if (!props.children) {
    return null
  }

  return (
    <p
      id={formDescriptionId}
      className={cn('text-sm text-muted-foreground', className)}
      {...props}
    />
  )
}

/**
 * 表单错误消息组件，优先展示校验错误，缺失错误时可展示调用方传入内容。
 */
function FormMessage({ className, children, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  const { error, formMessageId } = useFormField()
  const body = error ? String(error?.message ?? '') : children

  if (!body) {
    return null
  }

  return (
    <p
      id={formMessageId}
      className={cn('text-sm font-medium text-destructive', className)}
      {...props}
    >
      {body}
    </p>
  )
}

export {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  useFormField,
}
