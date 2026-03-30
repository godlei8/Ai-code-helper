<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CloseBold, Delete, Position, RefreshRight } from '@element-plus/icons-vue'
import { buildSseUrl, apiBaseUrl } from './services/http'
import { getDailyHotPrompts } from './services/hotPrompts'
import { renderMarkdown } from './utils/markdown'

const messages = ref([])
const prompt = ref('')
const memoryId = ref(generateMemoryId())
const isStreaming = ref(false)
const chatBodyRef = ref(null)
const inputRef = ref(null)
const currentStream = ref(null)
const currentAssistantMessageId = ref(null)
const hotPrompts = ref([])
const isLoadingHotPrompts = ref(false)
const hotPromptsError = ref('')
const totalMessages = computed(() => Math.max(messages.value.length - 1, 0))

const connectionLabel = computed(() =>
  isStreaming.value ? 'AI 正在实时回复' : `SSE 接口：${apiBaseUrl}/ai/chat`
)

const canSend = computed(() => prompt.value.trim() && !isStreaming.value)

seedWelcomeMessage()

onMounted(() => {
  loadHotPrompts()
  focusInput()
  scrollToBottom()
})

onBeforeUnmount(() => {
  closeStream()
})

function seedWelcomeMessage() {
  messages.value = [
    {
      id: createMessageId(),
      role: 'assistant',
      content: `你好，我是 AI 编程小助手。当前会话 ID 为 ${memoryId.value}。\n你可以问我编程学习路线、项目实践建议、求职准备和面试题。`,
      status: 'done',
      createdAt: Date.now()
    }
  ]
}

function generateMemoryId() {
  if (window.crypto?.getRandomValues) {
    const buffer = new Uint32Array(1)
    window.crypto.getRandomValues(buffer)
    return 100000000 + (buffer[0] % 900000000)
  }

  return Math.floor(100000000 + Math.random() * 900000000)
}

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function formatTime(timestamp) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(timestamp)
}

function focusInput() {
  nextTick(() => {
    inputRef.value?.focus?.()
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (!chatBodyRef.value) {
      return
    }

    chatBodyRef.value.scrollTo({
      top: chatBodyRef.value.scrollHeight,
      behavior: 'smooth'
    })
  })
}

function appendMessage(message) {
  messages.value.push(message)
  scrollToBottom()
}

function selectHotPrompt(item) {
  if (isStreaming.value) {
    return
  }

  prompt.value = item.title
  focusInput()
}

function startNewSession() {
  closeStream()
  memoryId.value = generateMemoryId()
  prompt.value = ''
  currentAssistantMessageId.value = null
  seedWelcomeMessage()
  focusInput()
  scrollToBottom()
  ElMessage.success(`已创建新会话：${memoryId.value}`)
}

function clearMessages() {
  closeStream()
  prompt.value = ''
  currentAssistantMessageId.value = null
  seedWelcomeMessage()
  focusInput()
  scrollToBottom()
  ElMessage.success('聊天记录已清空')
}

async function loadHotPrompts(forceRefresh = false) {
  isLoadingHotPrompts.value = true
  hotPromptsError.value = ''

  try {
    hotPrompts.value = await getDailyHotPrompts({ forceRefresh })
  } catch {
    hotPrompts.value = []
    hotPromptsError.value = '今日热门问题加载失败，你也可以直接输入自己的问题。'
  } finally {
    isLoadingHotPrompts.value = false
  }
}

function closeStream() {
  if (currentStream.value) {
    currentStream.value.close()
    currentStream.value = null
  }
  isStreaming.value = false
}

function stopStreaming() {
  const message = messages.value.find((item) => item.id === currentAssistantMessageId.value)

  closeStream()

  if (message) {
    if (message.content.trim()) {
      message.status = 'done'
    } else {
      message.status = 'stopped'
      message.content = '本次回答已手动停止。'
    }
  }

  currentAssistantMessageId.value = null
}

function sendMessage() {
  const userText = prompt.value.trim()

  if (!userText || isStreaming.value) {
    return
  }

  appendMessage({
    id: createMessageId(),
    role: 'user',
    content: userText,
    status: 'done',
    createdAt: Date.now()
  })

  const assistantMessage = reactive({
    id: createMessageId(),
    role: 'assistant',
    content: '',
    status: 'streaming',
    createdAt: Date.now()
  })

  appendMessage(assistantMessage)

  currentAssistantMessageId.value = assistantMessage.id
  prompt.value = ''
  isStreaming.value = true

  const sseUrl = buildSseUrl('/ai/chat', {
    memoryId: memoryId.value,
    message: userText
  })

  const eventSource = new EventSource(sseUrl)
  let hasReceivedChunk = false

  currentStream.value = eventSource

  eventSource.onmessage = (event) => {
    hasReceivedChunk = true
    assistantMessage.content += event.data
    assistantMessage.status = 'streaming'
    scrollToBottom()
  }

  eventSource.onerror = () => {
    closeStream()

    if (hasReceivedChunk) {
      assistantMessage.status = 'done'
      currentAssistantMessageId.value = null
      return
    }

    assistantMessage.status = 'error'
    assistantMessage.content =
      '对话连接失败，请检查后端服务、SSE 接口和跨域配置是否正常。'
    currentAssistantMessageId.value = null
    ElMessage.error('SSE 连接失败，请确认后端接口可用。')
  }
}
</script>

