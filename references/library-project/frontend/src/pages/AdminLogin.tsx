import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BookOpenText, KeyRound, UserRound } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  AdminAccountStatuses,
  AdminCredentials,
  type AdminLoginRequest,
  type AdminLoginResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '管理员登录页'
const route = '/library/login'

type LoginResultId = 'login-success' | 'login-password-error' | 'login-account-error'
type LoginMockOption = MockResultOption<LoginResultId, AdminLoginResponse>

const mockOptions: LoginMockOption[] = [
  {
    id: 'login-success',
    title: '登录成功',
    description: '账号密码验证通过，返回 AdminLoginResponse 成功结果并跳转至管理员后台首页。',
    badge: 'success',
    noticeMessage: '登录成功，正在进入管理员后台首页。',
    payload: {
      accountStatus: AdminAccountStatuses.Active,
      redirectTo: '/library/admin/dashboard',
    },
  },
  {
    id: 'login-password-error',
    title: '密码错误',
    description: '密码校验失败，返回 AdminLoginResponse 失败结果并在表单下方显示错误提示。',
    badge: 'error',
    payload: {
      accountStatus: AdminAccountStatuses.PasswordInvalid,
      reason: '密码错误，请重新输入。',
    },
  },
  {
    id: 'login-account-error',
    title: '账号不存在',
    description: '管理员账号未找到，返回 AdminLoginResponse 失败结果并在表单下方显示错误提示。',
    badge: 'error',
    payload: {
      accountStatus: AdminAccountStatuses.NotFound,
      reason: '账号不存在，请检查用户名。',
    },
  },
]

export default function AdminLogin() {
  const navigate = useNavigate()
  const { openMockDialog } = useMockSystem()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleMockResult = (option: LoginMockOption) => {
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
    const request: AdminLoginRequest = {
      credentials: AdminCredentials.fromForm(username, password),
    }

    if (!request.credentials.isComplete) {
      setErrorMessage('请输入用户名和密码。')
      return
    }

    setErrorMessage('')
    setIsSubmitting(true)

    openMockDialog({
      pageName,
      route,
      componentName: '登录按钮',
      interactionName: '管理员登录',
      title: '选择登录处理结果',
      description: '模拟管理员提交账号密码后的不同验证结果。',
      prioritySuggestion: '优先确认登录成功后的跳转，以及账号或密码错误时的提示文案。',
      options: mockOptions,
      onSelect: handleMockResult,
    })
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'enter-submit',
      label: '回车提交登录',
      description: '模拟管理员在密码输入框中按回车键提交表单。',
      dialog: {
        pageName,
        route,
        eventName: '回车提交登录',
        interactionName: '键盘回车触发登录',
        title: '选择回车提交结果',
        description: '模拟按下回车键后，以 AdminLoginRequest 提交登录并返回对应的 AdminLoginResponse。',
        options: mockOptions,
        onSelect: (option) => handleMockResult(option as LoginMockOption),
      },
    },
    {
      id: 'session-expired',
      label: '登录状态失效',
      description: '模拟管理员因会话失效返回登录页后的错误提示。',
      dialog: {
        pageName,
        route,
        eventName: '登录状态失效',
        interactionName: '页面级异常提示',
        title: '选择状态失效后的页面反馈',
        description: '模拟管理员被要求重新登录时的提示信息。',
        options: [
          {
            id: 'session-expired-message',
            title: '显示会话失效提示',
            description: '在错误提示区域展示重新登录文案。',
            badge: 'warning',
          },
          {
            id: 'session-expired-noop',
            title: '保持当前页面',
            description: '不展示额外提示，仅停留在当前登录页。',
            badge: 'info',
          },
        ],
        onSelect: (option: MockResultOption) => {
          setIsSubmitting(false)
          setErrorMessage(option.id === 'session-expired-message' ? '登录状态已失效，请重新登录。' : '')
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
                <BookOpenText className="size-4 text-slate-700" />
                管理员身份验证
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 管理员登录
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  使用管理员账号进入图书馆管理后台。
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
                  <CardTitle className="text-xl text-slate-900">图书馆管理系统 - 管理员登录</CardTitle>
                </div>
              </div>
              <div className="hidden lg:block">
                <CardTitle className="text-2xl text-slate-900">管理员登录</CardTitle>
              </div>
              <CardDescription className="text-sm text-slate-500">
                请输入用户名和密码
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
                  <Label htmlFor="admin-username" className="text-slate-700">
                    用户名
                  </Label>
                  <div className="relative">
                    <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="admin-username"
                      value={username}
                      placeholder="请输入管理员用户名"
                      autoComplete="username"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setUsername(event.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="admin-password" className="text-slate-700">
                    密码
                  </Label>
                  <div className="relative">
                    <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      id="admin-password"
                      type="password"
                      value={password}
                      placeholder="请输入管理员密码"
                      autoComplete="current-password"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setPassword(event.target.value)}
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
                  登录
                </Button>

                <Button
                  type="button"
                  variant="link"
                  className="h-auto w-full px-0 text-sm text-slate-500 hover:text-slate-800"
                  onClick={() => navigate('/library/register')}
                >
                  没有账号？前往注册
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
