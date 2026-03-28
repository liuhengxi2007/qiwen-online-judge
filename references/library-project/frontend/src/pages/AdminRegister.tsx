import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BookOpenText, KeyRound, ShieldCheck, UserRound } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  AdminAccountStatuses,
  AdminRegistrationDraft,
  type AdminRegisterRequest,
  type AdminRegisterResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '管理员注册页'
const route = '/library/register'

type RegisterResultId = 'register-success' | 'register-account-exists' | 'register-save-failed'
type RegisterMockOption = MockResultOption<RegisterResultId, AdminRegisterResponse>

const mockOptions: RegisterMockOption[] = [
  {
    id: 'register-success',
    title: '注册成功',
    description: '管理员账号创建完成，返回 AdminRegisterResponse 成功结果并跳转至管理员登录页。',
    badge: 'success',
    noticeMessage: '注册成功，请使用新账号登录。',
    payload: {
      accountStatus: AdminAccountStatuses.Active,
      redirectTo: '/library/login',
    },
  },
  {
    id: 'register-account-exists',
    title: '用户名已存在',
    description: '管理员用户名重复，返回失败结果并在表单下方显示错误提示。',
    badge: 'error',
    payload: {
      accountStatus: AdminAccountStatuses.NotFound,
      reason: '用户名已存在，请更换后重试。',
    },
  },
  {
    id: 'register-save-failed',
    title: '注册失败',
    description: '账号保存失败，返回失败结果并在表单下方显示错误提示。',
    badge: 'error',
    payload: {
      accountStatus: AdminAccountStatuses.PasswordInvalid,
      reason: '注册失败，请稍后重试。',
    },
  },
]

export default function AdminRegister() {
  const navigate = useNavigate()
  const { openMockDialog } = useMockSystem()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleMockResult = (option: RegisterMockOption) => {
    setIsSubmitting(false)

    const response = option.payload
    if (!response) {
      return
    }

    if (response.accountStatus === AdminAccountStatuses.Active) {
      setErrorMessage('')
      navigate(response.redirectTo)
      return
    }

    setErrorMessage(response.reason)
  }

  const handleSubmit = () => {
    const request: AdminRegisterRequest = {
      draft: AdminRegistrationDraft.fromForm(username, password, confirmPassword),
    }

    if (!request.draft.isComplete || !request.draft.confirmPassword) {
      setErrorMessage('请完整填写注册信息。')
      return
    }

    if (!request.draft.passwordsMatch) {
      setErrorMessage('两次输入的密码不一致。')
      return
    }

    setErrorMessage('')
    setIsSubmitting(true)

    openMockDialog({
      pageName,
      route,
      componentName: '注册按钮',
      interactionName: '管理员注册',
      title: '选择注册处理结果',
      description: '模拟管理员提交注册信息后的不同处理结果。',
      prioritySuggestion: '优先确认注册成功后的跳转，以及用户名冲突或保存失败时的提示文案。',
      options: mockOptions,
      onSelect: handleMockResult,
    })
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'enter-submit-register',
      label: '回车提交注册',
      description: '模拟管理员在确认密码输入框中按回车键提交表单。',
      dialog: {
        pageName,
        route,
        eventName: '回车提交注册',
        interactionName: '键盘回车触发注册',
        title: '选择回车提交结果',
        description: '模拟按下回车键后，以 AdminRegisterRequest 提交注册并返回对应的 AdminRegisterResponse。',
        options: mockOptions,
        onSelect: (option) => handleMockResult(option as RegisterMockOption),
      },
    },
    {
      id: 'prefill-register-form',
      label: '填充示例账号',
      description: '模拟页面自动填入一组管理员注册信息。',
      dialog: {
        pageName,
        route,
        eventName: '填充示例账号',
        interactionName: '页面级表单填充',
        title: '选择填充结果',
        description: '模拟页面为管理员快速填充一组注册示例数据。',
        options: [
          {
            id: 'prefill-register-form-confirm',
            title: '填充示例信息',
            description: '将用户名和密码字段填入一组可提交的示例内容。',
            badge: 'info',
            noticeMessage: '已填充示例注册信息。',
          },
          {
            id: 'prefill-register-form-cancel',
            title: '保持当前内容',
            description: '不对当前表单内容做任何修改。',
            badge: 'warning',
          },
        ],
        onSelect: (option: MockResultOption) => {
          setIsSubmitting(false)

          if (option.id !== 'prefill-register-form-confirm') {
            return
          }

          setUsername('library_admin')
          setPassword('Admin@123')
          setConfirmPassword('Admin@123')
          setErrorMessage('')
        },
      },
    },
  ]

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(185,208,255,0.36),transparent_36%),linear-gradient(180deg,#f5f7fb_0%,#eef2f7_48%,#e8edf4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.56),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.12),transparent_70%)] opacity-70" />

      <FloatingPageTools
        events={pageEvents}
        onEventSelect={(event) => openMockDialog(event.dialog)}
      />

      <section className="relative mx-auto flex min-h-screen w-full max-w-6xl items-center justify-center px-6 py-16 sm:px-8 lg:px-12">
        <div className="grid w-full max-w-4xl items-center gap-8 lg:grid-cols-[1fr_0.95fr]">
          <div className="hidden lg:block">
            <div className="max-w-md space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <ShieldCheck className="size-4 text-slate-700" />
                管理员账号创建
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 管理员注册
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  创建管理员账号后即可登录系统后台。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-center gap-3 lg:hidden">
                <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
                  <BookOpenText className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-900">图书馆管理系统 - 管理员注册</CardTitle>
                </div>
              </div>
              <div className="hidden lg:block">
                <CardTitle className="text-2xl text-slate-900">管理员注册</CardTitle>
              </div>
              <CardDescription className="text-sm text-slate-500">
                请输入管理员注册信息
              </CardDescription>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <form
                className="space-y-5"
                onSubmit={(event) => {
                  event.preventDefault()
                  handleSubmit()
                }}
              >
                <div className="space-y-2">
                  <Label htmlFor="register-username" className="text-slate-700">
                    用户名
                  </Label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-username"
                      value={username}
                      placeholder="请输入管理员用户名"
                      autoComplete="username"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setUsername(event.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="register-password" className="text-slate-700">
                    密码
                  </Label>
                  <div className="relative">
                    <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-password"
                      type="password"
                      value={password}
                      placeholder="请输入管理员密码"
                      autoComplete="new-password"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setPassword(event.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="register-confirm-password" className="text-slate-700">
                    确认密码
                  </Label>
                  <div className="relative">
                    <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="register-confirm-password"
                      type="password"
                      value={confirmPassword}
                      placeholder="请再次输入管理员密码"
                      autoComplete="new-password"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setConfirmPassword(event.target.value)}
                    />
                  </div>
                </div>

                <div className="min-h-16">
                  {errorMessage ? (
                    <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/90">
                      <AlertDescription className="text-sm text-rose-700">
                        {errorMessage}
                      </AlertDescription>
                    </Alert>
                  ) : null}
                </div>

                <Button
                  type="submit"
                  size="lg"
                  className="h-11 w-full rounded-xl bg-slate-900 text-base font-medium text-white hover:bg-slate-800"
                  disabled={isSubmitting}
                >
                  注册
                </Button>

                <Button
                  type="button"
                  variant="link"
                  className="h-auto w-full px-0 text-sm text-slate-500 hover:text-slate-800"
                  onClick={() => navigate('/library/login')}
                >
                  已有账号？返回登录
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </section>

      <footer className="relative pb-8 text-center text-sm text-slate-500">
        Copyright © 2025 图书馆管理系统
      </footer>
    </main>
  )
}
