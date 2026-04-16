// 小程序端避免依赖 npm 解析器，使用轻量文本转 HTML，保证组件可稳定加载。

const applyPrettyStyle = (html = '') =>
  html
    .replace(/<h1>/g, '<h1 style="font-size:22px;line-height:1.35;margin:14px 0 10px;font-weight:700;color:#0f172a;">')
    .replace(/<h2>/g, '<h2 style="font-size:20px;line-height:1.35;margin:12px 0 8px;font-weight:700;color:#0f172a;">')
    .replace(/<h3>/g, '<h3 style="font-size:18px;line-height:1.35;margin:10px 0 8px;font-weight:700;color:#1e293b;">')
    .replace(/<p>/g, '<p style="line-height:1.75;margin:8px 0;color:#1f2937;">')
    .replace(/<ul>/g, '<ul style="margin:8px 0 8px 18px;padding:0;color:#1f2937;">')
    .replace(/<ol>/g, '<ol style="margin:8px 0 8px 18px;padding:0;color:#1f2937;">')
    .replace(/<li>/g, '<li style="margin:6px 0;line-height:1.65;">')
    .replace(/<blockquote>/g, '<blockquote style="margin:12px 0;padding:10px 12px;border-left:4px solid #93c5fd;background:#eff6ff;border-radius:8px;color:#1e40af;">')
    .replace(/<pre>/g, '<pre style="margin:12px 0;padding:12px;border-radius:10px;background:#0f172a;color:#e2e8f0;overflow:auto;">')
    .replace(/<code>/g, '<code style="font-family:monospace;">')
    .replace(/<table>/g, '<table style="width:100%;border-collapse:collapse;margin:12px 0;font-size:13px;">')
    .replace(/<th>/g, '<th style="border:1px solid #dbeafe;padding:8px;background:#eff6ff;text-align:left;">')
    .replace(/<td>/g, '<td style="border:1px solid #e5e7eb;padding:8px;">')
    .replace(/<a /g, '<a style="color:#2563eb;text-decoration:none;" ')

export const parseMarkdownToHtml = (markdown = '') => {
  const text = String(markdown || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  const withCodeBlock = text.replace(/```([\s\S]*?)```/g, (_m, code) => {
    return `<pre><code>${code.trim()}</code></pre>`
  })
  const withHeading = withCodeBlock
    .replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
  const withInline = withHeading
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+?)`/g, '<code>$1</code>')
  const withList = withInline
    .replace(/(?:^|\n)-\s+(.+)(?=\n|$)/g, '<li>$1</li>')
    .replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
  const rawHtml = withList
    .split('\n')
    .map((line) => {
      const t = line.trim()
      if (!t) {
        return '<br/>'
      }
      if (t.startsWith('<h1>') || t.startsWith('<h2>') || t.startsWith('<h3>')
        || t.startsWith('<ul>') || t.startsWith('<li>') || t.startsWith('<pre>')) {
        return t
      }
      return `<p>${t}</p>`
    })
    .join('')
  const prettyHtml = applyPrettyStyle(rawHtml)
  return prettyHtml || '<div></div>'
}
