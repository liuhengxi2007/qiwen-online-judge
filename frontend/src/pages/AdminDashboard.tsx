import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  BookCopy,
  BookMinus,
  BookOpenText,
  BookPlus,
  LibraryBig,
  LogOut,
  RotateCcw,
  ShieldCheck,
} from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '管理员后台首页'
const route = '/library/admin/dashboard'

const operationOptions: MockResultOption[] = [
  {
    id: 'operation-open',
    title: '进入操作页面',
    description: '模拟管理员进入对应功能页面继续处理业务。',
    badge: 'success',
    noticeMessage: '已进入对应功能页面。',
  },
  {
    id: 'operation-empty',
    title: '当前暂无可处理数据',
    description: '模拟进入页面后暂无待借阅、待归还或可编辑内容。',
    badge: 'warning',
  },
  {
    id: 'operation-error',
    title: '加载失败',
    description: '模拟页面初始化失败，提示管理员稍后重试。',
    badge: 'error',
  },
]

const actionCards = [
  {
    id: 'borrow-book',
    title: '借书登记',
    description: '登记读者借阅图书，确认借出信息。',
    icon: BookCopy,
    buttonText: '开始借书',
    targetPath: '/library/admin/borrow/manage',
  },
  {
    id: 'return-book',
    title: '还书处理',
    description: '处理读者归还图书，更新馆藏状态。',
    icon: RotateCcw,
    buttonText: '处理还书',
    targetPath: '/library/admin/return/manage',
  },
  {
    id: 'add-book',
    title: '添加图书',
    description: '录入新书资料，扩充馆藏目录。',
    icon: BookPlus,
    buttonText: '添加图书',
    targetPath: '/library/admin/book/add',
  },
  {
    id: 'delete-book',
    title: '删除图书',
    description: '移除不再保留的图书记录。',
    icon: BookMinus,
    buttonText: '删除图书',
    targetPath: '/library/admin/book/list',
  },
] as const

