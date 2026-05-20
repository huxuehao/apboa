import { message, Modal } from 'ant-design-vue'
import type { SkillImportResult } from '@/types'

/**
 * 根据导入结果展示提示。
 * @returns 是否有新技能成功导入（此时才应刷新列表并切换分类）
 */
export function showSkillImportMessage(result: SkillImportResult | undefined | null): boolean {
  if (!result || result.totalCount === 0) {
    const detail = result?.hintMessage?.trim()
    if (detail) {
      Modal.warning({
        title: '未识别到有效技能包',
        content: detail,
        width: 560
      })
    } else {
      message.warning('未识别到有效技能包，请检查 skills/ 目录结构及 SKILL.md 文件')
    }
    return false
  }
  if (result.importedCount === 0) {
    message.warning(
      `识别到 ${result.totalCount} 个技能，均已存在且未开启覆盖，列表不会有变化。如需更新请开启「覆盖已有同名技能」。`
    )
    return false
  }
  if (result.skippedCount > 0) {
    message.success(`成功导入 ${result.importedCount} 个技能包，跳过 ${result.skippedCount} 个同名技能`)
  } else {
    message.success(`成功导入 ${result.importedCount} 个技能包`)
  }
  return true
}

type SkillImportEmit = {
  (e: 'update:visible', value: boolean): void
  (e: 'success', category?: string): void
}

/**
 * 展示导入结果并关闭弹窗；仅在有新技能导入成功时触发 success。
 */
export function finishSkillImport(
  result: SkillImportResult | undefined | null,
  emit: SkillImportEmit,
  category?: string
): boolean {
  const imported = showSkillImportMessage(result)
  emit('update:visible', false)
  if (imported) {
    emit('success', category)
  }
  return imported
}
