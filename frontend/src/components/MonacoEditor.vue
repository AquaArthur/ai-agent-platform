<template>
  <div ref="editorContainer" class="monaco-editor-container" :style="{ height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as monaco from 'monaco-editor'

interface Props {
  modelValue: string
  language?: string
  height?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  language: 'markdown',
  height: '300px'
})

const emit = defineEmits<Emits>()

const editorContainer = ref<HTMLDivElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const editorOptions: monaco.editor.IStandaloneEditorConstructionOptions = {
  theme: 'vs',
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  fontSize: 14,
  lineNumbers: 'on',
  roundedSelection: false,
  cursorStyle: 'line',
  automaticLayout: true,
  wordWrap: 'on',
  tabSize: 2,
  insertSpaces: true,
  formatOnPaste: true,
  formatOnType: true,
  folding: true,
  lineDecorationsWidth: 10,
  lineNumbersMinChars: 3,
  renderLineHighlight: 'all',
  selectOnLineNumbers: true,
  matchBrackets: 'always',
  renderWhitespace: 'selection',
  quickSuggestions: true,
  suggestOnTriggerCharacters: true,
  acceptSuggestionOnEnter: 'on',
  tabCompletion: 'on'
}

onMounted(async () => {
  await nextTick()
  if (!editorContainer.value) return

  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue || '',
    language: props.language,
    ...editorOptions
  })

  editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() || ''
    emit('update:modelValue', value)
  })
})

watch(
  () => props.modelValue,
  (newValue) => {
    if (editor && editor.getValue() !== newValue) {
      editor.setValue(newValue || '')
    }
  }
)

watch(
  () => props.language,
  (newLanguage) => {
    const model = editor?.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, newLanguage)
    }
  }
)

watch(
  () => props.height,
  () => {
    editor?.layout()
  }
)

onUnmounted(() => {
  editor?.dispose()
})
</script>

<style scoped>
.monaco-editor-container {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.2s;
}

.monaco-editor-container:hover {
  border-color: var(--el-border-color-hover);
}

.monaco-editor-container:focus-within {
  border-color: var(--el-color-primary);
}
</style>