export default function AdminDashboard() {
  const navigate = useNavigate()
  const { openMockDialog, showNotice } = useMockSystem()
  const [activeActionId, setActiveActionId] = useState<string | null>(null)

  const handleOperationSelect = (
    action: (typeof actionCards)[number],
    option: MockResultOption,
  ) => {
    setActiveActionId(null)

    if (option.id === 'operation-open') {
      navigate(action.targetPath)
      return
    }

    if (option.id === 'operation-empty') {
      if (action.id === 'delete-book') {
        navigate('/library/admin/book/list')
        return
      }

      return
    }

    if (option.id === 'operation-error') {
      return
    }
  }

  const handleActionClick = (action: (typeof actionCards)[number]) => {
    setActiveActionId(action.id)

    openMockDialog({
      pageName,
      route,
      componentName: action.title,
      interactionName: `${action.title}入口`,
      title: `选择${action.title}的处理结果`,
      description: `模拟管理员从后台首页进入“${action.title}”后的页面反馈。`,
      prioritySuggestion: '优先确认核心业务入口的跳转状态，以及无数据或加载失败时的页面反馈。',
      options: operationOptions,
      onSelect: (option) => handleOperationSelect(action, option),
    })
  }

  const handleLogoutSelect = (option: MockResultOption) => {
    if (option.id === 'logout-success') {
      navigate('/library/login')
      return
    }

    if (option.id === 'logout-cancel') {
      showNotice('已取消退出登录。', 'info')
    }
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'session-expiring',
      label: '登录会话即将到期',
      description: '模拟后台首页检测到会话接近过期时的处理结果。',
      dialog: {
        pageName,
        route,
        eventName: '登录会话即将到期',
        interactionName: '页面级会话提醒',
        title: '选择会话到期提醒结果',
        description: '模拟管理员停留在后台首页时收到登录状态提醒后的反馈。',
        options: [
          {
            id: 'session-remind',
            title: '显示续期提醒',
            description: '提示管理员当前登录状态即将到期。',
            badge: 'warning',
            noticeMessage: '当前登录状态即将到期，请尽快完成操作。',
          },
          {
            id: 'session-keep',
            title: '保持当前页面',
            description: '不展示额外提示，继续停留在首页。',
            badge: 'info',
          },
        ],
        onSelect: () => undefined,
      },
    },
    {
      id: 'refresh-dashboard',
      label: '刷新后台首页',
      description: '模拟管理员手动刷新页面后后台首页的加载状态。',
      dialog: {
        pageName,
        route,
        eventName: '刷新后台首页',
        interactionName: '页面刷新',
        title: '选择刷新后的页面状态',
        description: '模拟后台首页刷新后内容重新加载的不同结果。',
        options: [
          {
            id: 'refresh-success',
            title: '刷新成功',
            description: '首页内容正常加载完成。',
            badge: 'success',
            noticeMessage: '后台首页已刷新。',
          },
          {
            id: 'refresh-error',
            title: '刷新失败',
            description: '加载过程异常，提示管理员稍后重试。',
            badge: 'error',
          },
        ],
        onSelect: () => undefined,
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

      <section className="relative mx-auto flex min-h-screen w-full max-w-6xl px-6 py-12 sm:px-8 lg:px-12">
        <div className="w-full space-y-8">
          <header className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div className="space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <ShieldCheck className="size-4 text-slate-700" />
                管理员后台
              </div>
              <div className="space-y-3">
                <h1 className="text-3xl font-semibold tracking-tight text-slate-900 sm:text-4xl">
                  图书馆管理系统 - 管理员后台首页
                </h1>
                <p className="max-w-2xl text-sm leading-7 text-slate-600 sm:text-base">
                  在这里处理图书借还与馆藏维护。
                </p>
              </div>
            </div>

            <Card className="w-full max-w-sm border-white/70 bg-white/82 py-0 shadow-[0_24px_60px_rgba(15,23,42,0.1)] backdrop-blur-xl">
              <CardContent className="flex items-center justify-between gap-4 px-5 py-5">
                <div className="flex items-center gap-4">
                  <Avatar className="size-12 border border-slate-200">
                    <AvatarFallback className="bg-slate-900 text-sm font-semibold text-white">
                      AD
                    </AvatarFallback>
                  </Avatar>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-medium text-slate-900">管理员</p>
                      <Badge variant="outline" className="border-slate-200 text-slate-600">
                        已登录
                      </Badge>
                    </div>
                    <p className="text-sm text-slate-500">可执行图书流通与馆藏维护操作</p>
                  </div>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="rounded-xl border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                  onClick={() =>
                    openMockDialog({
                      pageName,
                      route,
                      componentName: '退出登录',
                      interactionName: '管理员退出登录',
                      title: '选择退出登录结果',
                      description: '模拟管理员从后台首页退出当前登录状态。',
                      prioritySuggestion: '优先确认退出成功后的返回路径，以及取消退出时是否保留当前页面。',
                      options: [
                        {
                          id: 'logout-success',
                          title: '退出成功',
                          description: '结束当前登录状态并返回管理员登录页。',
                          badge: 'success',
                          noticeMessage: '已退出登录。',
                        },
                        {
                          id: 'logout-cancel',
                          title: '取消退出',
                          description: '关闭退出动作，继续停留在后台首页。',
                          badge: 'info',
                        },
                      ],
                      onSelect: handleLogoutSelect,
                    })
                  }
                >
                  <LogOut className="size-4" />
                  退出
                </Button>
              </CardContent>
            </Card>
          </header>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-center gap-3">
                <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
                  <LibraryBig className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-2xl text-slate-900">功能入口</CardTitle>
                  <CardDescription className="mt-1 text-sm text-slate-500">
                    选择需要处理的业务操作
                  </CardDescription>
                </div>
              </div>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <div className="grid gap-4 md:grid-cols-2">
                {actionCards.map((action) => {
                  const Icon = action.icon

                  return (
                    <Card
                      key={action.id}
                      className="border-slate-200/80 bg-[linear-gradient(180deg,rgba(255,255,255,0.96),rgba(248,250,252,0.98))] py-0 shadow-sm"
                    >
                      <CardContent className="space-y-5 px-5 py-5">
                        <div className="flex items-start justify-between gap-4">
                          <div className="space-y-2">
                            <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
                              <Icon className="size-5" />
                            </div>
                            <div className="space-y-1">
                              <h2 className="text-lg font-semibold text-slate-900">{action.title}</h2>
                              <p className="text-sm leading-6 text-slate-500">{action.description}</p>
                            </div>
                          </div>
                          {activeActionId === action.id ? (
                            <Badge className="bg-slate-900 text-white hover:bg-slate-900">处理中</Badge>
                          ) : null}
                        </div>

                        <Button
                          type="button"
                          className="h-11 w-full rounded-xl bg-slate-900 text-white hover:bg-slate-800"
                          onClick={() => handleActionClick(action)}
                        >
                          {action.buttonText}
                        </Button>
                      </CardContent>
                    </Card>
                  )
                })}
              </div>

              <div className="mt-6 flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-4 text-sm text-slate-600">
                <BookOpenText className="size-4 shrink-0 text-slate-500" />
                请根据当前业务选择相应操作入口。
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