<template>
  <div class="page-shell">
    <div class="backdrop glow-one"></div>
    <div class="backdrop glow-two"></div>
    <div class="backdrop grid-mask"></div>

    <main class="chat-layout">
      <section class="hero-panel">
        <div class="hero-copy">
          <div class="brand-chip">
            <img src="/favicon.svg" alt="AI Code Helper" class="brand-chip__logo" />
            <span>AI Programming Co-pilot</span>
          </div>
          <h1>AI 编程小助手</h1>
          <p>
            面向编程学习、项目实践与求职面试的实时对话助手。
            通过 SSE 流式输出建议，保持一问一答的聊天节奏。
          </p>
          <div class="hero-badges">
            <span>实时 SSE 回复</span>
            <span>Markdown 富文本</span>
            <span>求职与学习双场景</span>
          </div>
        </div>

        <div class="session-panel">
          <div class="session-grid">
            <div class="session-card">
              <span>聊天室 ID</span>
              <strong>{{ memoryId }}</strong>
            </div>
            <div class="session-card">
              <span>当前消息数</span>
              <strong>{{ totalMessages }}</strong>
            </div>
          </div>
          <div class="session-actions">
            <el-button plain round :icon="Delete" @click="clearMessages">
              清空记录
            </el-button>
            <el-button plain round :icon="RefreshRight" @click="startNewSession">
              新建会话
            </el-button>
          </div>
        </div>
      </section>

      <section class="chat-panel">
        <header class="chat-panel__header">
          <div>
            <h2>对话记录</h2>
            <p>用户消息在右侧，AI 回复在左侧，内容会实时滚动更新。</p>
          </div>
          <div class="header-actions">
            <div class="status-pill" :class="{ active: isStreaming }">
              <span class="status-pill__dot"></span>
              {{ connectionLabel }}
            </div>
          </div>
        </header>

        <div ref="chatBodyRef" class="chat-thread">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="avatar">
              {{ message.role === 'assistant' ? 'AI' : '我' }}
            </div>

            <div class="bubble" :class="message.status">
              <div class="bubble__meta">
                <span>{{ message.role === 'assistant' ? 'AI 编程小助手' : '你' }}</span>
                <time>{{ formatTime(message.createdAt) }}</time>
              </div>

              <div
                v-if="message.role === 'assistant'"
                class="bubble__content bubble__content--markdown"
                v-html="renderMarkdown(message.content)"
              ></div>

              <div v-else class="bubble__content">
                {{ message.content }}
              </div>

              <div
                v-if="message.role === 'assistant' && message.status === 'streaming'"
                class="typing-indicator"
              >
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <section class="quick-prompts">
          <div class="quick-prompts__header">
            <div>
              <h3>今日技术热榜</h3>
              <p>基于掘金、V2EX、CNode 中文技术社区热榜，每天自动刷新。</p>
            </div>
            <el-button
              plain
              round
              :icon="RefreshRight"
              :loading="isLoadingHotPrompts"
              @click="loadHotPrompts(true)"
            >
              刷新热榜
            </el-button>
          </div>

          <div v-if="isLoadingHotPrompts" class="prompt-grid prompt-grid--loading">
            <div v-for="index in 4" :key="index" class="prompt-card prompt-card--placeholder">
              <span class="prompt-card__tag">加载中</span>
              <strong></strong>
              <p></p>
            </div>
          </div>

          <div v-else-if="hotPrompts.length" class="prompt-grid">
            <button
              v-for="item in hotPrompts"
              :key="item.id"
              type="button"
              class="prompt-card"
              :title="item.title"
              @click="selectHotPrompt(item)"
            >
              <div class="prompt-card__meta">
                <span class="prompt-card__tag">{{ item.sourceLabel }}</span>
                <span class="prompt-card__tag prompt-card__tag--soft">{{ item.tagLabel }}</span>
              </div>
              <strong>{{ item.title }}</strong>
              <p>点击带入这个中文热榜话题，做编程讲解、求职分析和面试延伸。</p>
            </button>
          </div>

          <div v-else class="prompt-empty">
            {{ hotPromptsError || '暂时没有可展示的热门问题。' }}
          </div>
        </section>

        <footer class="composer">
          <el-input
            ref="inputRef"
            v-model="prompt"
            class="composer__input"
            type="textarea"
            resize="none"
            :autosize="{ minRows: 2, maxRows: 5 }"
            placeholder="输入你的问题，例如：怎么学习 Java、如何准备后端面试？"
            @keydown.enter.exact.prevent="sendMessage"
          />

          <div class="composer__toolbar">
            <span>Enter 发送，Shift + Enter 换行</span>

            <div class="composer__actions">
              <el-button
                v-if="isStreaming"
                round
                :icon="CloseBold"
                @click="stopStreaming"
              >
                停止生成
              </el-button>
              <el-button
                type="primary"
                round
                :icon="Position"
                :disabled="!canSend"
                @click="sendMessage"
              >
                发送消息
              </el-button>
            </div>
          </div>
        </footer>
      </section>
    </main>
  </div>
</template>
