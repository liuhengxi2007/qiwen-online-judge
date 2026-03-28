import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { BookCopy, CalendarClock, Search, Undo2, UserRoundCheck } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  CirculationReviewStatuses,
  ReturnRecordEntity,
  ReturnRecordStatuses,
  type ReturnProcessRequest,
  type ReturnProcessResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '还书管理页'
const route = '/library/admin/return/manage'

type ReturnManageOption = MockResultOption<
  'return-success' | 'return-fine-required' | 'return-error',
  ReturnProcessResponse
>

const initialRecords: ReturnRecordEntity[] = [
  new ReturnRecordEntity('RET-2025-001', '现代数据库系统', '张晨', 'BR-50128', '2025-01-06', '2025-01-20', ReturnRecordStatuses.Pending),
  new ReturnRecordEntity('RET-2025-002', '软件工程实践', '李雨桐', 'BR-50177', '2024-12-28', '2025-01-11', ReturnRecordStatuses.Overdue),
  new ReturnRecordEntity('RET-2025-003', '计算机网络原理', '王哲', 'BR-50204', '2025-01-03', '2025-01-17', ReturnRecordStatuses.Returned, '2025-01-15'),
]

const searchOptions: MockResultOption[] = [
  {
    id: 'search-success',
    title: '检索到借阅记录',
    description: '按当前关键词返回匹配的借阅记录列表。',
    badge: 'success',
    noticeMessage: '已更新还书记录列表。',
  },
  {
    id: 'search-empty',
    title: '未找到匹配记录',
    description: '列表清空，仅保留空状态提示。',
    badge: 'warning',
  },
  {
    id: 'search-error',
    title: '检索失败',
    description: '保留当前列表，并提示系统暂时无法查询。',
    badge: 'error',
  },
]

const returnOptions: ReturnManageOption[] = [
  {
    id: 'return-success',
    title: '还书成功',
    description: '更新当前记录状态为已归还，并返回 ReturnProcessResponse 成功结果。',
    badge: 'success',
    noticeMessage: '还书已办理完成。',
    payload: {
      ok: true,
      reviewStatus: CirculationReviewStatuses.None,
      recordId: 'active-return-record',
      returnedDate: '2025-01-16',
    },
  },
  {
    id: 'return-fine-required',
    title: '存在逾期费用',
    description: '返回 ReturnProcessResponse 失败结果，提示该记录需先处理逾期费用，暂不完成归还。',
    badge: 'warning',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.FineRequired,
      message: '该借阅记录存在逾期费用，请先完成费用处理。',
      recordId: 'active-return-record',
    },
  },
  {
    id: 'return-error',
    title: '办理失败',
    description: '返回 ReturnProcessResponse 失败结果，归还处理未成功并保留原有借阅状态。',
    badge: 'error',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.RetryRequired,
      message: '还书办理失败，请稍后重新提交。',
      recordId: 'active-return-record',
    },
  },
]

