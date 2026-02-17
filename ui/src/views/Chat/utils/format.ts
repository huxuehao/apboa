/**
 * 尝试将字符串格式化为 JSON（美化），失败则返回原文
 */
export function formatToolDisplay(text: string): string {
  if (!text) return text
  const trimmed = text.trim()
  if (!trimmed) return text
  if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
    try {
      const parsed = JSON.parse(trimmed)
      return JSON.stringify(parsed, null, 2)
    } catch {
      return text
    }
  }
  return text
}

/**
 * 构建工具调用的 Markdown 内容（用于保存到消息中）
 */
export function buildToolCallsContent(
  toolCalls: Array<{ id: string; name: string; args: string; result?: string; elapsed?: number }>
): string {
  if (toolCalls.length === 0) return ''

  const blocks: string[] = []
  for (const t of toolCalls) {
    let block = ''
    if (t.args) block += '\n\n参数：\n````json\n' + formatToolDisplay(t.args) + '\n````\n'
    if (t.result != null) block += '\n\n结果：\n````\n' + formatToolDisplay(t.result) + '\n````\n'
    blocks.push(
      `<details> <summary> <span style="color: #1890FF; font-weight: bold; cursor: pointer; "> ${t.name}（耗时：${t.elapsed || 0}ms） </span> </summary>\n\n ${block} </details>`
    )
  }

  return '**工具调用记录**\n\n' + blocks.join('\n\n---\n\n')
}

/**
 * 根据用户输入生成会话标题（截取前50字符）
 */
export function formatSessionTitle(input: string): string {
  const t = (input || '').trim()
  if (!t) return '新对话'
  return t.length > 50 ? t.slice(0, 50) + '...' : t
}
