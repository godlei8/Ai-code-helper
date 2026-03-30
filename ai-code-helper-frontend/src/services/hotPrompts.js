import { http } from './http'

const HOT_PROMPTS_CACHE_KEY = 'ai-code-helper:hot-prompts:v2'

const DEFAULT_PROMPT_ITEMS = [
  {
    id: 'default-java-roadmap',
    title: '零基础转 Java 后端，3 个月应该怎么规划学习路线？',
    tagLabel: '学习路线'
  },
  {
    id: 'default-interview-java',
    title: 'Java 后端面试中，JVM、并发、Spring、MySQL 应该怎么系统准备？',
    tagLabel: '面试准备'
  },
  {
    id: 'default-project-resume',
    title: '后端求职项目经历怎么包装，才能让简历更有竞争力？',
    tagLabel: '项目简历'
  },
  {
    id: 'default-offer-plan',
    title: '如果目标是拿到后端 offer，刷题、八股、项目和投递应该怎么分配时间？',
    tagLabel: '求职规划'
  },
  {
    id: 'default-springboot',
    title: '用通俗的话解释 Spring Boot 自动装配，并说清常见面试追问。',
    tagLabel: '框架原理'
  },
  {
    id: 'default-rag-ai',
    title: '做一个 AI 编程助手项目，RAG、SSE 流式输出和 MCP 工具调用分别适合解决什么问题？',
    tagLabel: '项目实战'
  }
]

function todayKey() {
  return new Date().toISOString().slice(0, 10)
}

function readCache() {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    const raw = window.localStorage.getItem(HOT_PROMPTS_CACHE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeCache(items) {
  if (typeof window === 'undefined') {
    return
  }

  try {
    window.localStorage.setItem(
      HOT_PROMPTS_CACHE_KEY,
      JSON.stringify({
        date: todayKey(),
        items
      })
    )
  } catch {
    // Ignore cache write failures.
  }
}

function buildFallbackPrompts() {
  return DEFAULT_PROMPT_ITEMS.map((item) => ({
    id: item.id,
    title: item.title,
    prompt: `请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：${item.title}`,
    tagLabel: item.tagLabel,
    link: '',
    sourceLabel: '热榜兜底'
  }))
}

function normalizeItems(items) {
  if (!Array.isArray(items)) {
    return []
  }

  return items
    .filter((item) => item && item.id && item.title && item.prompt)
    .map((item) => ({
      id: String(item.id),
      title: String(item.title),
      prompt: String(item.prompt),
      tagLabel: String(item.tagLabel || '技术热榜'),
      link: String(item.link || ''),
      sourceLabel: String(item.sourceLabel || '热榜来源')
    }))
}

export async function getDailyHotPrompts(options = {}) {
  const { forceRefresh = false } = options
  const cache = readCache()

  if (!forceRefresh && cache?.date === todayKey() && Array.isArray(cache.items) && cache.items.length) {
    return cache.items
  }

  try {
    const response = await http.get('/hot/prompts', {
      timeout: 15000,
      params: {
        refresh: forceRefresh
      }
    })
    const items = normalizeItems(response.data)

    if (items.length) {
      writeCache(items)
      return items
    }
  } catch {
    if (!forceRefresh && cache?.items?.length) {
      return cache.items
    }
  }

  return buildFallbackPrompts()
}