export default function ReturnManage() {
  const [searchParams] = useSearchParams()
  const { openMockDialog, showNotice } = useMockSystem()

  const [keyword, setKeyword] = useState('')
  const [records, setRecords] = useState<ReturnRecordEntity[]>(initialRecords)
  const [activeRecordId, setActiveRecordId] = useState<string | null>(null)
  const [isSearching, setIsSearching] = useState(false)
  const pendingBookName = searchParams.get('bookName')
  const pendingBookId = searchParams.get('bookId')

  const filteredRecords = useMemo(() => {
    const trimmedKeyword = keyword.trim().toLowerCase()
    if (!trimmedKeyword) {
      return records
    }

    return records.filter((record) =>
      [record.bookTitle, record.readerName, record.borrowCode, record.id]
        .join(' ')
        .toLowerCase()
        .includes(trimmedKeyword),
    )
  }, [keyword, records])

  const handleSearchResult = (option: MockResultOption) => {
    setIsSearching(false)

    if (option.id === 'search-success') {
      return
    }

    if (option.id === 'search-empty') {
      setRecords([])
      return
    }

    showNotice('检索失败，请稍后重试。', 'error')
  }

  const handleSearch = () => {
    setIsSearching(true)
    openMockDialog({
      pageName,
      route,
      componentName: '检索按钮',
      interactionName: '检索待还图书',
      title: '选择检索结果',
      description: '模拟管理员按关键词查询借阅记录后的不同返回结果。',
      prioritySuggestion: '优先确认有结果、无结果和查询失败三种状态下的页面反馈。',
      options: searchOptions,
      onSelect: handleSearchResult,
    })
  }

  const handleReturnResult = (option: ReturnManageOption) => {
    if (!activeRecordId) {
      return
    }

    const request: ReturnProcessRequest = { recordId: activeRecordId }
    void request

    const response = option.payload
    if (response?.ok) {
      setRecords((current) =>
        current.map((record) =>
          record.id === activeRecordId
            ? new ReturnRecordEntity(
                record.id,
                record.bookTitle,
                record.readerName,
                record.borrowCode,
                record.borrowDate,
                record.dueDate,
                ReturnRecordStatuses.Returned,
                response.returnedDate,
              )
            : record,
        ),
      )
      setActiveRecordId(null)
      return
    }

    if (response?.reviewStatus === CirculationReviewStatuses.FineRequired) {
      showNotice(response.message, 'info')
      setActiveRecordId(null)
      return
    }

    showNotice(response?.message ?? '还书办理失败，请稍后重新提交。', 'error')
    setActiveRecordId(null)
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'scan-borrow-code',
      label: '扫描借阅码',
      description: '模拟管理员使用扫码枪录入借阅码并自动检索记录。',
      dialog: {
        pageName,
        route,
        eventName: '扫描借阅码',
        interactionName: '扫码检索借阅记录',
        title: '选择扫码检索结果',
        description: '模拟页面接收到借阅码后自动完成检索的处理结果。',
        options: searchOptions,
        onSelect: (option) => {
          setKeyword(option.id === 'search-success' ? 'BR-50177' : keyword)
          handleSearchResult(option)
        },
      },
    },
    {
      id: 'refresh-overdue-records',
      label: '刷新逾期记录',
      description: '模拟页面自动刷新后更新逾期借阅状态。',
      dialog: {
        pageName,
        route,
        eventName: '刷新逾期记录',
        interactionName: '页面级借阅状态刷新',
        title: '选择刷新后的页面状态',
        description: '模拟管理员进入页面后系统刷新待还记录的结果。',
        options: [
          {
            id: 'refresh-success',
            title: '刷新成功',
            description: '更新列表并提示逾期记录状态已同步。',
            badge: 'success',
            noticeMessage: '借阅状态已同步到最新数据。',
          },
          {
            id: 'refresh-no-change',
            title: '无状态变化',
            description: '页面保持当前列表，不显示额外异常。',
            badge: 'info',
          },
          {
            id: 'refresh-error',
            title: '刷新失败',
            description: '保留当前页面数据，并提示同步失败。',
            badge: 'error',
          },
        ],
        onSelect: (option) => {
          if (option.id === 'refresh-error') {
            showNotice('状态刷新失败，请稍后重试。', 'error')
          }
        },
      },
    },
  ]

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(197,225,255,0.32),transparent_34%),linear-gradient(180deg,#f4f8fb_0%,#edf3f7_48%,#e7eef4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.55),transparent_42%,rgba(15,23,42,0.05)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.1),transparent_70%)] opacity-80" />

      <FloatingPageTools
        events={pageEvents}
        onEventSelect={(event) => openMockDialog(event.dialog)}
      />

      <section className="relative mx-auto flex min-h-screen w-full max-w-7xl px-6 py-12 sm:px-8 lg:px-12">
        <div className="w-full space-y-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <Undo2 className="size-4 text-slate-700" />
                还书业务办理
              </div>
              <div className="space-y-3">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">还书管理</h1>
                <p className="max-w-2xl text-base leading-7 text-slate-600">
                  查询借阅记录并办理图书归还。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-5 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div className="space-y-2">
                  <CardTitle className="text-2xl text-slate-900">借阅记录检索</CardTitle>
                  <CardDescription className="text-sm text-slate-500">
                    支持按图书名称、读者姓名、借阅码查询待还记录。
                  </CardDescription>
                </div>
                <div className="flex w-full flex-col gap-3 sm:flex-row lg:max-w-xl">
                  <div className="relative flex-1">
                    <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      value={keyword}
                      placeholder="请输入图书名称、读者姓名或借阅码"
                      className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                      onChange={(event) => setKeyword(event.target.value)}
                    />
                  </div>
                  <Button
                    type="button"
                    className="h-11 rounded-xl bg-slate-900 px-6 text-white hover:bg-slate-800"
                    onClick={handleSearch}
                    disabled={isSearching}
                  >
                    查询记录
                  </Button>
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-5 px-7 py-7 sm:px-8">
              {pendingBookName ? (
                <Alert className="rounded-2xl border-sky-200 bg-sky-50/90">
                  <AlertDescription className="text-sm text-sky-700">
                    已从上一页带入《{pendingBookName}》{pendingBookId ? `（${pendingBookId}）` : ''}，可直接查询并办理归还。
                  </AlertDescription>
                </Alert>
              ) : null}

              <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white/80">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
                      <TableHead className="h-12 pl-5 text-slate-500">图书信息</TableHead>
                      <TableHead className="text-slate-500">读者</TableHead>
                      <TableHead className="text-slate-500">借阅码</TableHead>
                      <TableHead className="text-slate-500">借阅日期</TableHead>
                      <TableHead className="text-slate-500">应还日期</TableHead>
                      <TableHead className="text-slate-500">状态</TableHead>
                      <TableHead className="pr-5 text-right text-slate-500">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredRecords.length ? (
                      filteredRecords.map((record) => (
                        <TableRow key={record.id} className="hover:bg-slate-50/70">
                          <TableCell className="pl-5">
                            <div className="space-y-1">
                              <div className="font-medium text-slate-900">{record.bookTitle}</div>
                              <div className="text-xs text-slate-500">{record.id}</div>
                            </div>
                          </TableCell>
                          <TableCell>
                            <div className="inline-flex items-center gap-2 text-slate-700">
                              <UserRoundCheck className="size-4 text-slate-400" />
                              {record.readerName}
                            </div>
                          </TableCell>
                          <TableCell className="text-slate-600">{record.borrowCode}</TableCell>
                          <TableCell className="text-slate-600">{record.borrowDate}</TableCell>
                          <TableCell>
                            <div className="inline-flex items-center gap-2 text-slate-600">
                              <CalendarClock className="size-4 text-slate-400" />
                              {record.dueDate}
                            </div>
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className={resolveStatusClassName(record.status)}>
                              {record.status}
                            </Badge>
                          </TableCell>
                          <TableCell className="pr-5 text-right">
                            <Button
                              type="button"
                              size="sm"
                              variant={record.status === ReturnRecordStatuses.Returned ? 'secondary' : 'outline'}
                              className="rounded-xl"
                              disabled={record.status === ReturnRecordStatuses.Returned}
                              onClick={() => {
                                setActiveRecordId(record.id)
                                openMockDialog({
                                  pageName,
                                  route,
                                  componentName: '办理还书按钮',
                                  interactionName: '提交还书办理',
                                  title: `选择“${record.bookTitle}”的还书结果`,
                                  description: '模拟管理员提交还书办理后的不同处理结果。',
                                  prioritySuggestion: '优先确认正常归还、逾期未处理和系统失败三种业务分支。',
                                  options: returnOptions,
                                  onSelect: handleReturnResult,
                                })
                              }}
                            >
                              {record.status === ReturnRecordStatuses.Returned ? '已办理' : '确认还书'}
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={7} className="h-56">
                          <div className="flex flex-col items-center justify-center gap-3 text-center">
                            <div className="flex size-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-500">
                              <BookCopy className="size-6" />
                            </div>
                            <div className="space-y-1">
                              <p className="text-sm font-medium text-slate-700">暂无匹配的借阅记录</p>
                              <p className="text-sm text-slate-500">请调整检索条件后重新查询。</p>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}

function resolveStatusClassName(status: (typeof ReturnRecordStatuses)[keyof typeof ReturnRecordStatuses]) {
  switch (status) {
    case ReturnRecordStatuses.Pending:
      return 'border-sky-200 bg-sky-50 text-sky-700'
    case ReturnRecordStatuses.Overdue:
      return 'border-amber-200 bg-amber-50 text-amber-700'
    case ReturnRecordStatuses.Returned:
      return 'border-emerald-200 bg-emerald-50 text-emerald-700'
    default:
      return ''
  }
}
