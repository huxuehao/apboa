/**
 * 敏感词输入组件
 * 支持输入框输入、标签展示、删除操作
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const inputValue = ref<string>('')
const words = ref<string[]>([...props.modelValue])

watch(
  () => props.modelValue,
  (newVal) => {
    words.value = [...newVal]
  }
)

/**
 * 处理输入框回车事件
 */
function handlePressEnter() {
  if (!inputValue.value.trim()) return

  const newWords = inputValue.value
    .split(/[\s,、]+/)
    .map((w) => w.trim())
    .filter((w) => w && !words.value.includes(w))

  if (newWords.length > 0) {
    words.value.push(...newWords)
    emit('update:modelValue', words.value)
  }

  inputValue.value = ''
}

/**
 * 删除单个敏感词
 *
 * @param index 索引
 */
function removeWord(index: number) {
  words.value.splice(index, 1)
  emit('update:modelValue', words.value)
}

/**
 * 清空所有敏感词
 */
function clearAll() {
  words.value = []
  emit('update:modelValue', words.value)
}
</script>

<template>
  <div class="words-input-container">
    <AInput
      v-model:value="inputValue"
      placeholder="输入敏感词,支持空格或逗号分隔,回车添加"
      @pressEnter="handlePressEnter"
    />
    <div v-if="words.length > 0" class="words-display mt-sm">
      <div class="flex items-center justify-between mb-sm">
        <span class="text-secondary text-sm">已添加 {{ words.length }} 个敏感词</span>
        <AButton type="text" size="small" @click="clearAll">清空全部</AButton>
      </div>
      <div class="words-tags flex flex-wrap gap-sm">
        <ATag
          v-for="(word, index) in words"
          :key="index"
          closable
          @close="removeWord(index)"
        >
          {{ word }}
        </ATag>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.words-input-container {
  .words-display {
    padding: var(--spacing-sm);
    background-color: var(--color-bg-light);
    border-radius: var(--border-radius-base);
  }

  //.words-tags {
  //  max-height: 150px;
  //  overflow-y: auto;
  //}
}
</style>
