import assert from 'node:assert/strict'
import {
  afterHitFlash,
  applyClick,
  clearBruise,
  clampPos,
  createShibaPetLogicState,
  HITS_TO_BRUISE,
  isShibaPetAllowed,
  PET_HEIGHT,
  PET_WIDTH,
} from './shibaPetLogic.ts'

assert.equal(isShibaPetAllowed('cc'), true)
assert.equal(isShibaPetAllowed('小星露'), true)
assert.equal(isShibaPetAllowed('other'), false)
assert.equal(isShibaPetAllowed(undefined), false)

let s = createShibaPetLogicState()
assert.equal(s.displayMood, 'idle')

{
  const r = applyClick(s)
  assert.equal(r.state.displayMood, 'hit_1')
  assert.equal(r.startBruiseTimer, false)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'idle')
  assert.equal(s.hitCount, 1)
}
{
  const r = applyClick(s)
  assert.equal(r.state.displayMood, 'hit_2')
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'idle')
  assert.equal(s.hitCount, 2)
}
{
  assert.equal(s.hitCount, HITS_TO_BRUISE - 1)
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, true)
  assert.equal(r.state.bruiseLevel, 1)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruise_1')
}
{
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, true)
  assert.equal(r.state.bruiseLevel, 2)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruise_2')
}
{
  const r = applyClick(s)
  assert.equal(r.state.bruiseLevel, 3)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruise_3')
}
{
  const r = applyClick(s)
  assert.equal(r.state.bruiseLevel, 3)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruise_3')
}

s = clearBruise(s)
assert.equal(s.displayMood, 'idle')
assert.equal(s.bruiseLevel, 0)
assert.equal(s.hitCount, 0)

const c = clampPos(-100, 9999, 400, 800)
assert.equal(c.x, 8)
assert.equal(c.y, 800 - PET_HEIGHT - 8)
assert.ok(PET_WIDTH > 0)

console.log('shibaPetLogic.selfcheck: ok')
