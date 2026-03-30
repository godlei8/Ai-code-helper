import hljs from 'highlight.js'
import MarkdownIt from 'markdown-it'
import markdownItMultimdTable from 'markdown-it-multimd-table'
import markdownItTaskLists from 'markdown-it-task-lists'

const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  xhtmlOut: false,
  linkify: true,
  typographer: true,
  highlight(code, language) {
    const normalizedLanguage = language?.trim()

    if (normalizedLanguage && hljs.getLanguage(normalizedLanguage)) {
      return `<pre class="code-block"><code class="hljs language-${normalizedLanguage}">${hljs.highlight(code, {
        language: normalizedLanguage,
        ignoreIllegals: true
      }).value}</code></pre>`
    }

    return `<pre class="code-block"><code class="hljs">${hljs.highlightAuto(code).value}</code></pre>`
  }
})
  .use(markdownItMultimdTable, {
    multiline: true,
    rowspan: true,
    headerless: true
  })
  .use(markdownItTaskLists, {
    enabled: true,
    label: true,
    labelAfter: true
  })

function normalizeMarkdownSyntax(source) {
  let inFence = false

  return source
    .split(/\r?\n/)
    .map((line) => {
      const trimmed = line.trimStart()

      if (/^```/.test(trimmed) || /^~~~/.test(trimmed)) {
        inFence = !inFence
        return line
      }

      if (inFence) {
        return line
      }

      return line.replace(/^(\s{0,3})(#{1,6})([^\s#])/, '$1$2 $3')
    })
    .join('\n')
}

export function renderMarkdown(content) {
  const source = content?.trim() ? normalizeMarkdownSyntax(content) : ''
  const html = markdown.render(source)

  return html.replace(/<table>/g, '<div class="table-wrap"><table>').replace(/<\/table>/g, '</table></div>')
}
