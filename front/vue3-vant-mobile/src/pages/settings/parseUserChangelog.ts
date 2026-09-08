export interface ChangelogSection {
  version: string
  date: string
  items: string[]
}

/** 解析 docs/USER_CHANGELOG.md 用户向正文；忽略前言。 */
export function parseUserChangelog(raw: string): ChangelogSection[] {
  const sections: ChangelogSection[] = []
  let current: ChangelogSection | null = null

  for (const line of raw.split(/\r?\n/)) {
    const heading = line.match(/^##\s+(v[\d.]+)\s*[（(]([^）)]+)[）)]\s*$/)
    if (heading) {
      current = { version: heading[1]!, date: heading[2]!.trim(), items: [] }
      sections.push(current)
      continue
    }
    if (!current)
      continue
    // ponytail: avoid /^-\s+(.+)$/ — eslint flags \s+/ .+ backtracking
    if (!line.startsWith('-'))
      continue
    const text = line.slice(1).trim()
    if (text)
      current.items.push(text)
  }

  return sections
}
