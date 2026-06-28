/**
 * 使用 fetch 消费 SSE 流（比 EventSource 更可靠：可处理 401 JSON 错误、无自动重连）
 */
export interface SseStreamOptions {
  url: string
  signal?: AbortSignal
  onChunk: (chunk: string) => void
  onDone: () => void
  onError: (message: string) => void
  /** 命名 SSE 事件（tool-start / build-log 等） */
  onEvent?: (eventName: string, data: string) => void
}

export async function consumeSseStream(options: SseStreamOptions): Promise<void> {
  const { url, signal, onChunk, onDone, onError, onEvent } = options

  let response: Response
  try {
    response = await fetch(url, { credentials: 'include', signal })
  } catch (e) {
    onError(e instanceof Error ? e.message : '网络请求失败')
    return
  }

  const contentType = response.headers.get('content-type') ?? ''

  // 鉴权失败等场景：后端返回 JSON 而非 SSE
  if (!contentType.includes('text/event-stream')) {
    const text = await response.text()
    try {
      const json = JSON.parse(text) as { code?: number; message?: string }
      if (json.code === 40100) {
        onError('请先登录后再生成')
        return
      }
      if (json.code === 40101) {
        onError('无权限操作此应用')
        return
      }
      onError(json.message || `请求失败 (${response.status})`)
    } catch {
      onError(`请求失败 (${response.status})`)
    }
    return
  }

  const reader = response.body?.getReader()
  if (!reader) {
    onError('无法读取响应流')
    return
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let receivedData = false
  let hasError = false

  const processBlock = (block: string) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines: string[] = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^\s/, ''))
      }
    }
    const data = dataLines.join('\n')
    if (eventName === 'done') {
      if (!hasError) {
        onDone()
      }
      return
    }
    if (eventName === 'gen-error') {
      hasError = true
      onError(data || '生成失败')
      return
    }
    if (eventName === 'message' || eventName === '') {
      if (data && data !== '[DONE]') {
        receivedData = true
        onChunk(data)
      }
      return
    }
    if (onEvent) {
      onEvent(eventName, data)
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() ?? ''
      for (const block of blocks) {
        processBlock(block)
      }
    }
    if (buffer.trim()) {
      processBlock(buffer)
    }
    // 流正常结束但未收到 done 事件
    if (receivedData && !hasError) {
      onDone()
    } else if (!receivedData && !hasError) {
      onError('未收到生成内容，请稍后重试')
    }
  } catch (e) {
    if (signal?.aborted) return
    onError(e instanceof Error ? e.message : '流式读取失败')
  }
}
