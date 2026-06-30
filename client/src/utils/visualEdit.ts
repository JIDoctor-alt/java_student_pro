/** 可视化编辑：选中元素上下文 */
export interface VisualEditContext {
  elementId?: string
  tagName: string
  cssSelector?: string
  xpath?: string
  siblingIndex?: number
  textPreview?: string
  outerHtmlSnippet?: string
  sourceFile?: string
  sourceLine?: number
  pageUrl?: string
  editMode?: 'manual' | 'chat'
  modificationDescription?: string
}

export type VisualEditParentMessage =
  | { type: 'VISUAL_EDIT_SELECT'; payload: VisualEditContext }
  | { type: 'VISUAL_EDIT_CLEAR' }
  | { type: 'VISUAL_EDIT_READY' }

export type VisualEditChildMessage =
  | { type: 'VISUAL_EDIT_MODE'; enabled: boolean }
  | { type: 'VISUAL_EDIT_HIGHLIGHT'; elementId: string }

const INSPECTOR_SCRIPT_PATH = '/preview-inspector.js'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

/** 获取 inspector 脚本的绝对 URL（供 srcdoc 注入） */
export function getPreviewInspectorScriptUrl(): string {
  const base = API_BASE.replace(/\/$/, '')
  return `${base}${INSPECTOR_SCRIPT_PATH}`
}

/** 向 HTML 注入 preview-inspector.js */
export function injectPreviewInspector(html: string): string {
  if (!html.trim() || html.includes('preview-inspector.js')) {
    return html
  }
  const scriptTag = `<script src="${getPreviewInspectorScriptUrl()}" defer></script>`
  const lower = html.toLowerCase()
  const bodyClose = lower.lastIndexOf('</body>')
  if (bodyClose >= 0) {
    return html.slice(0, bodyClose) + scriptTag + html.slice(bodyClose)
  }
  return html + scriptTag
}

/** 选中元素摘要（输入框上方 chip） */
export function formatVisualSelectionLabel(ctx: VisualEditContext | null): string {
  if (!ctx?.tagName) return ''
  let label = ctx.tagName
  if (ctx.elementId) label += ` #${ctx.elementId}`
  if (ctx.sourceFile) {
    label += ` @ ${ctx.sourceFile}`
    if (ctx.sourceLine != null) label += `:${ctx.sourceLine}`
  } else if (ctx.textPreview) {
    label += ` · ${ctx.textPreview.slice(0, 24)}`
  }
  return label
}

/** 校验 postMessage 是否来自预览 iframe */
export function isVisualEditMessage(data: unknown): data is VisualEditParentMessage {
  if (!data || typeof data !== 'object') return false
  const t = (data as { type?: string }).type
  return t === 'VISUAL_EDIT_SELECT' || t === 'VISUAL_EDIT_CLEAR' || t === 'VISUAL_EDIT_READY'
}
