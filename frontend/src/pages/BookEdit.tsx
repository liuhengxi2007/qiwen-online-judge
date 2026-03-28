import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { BookText, FileText, Hash, LibraryBig, PencilLine, ScanSearch } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  BookCategoryCodes,
  type BookCategoryCode,
  BookFormDraft,
  SaveBookFailureReasons,
  type SaveBookRequest,
  type SaveBookResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '图书编辑页'
const route = '/library/admin/book/edit/:id'

type EditBookResultId = 'edit-success' | 'edit-conflict'
type EditBookMockOption = MockResultOption<EditBookResultId, SaveBookResponse>

const submitOptions: EditBookMockOption[] = [
  {
    id: 'edit-success',
    title: '保存成功',
    description: '图书信息更新成功，返回 SaveBookResponse 成功结果并回到图书详情页继续核对。',
    badge: 'success',
    noticeMessage: '图书信息已更新。',
    payload: {
      ok: true,
      bookId: 'current-edit-book',
      redirectTo: '/library/book/detail/__ID__?updated=1',
    },
  },
  {
    id: 'edit-conflict',
    title: '编号冲突',
    description: '系统检测到 ISBN 与其他记录冲突，返回 SaveBookResponse 失败结果并保留当前表单。',
    badge: 'error',
    payload: {
      ok: false,
      reason: SaveBookFailureReasons.DuplicateIsbn,
      message: 'ISBN 与现有图书冲突，请调整后重新保存。',
    },
  },
]

