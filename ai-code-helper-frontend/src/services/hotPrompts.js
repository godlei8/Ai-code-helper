import axios from 'axios'

const HOT_PROMPTS_CACHE_KEY = 'ai-code-helper:hot-prompts'
const HOT_PROXY_BASE = import.meta.env.VITE_HOT_PROXY_BASE || ''
const JUEJIN_FEED_URL = `${HOT_PROXY_BASE}/juejin/recommend_api/v1/article/recommend_all_feed`
const V2EX_HOT_URL = `${HOT_PROXY_BASE}/v2ex/api/topics/hot.json`
const CNODE_HOT_URL = 'https://cnodejs.org/api/v1/topics'

const POSITIVE_KEYWORDS = [
  ['面试', 120],
  ['求职', 110],
  ['简历', 95],
  ['offer', 95],
  ['校招', 90],
  ['社招', 90],
  ['实习', 85],
  ['八股', 85],
  ['后端', 78],
  ['前端', 74],
  ['全栈', 72],
  ['java', 82],
  ['spring', 72],
  ['mysql', 65],
  ['redis', 62],
  ['jvm', 60],
  ['算法', 62],
  ['python', 58],
  ['golang', 58],
  ['go', 44],
  ['docker', 52],
  ['kubernetes', 50],
  ['微服务', 58],
  ['程序员', 40],
  ['职场', 70],
  ['ai编程', 46],
  ['开发', 38],
  ['编程', 38]
]

const NEGATIVE_KEYWORDS = ['获奖', '抽奖', '推广', '广告', '活动', '吐槽', '闲聊', '相亲', '新茶', '摩托车']
const V2EX_ALLOWED_NODES = new Set(['programmer', 'career', 'java', 'python', 'go', 'cloud', 'devops', 'mysql', 'server'])

function todayKey() {
  return new Date().toISOString().slice(0, 10)
}

function decodeHtml(value) {
  if (typeof window === 'undefined') {
    return value
  }

  const textarea = document.createElement('textarea')
  textarea.innerHTML = value
  return textarea.value
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

function buildPrompt(item, tagLabel) {
  return `请结合这条中文技术社区热榜内容，用中文帮我讲清楚背景、核心问题、解决思路、适用场景，并补充面试中可能怎么问：${item.title}\n来源：${item.sourceLabel}\n标签：${tagLabel}\n原始链接：${item.link}`
}

function normalizeText(value) {
  return String(value || '').toLowerCase()
}

function keywordScore(text, tags = [], node = '') {
  const merged = `${normalizeText(text)} ${normalizeText(tags.join(' '))} ${normalizeText(node)}`
  let score = 0

  for (const [keyword, weight] of POSITIVE_KEYWORDS) {
    if (merged.includes(keyword)) {
      score += weight
    }
  }

  for (const keyword of NEGATIVE_KEYWORDS) {
    if (merged.includes(normalizeText(keyword))) {
      score -= 140
    }
  }

  return score
}

function isRelevant(item) {
  return item.relevanceScore >= 45
}

async function fetchJuejinHot() {
  if (!HOT_PROXY_BASE) {
    return []
  }

  const response = await axios.post(
    JUEJIN_FEED_URL,
    {
      id_type: 2,
      sort_type: 200,
      cursor: '0',
      limit: 24
    },
    {
      timeout: 12000
    }
  )

  return (response.data?.data ?? [])
    .map((entry) => {
      const article = entry.item_info?.article_info
      const tags = (entry.item_info?.tags ?? []).map((tag) => tag.tag_name)
      const title = decodeHtml(article?.title ?? '')
      const relevanceScore = keywordScore(title, tags)

      return {
        id: `juejin-${article?.article_id}`,
        title,
        link: `https://juejin.cn/post/${article?.article_id}`,
        tagLabel: tags[0] || '掘金热榜',
        sourceLabel: '掘金',
        relevanceScore,
        hotnessScore:
          (article?.hot_index ?? 0) +
          (article?.digg_count ?? 0) * 4 +
          (article?.comment_count ?? 0) * 6 +
          Math.log10((article?.view_count ?? 0) + 1) * 55 +
          relevanceScore
      }
    })
    .filter((item) => item.title && isRelevant(item))
}

async function fetchV2exHot() {
  if (!HOT_PROXY_BASE) {
    return []
  }

  const response = await axios.get(V2EX_HOT_URL, {
    timeout: 12000
  })

  return (response.data ?? [])
    .map((item) => {
      const title = decodeHtml(item.title ?? '')
      const nodeName = item.node?.name ?? ''
      const nodeTitle = item.node?.title ?? ''
      const relevanceScore = keywordScore(title, [nodeTitle], nodeName)
      const nodeBoost = V2EX_ALLOWED_NODES.has(nodeName) ? 90 : 0

      return {
        id: `v2ex-${item.id}`,
        title,
        link: item.url || `https://www.v2ex.com/t/${item.id}`,
        tagLabel: nodeTitle || 'V2EX 热门',
        sourceLabel: 'V2EX',
        relevanceScore: relevanceScore + nodeBoost,
        hotnessScore:
          (item.replies ?? 0) * 18 +
          (item.member?.id ?? 0) / 1000 +
          relevanceScore +
          nodeBoost
      }
    })
    .filter((item) => item.title && isRelevant(item))
}

async function fetchCNodeHot() {
  const response = await axios.get(CNODE_HOT_URL, {
    params: {
      tab: 'all',
      page: 1,
      limit: 20
    },
    timeout: 12000
  })

  return (response.data?.data ?? [])
    .map((item) => {
      const title = decodeHtml(item.title ?? '')
      const relevanceScore = keywordScore(title, ['Node', '前端', '后端'])

      return {
        id: `cnode-${item.id}`,
        title,
        link: `https://cnodejs.org/topic/${item.id}`,
        tagLabel: item.top ? 'CNode 置顶' : item.good ? 'CNode 精华' : 'CNode 热议',
        sourceLabel: 'CNode',
        relevanceScore,
        hotnessScore:
          (item.visit_count ?? 0) / 120 +
          (item.reply_count ?? 0) * 8 +
          relevanceScore
      }
    })
    .filter((item) => item.title && isRelevant(item))
}

export async function getDailyHotPrompts() {
  const cache = readCache()
  if (cache?.date === todayKey() && Array.isArray(cache.items) && cache.items.length) {
    return cache.items
  }

  try {
    const result = await Promise.all([fetchJuejinHot(), fetchV2exHot(), fetchCNodeHot()])
    const merged = result
      .flat()
      .filter((item, index, array) => array.findIndex((candidate) => candidate.title === item.title) === index)
      .sort((left, right) => right.hotnessScore - left.hotnessScore)
      .slice(0, 6)
      .map((item) => {
        return {
          id: item.id,
          title: item.title,
          prompt: buildPrompt(item, item.tagLabel),
          tagLabel: item.tagLabel,
          link: item.link,
          sourceLabel: item.sourceLabel
        }
      })

    writeCache(merged)
    return merged
  } catch (error) {
    if (cache?.items?.length) {
      return cache.items
    }

    throw error
  }
}
