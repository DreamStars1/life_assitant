// commentImageGallery.selfcheck.ts
import assert from 'node:assert/strict'
import { buildCommentImageGallery } from './commentImageGallery'

const comments = [
  { id: 'a', imageUrls: ['u1', ''] },
  { id: 'b', imageUrls: null },
  { id: 'c', imageUrls: ['u2', 'u3'] },
  { id: 'd' },
]

let g = buildCommentImageGallery(comments, 'c', 1)
assert.deepEqual(g.images, ['u1', 'u2', 'u3'])
assert.equal(g.startPosition, 2)

g = buildCommentImageGallery(comments, 'a', 0)
assert.equal(g.startPosition, 0)

g = buildCommentImageGallery(comments, 'missing', 0)
assert.deepEqual(g.images, ['u1', 'u2', 'u3'])
assert.equal(g.startPosition, 0)

g = buildCommentImageGallery([], 'a', 0)
assert.deepEqual(g.images, [])
assert.equal(g.startPosition, 0)

console.log('commentImageGallery.selfcheck: ok')