export default function BookEdit() {
  const navigate = useNavigate()
  const { id } = useParams()
  const { openMockDialog } = useMockSystem()

  const [title, setTitle] = useState(id === 'BK-1002' ? '三体' : '百年孤独')
  const [author, setAuthor] = useState(id === 'BK-1002' ? '刘慈欣' : '加西亚·马尔克斯')
  const [isbn, setIsbn] = useState(id === 'BK-1002' ? '9787536692930' : '9787544291170')
  const [category, setCategory] = useState<BookCategoryCode>(
    id === 'BK-1002' ? BookCategoryCodes.Computer : BookCategoryCodes.Literature,
  )
  const [stock, setStock] = useState(id === 'BK-1002' ? '2' : '4')
  const [summary, setSummary] = useState(
    id === 'BK-1002'
      ? '文明冲突与宇宙社会学交织展开的科幻长篇。'
      : '布恩迪亚家族七代人的命运在马孔多小镇交织展开。',
  )
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmitResult = (option: EditBookMockOption) => {
    setIsSubmitting(false)

    const response = option.payload
    if (!response) {
      return
    }

    if (response.ok) {
      setErrorMessage('')
      navigate(`/library/book/detail/${id}?updated=1`)
      return
    }

    setErrorMessage(response.message)
  }

  const handleSubmit = () => {
    const request: SaveBookRequest = {
      draft: BookFormDraft.fromForm(title, author, isbn, category, stock, summary),
    }

    if (!request.draft.isComplete) {
      setErrorMessage('请完整填写图书信息。')
      return
    }

    if (!request.draft.hasValidStock) {
      setErrorMessage('库存数量需为大于 0 的整数。')
      return
    }

    setErrorMessage('')
    setIsSubmitting(true)

    openMockDialog({
      pageName,
      route,
      componentName: '保存修改按钮',
      interactionName: '提交图书修改',
      title: '选择保存结果',
      description: '模拟管理员提交图书编辑信息后的处理结果。',
      prioritySuggestion: '优先确认保存成功后返回详情页继续核对，以及编号冲突时的提示。',
      options: submitOptions,
      onSelect: handleSubmitResult,
    })
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'restore-original',
      label: '恢复原始信息',
      description: '模拟管理员恢复当前图书的原始馆藏信息。',
      dialog: {
        pageName,
        route,
        eventName: '恢复原始信息',
        interactionName: '页面级恢复初始值',
        title: '选择恢复结果',
        description: '模拟管理员撤回当前编辑内容后恢复初始信息。',
        options: [
          {
            id: 'restore-success',
            title: '恢复成功',
            description: '表单回到原始记录内容。',
            badge: 'success',
          },
        ],
        onSelect: () => {
          setTitle(id === 'BK-1002' ? '三体' : '百年孤独')
          setAuthor(id === 'BK-1002' ? '刘慈欣' : '加西亚·马尔克斯')
          setIsbn(id === 'BK-1002' ? '9787536692930' : '9787544291170')
          setCategory(id === 'BK-1002' ? BookCategoryCodes.Computer : BookCategoryCodes.Literature)
          setStock(id === 'BK-1002' ? '2' : '4')
          setSummary(
            id === 'BK-1002'
              ? '文明冲突与宇宙社会学交织展开的科幻长篇。'
              : '布恩迪亚家族七代人的命运在马孔多小镇交织展开。',
          )
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
        <div className="grid w-full max-w-5xl items-center gap-8 lg:grid-cols-[1fr_1.02fr]">
          <div className="hidden lg:block">
            <div className="max-w-md space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <PencilLine className="size-4 text-slate-700" />
                图书信息编辑
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 编辑图书
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  修改现有图书资料，保存后返回详情页继续核对最新内容。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <CardTitle className="text-2xl text-slate-900">编辑图书</CardTitle>
                  <CardDescription className="text-sm text-slate-500">
                    当前编辑记录：{id}
                  </CardDescription>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="rounded-xl"
                  onClick={() => navigate(`/library/book/detail/${id}`)}
                >
                  返回详情
                </Button>
              </div>
            </CardHeader>

            <CardContent className="px-7 py-7 sm:px-8">
              <form
                className="space-y-5"
                onSubmit={(event) => {
                  event.preventDefault()
                  handleSubmit()
                }}
              >
                <div className="grid gap-5 sm:grid-cols-2">
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="book-title" className="text-slate-700">
                      图书名称
                    </Label>
                    <div className="relative">
                      <BookText className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="book-title"
                        value={title}
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 focus-visible:ring-slate-400"
                        onChange={(event) => setTitle(event.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="book-author" className="text-slate-700">
                      作者
                    </Label>
                    <div className="relative">
                      <FileText className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="book-author"
                        value={author}
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 focus-visible:ring-slate-400"
                        onChange={(event) => setAuthor(event.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="book-isbn" className="text-slate-700">
                      ISBN
                    </Label>
                    <div className="relative">
                      <ScanSearch className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="book-isbn"
                        value={isbn}
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 focus-visible:ring-slate-400"
                        onChange={(event) => setIsbn(event.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="book-category" className="text-slate-700">
                      分类
                    </Label>
                    <Select value={category} onValueChange={(value) => setCategory(value as BookCategoryCode)}>
                      <SelectTrigger
                        id="book-category"
                        className="h-11 rounded-xl border-slate-200 bg-white text-slate-900 focus:ring-slate-400"
                      >
                        <SelectValue placeholder="请选择图书分类" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="computer">计算机</SelectItem>
                        <SelectItem value="literature">文学</SelectItem>
                        <SelectItem value="history">历史</SelectItem>
                        <SelectItem value="management">管理</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="book-stock" className="text-slate-700">
                      库存数量
                    </Label>
                    <div className="relative">
                      <Hash className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                      <Input
                        id="book-stock"
                        value={stock}
                        inputMode="numeric"
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 focus-visible:ring-slate-400"
                        onChange={(event) => setStock(event.target.value)}
                      />
                    </div>
                  </div>

                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="book-summary" className="text-slate-700">
                      图书简介
                    </Label>
                    <div className="relative">
                      <LibraryBig className="pointer-events-none absolute top-4 left-3 size-4 text-slate-400" />
                      <Textarea
                        id="book-summary"
                        value={summary}
                        className="min-h-32 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900 focus-visible:ring-slate-400"
                        onChange={(event) => setSummary(event.target.value)}
                      />
                    </div>
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
                  className="h-11 w-full rounded-xl bg-slate-900 text-sm font-medium text-white shadow-[0_16px_36px_rgba(15,23,42,0.18)] transition hover:bg-slate-800"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? '保存中...' : '保存修改'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
