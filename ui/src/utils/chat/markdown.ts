import { marked } from 'marked'

export function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text, { gfm: true, breaks: true }) as string
  } catch {
    return text
  }
}
