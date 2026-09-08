import { parseUserChangelog } from './parseUserChangelog'

const sample = `# 前言

发版说明见别处。

---

## v1.9.3（2026-08-12）

- 第一项
- 第二项

## v1.8.0 (2026-08-08)

- ascii 括号
`

const sections = parseUserChangelog(sample)
if (sections.length !== 2)
  throw new Error(`expected 2 sections, got ${sections.length}`)
if (sections[0]!.version !== 'v1.9.3' || sections[0]!.date !== '2026-08-12')
  throw new Error('first section meta wrong')
if (sections[0]!.items.join('|') !== '第一项|第二项')
  throw new Error('first items wrong')
if (sections[1]!.version !== 'v1.8.0' || sections[1]!.items[0] !== 'ascii 括号')
  throw new Error('second section wrong')
if (parseUserChangelog('').length !== 0)
  throw new Error('empty should be []')

console.warn('parseUserChangelog.selfcheck: ok')
