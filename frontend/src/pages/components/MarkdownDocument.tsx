import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'

/**
 * Markdown 文档渲染属性，输入为可信业务 Markdown 文本和可选容器类名。
 */
type MarkdownDocumentProps = {
  content: string
  className?: string
}

/**
 * Markdown 展示组件，禁用 HTML 并启用 GFM、数学公式和自定义链接/图片/代码样式。
 */
export function MarkdownDocument({ content, className = '' }: MarkdownDocumentProps) {
  return (
    <div className={`markdown-document ${className}`.trim()}>
      <ReactMarkdown
        skipHtml
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{
          a: ({ ...props }) => (
            <a
              {...props}
              className="font-medium text-emerald-700 underline underline-offset-4 hover:text-emerald-800"
              rel="noreferrer"
              target="_blank"
            />
          ),
          img: ({ alt = '', ...props }) => (
            <img
              {...props}
              alt={alt}
              className="max-h-96 w-full rounded-2xl border border-slate-200 bg-white object-contain"
              loading="lazy"
            />
          ),
          table: ({ ...props }) => (
            <div className="overflow-x-auto">
              <table {...props} className="min-w-full border-collapse text-sm" />
            </div>
          ),
          code: ({ className: codeClassName, children, ...props }) => {
            const isBlock = Boolean(codeClassName)

            if (!isBlock) {
              return (
                <code
                  {...props}
                  className="rounded-md bg-slate-100 px-1.5 py-0.5 font-mono text-[0.95em] text-slate-800"
                >
                  {children}
                </code>
              )
            }

            return (
              <code {...props} className={`${codeClassName ?? ''} font-mono text-sm text-slate-800`}>
                {children}
              </code>
            )
          },
          pre: ({ ...props }) => (
            <pre
              {...props}
              className="overflow-x-auto rounded-xl border border-slate-200 bg-slate-100 px-5 py-4 text-sm leading-7"
            />
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  )
}
