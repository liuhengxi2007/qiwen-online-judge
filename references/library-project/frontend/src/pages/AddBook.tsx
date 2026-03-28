import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BookPlus, BookText, FileText, Hash, LibraryBig, ScanSearch } from 'lucide-react'

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

const pageName = '添加图书页'
const route = '/library/admin/book/add'
const createdBookId = 'BK-2001'

type AddBookResultId = 'add-book-success' | 'duplicate-isbn' | 'inventory-error'
type AddBookMockOption = MockResultOption<AddBookResultId, SaveBookResponse>

const submitOptions: AddBookMockOption[] = [
  {
    id: 'add-book-success',
    title: '添加成功',
    description: '图书信息保存成功，返回 SaveBookResponse 成功结果并继续进入新图书详情页确认录入结果。',
    badge: 'success',
    noticeMessage: '图书已添加成功，正在进入详情页。',
    payload: {
      ok: true,
      bookId: createdBookId,
      redirectTo: `/library/book/detail/${createdBookId}?source=add&highlight=${createdBookId}`,
    },
  },
  {
    id: 'duplicate-isbn',
    title: 'ISBN 已存在',
    description: '系统检测到重复编号，返回 SaveBookResponse 失败结果并在表单下方提示冲突信息。',
    badge: 'error',
    payload: {
      ok: false,
      reason: SaveBookFailureReasons.DuplicateIsbn,
      message: 'ISBN 已存在，请检查后重新输入。',
    },
  },
  {
    id: 'inventory-error',
    title: '库存数量异常',
    description: '库存数据不符合要求，返回 SaveBookResponse 失败结果并提示管理员修正后重新提交。',
    badge: 'warning',
    payload: {
      ok: false,
      reason: SaveBookFailureReasons.InvalidStock,
      message: '库存数量需为大于 0 的整数。',
    },
  },
]

export default function AddBook() {
  const navigate = useNavigate()
  const { openMockDialog } = useMockSystem()

  const [title, setTitle] = useState('')
  const [author, setAuthor] = useState('')
  const [isbn, setIsbn] = useState('')
  const [category, setCategory] = useState<BookCategoryCode | ''>('')
  const [stock, setStock] = useState('')
  const [summary, setSummary] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleMockResult = (option: AddBookMockOption) => {
    setIsSubmitting(false)

    const response = option.payload
    if (!response) {
      return
    }

    if (response.ok) {
      setErrorMessage('')
      navigate(response.redirectTo)
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
      componentName: '添加图书按钮',
      interactionName: '提交新增图书',
      title: '选择添加图书结果',
      description: '模拟管理员提交新图书信息后的不同处理结果。',
      prioritySuggestion: '优先确认添加成功后进入详情页继续核对，以及重复 ISBN 和库存异常时的提示。',
      options: submitOptions,
      onSelect: handleMockResult,
    })
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'scan-isbn',
      label: '扫码录入 ISBN',
      description: '模拟管理员通过扫码枪快速填写 ISBN。',
      dialog: {
        pageName,
        route,
        eventName: '扫码录入 ISBN',
        interactionName: '设备录入 ISBN',
        title: '选择扫码录入结果',
        description: '模拟扫码设备录入 ISBN 后，表单的填充与异常反馈。',
        options: [
          {
            id: 'scan-success',
            title: '识别成功',
            description: '自动填入一个有效的 ISBN 编号。',
            badge: 'success',
            noticeMessage: '已识别 ISBN 编号。',
          },
          {
            id: 'scan-failed',
            title: '识别失败',
            description: '未能识别条码，提示管理员手动输入。',
            badge: 'warning',
          },
        ],
        onSelect: (option: MockResultOption) => {
          setIsSubmitting(false)
          if (option.id === 'scan-success') {
            setIsbn('9787302511853')
            setErrorMessage('')
            return
          }

          setErrorMessage('未识别到有效条码，请手动输入 ISBN。')
        },
      },
    },
    {
      id: 'restore-draft',
      label: '恢复草稿内容',
      description: '模拟页面重新打开后恢复上次未提交的录入内容。',
      dialog: {
        pageName,
        route,
        eventName: '恢复草稿内容',
        interactionName: '页面级草稿恢复',
        title: '选择草稿恢复结果',
        description: '模拟管理员返回页面时，系统是否恢复未提交的图书表单内容。',
        options: [
          {
            id: 'restore-success',
            title: '恢复最近草稿',
            description: '填回一组上次未提交的图书信息。',
            badge: 'info',
          },
          {
            id: 'restore-empty',
            title: '无可恢复内容',
            description: '保持当前空白表单，不做额外提示。',
            badge: 'warning',
          },
        ],
        onSelect: (option: MockResultOption) => {
          setIsSubmitting(false)
          if (option.id === 'restore-success') {
            setTitle('深入理解计算机系统')
            setAuthor('Randal E. Bryant')
            setIsbn('9787111544937')
            setCategory(BookCategoryCodes.Computer)
            setStock('6')
            setSummary('从程序运行视角理解计算机系统结构与实现。')
            setErrorMessage('')
            return
          }

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
                <BookPlus className="size-4 text-slate-700" />
                图书信息录入
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 添加图书
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  录入图书基础信息，提交后进入详情页继续核对，并可返回列表确认新记录。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-3 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-center gap-3 lg:hidden">
                <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
                  <BookPlus className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-900">图书馆管理系统 - 添加图书</CardTitle>
                </div>
              </div>
              <div className="hidden lg:block">
                <CardTitle className="text-2xl text-slate-900">添加图书</CardTitle>
              </div>
              <CardDescription className="text-sm text-slate-500">
                请填写图书信息
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
                        placeholder="请输入图书名称"
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
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
                        placeholder="请输入作者姓名"
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
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
                        placeholder="请输入 ISBN 编号"
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
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
                        placeholder="请输入库存数量"
                        className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
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
                        placeholder="请输入图书简介"
                        className="min-h-32 rounded-2xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
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
                  {isSubmitting ? '提交中...' : '添加图书'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
