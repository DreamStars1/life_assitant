import assert from 'node:assert/strict'
import {
  afterHitFlash,
  applyClick,
  clearBruise,
  clampPos,
  createShibaPetLogicState,
  HITS_TO_BRUISE,
  isShibaPetAllowed,
} from './shibaPetLogic.ts'

assert.equal(isShibaPetAllowed('cc'), true)
assert.equal(isShibaPetAllowed('小星露'), true)
assert.equal(isShibaPetAllowed('other'), false)
assert.equal(isShibaPetAllowed(undefined), false)
assert.equal(isShibaPetAllowed(''), false)

let s = createShibaPetLogicState()
for (let i = 0; i < HITS_TO_BRUISE - 1; i++) {
  const r = applyClick(s)
  s = afterHitFlash(r.state)
  assert.equal(s.bruised, false)
  assert.equal(s.displayMood, 'normal')
}
{
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, true)
  assert.equal(r.state.bruised, true)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruised')
}
{
  const r = applyClick(s)
  assert.equal(r.startBruiseTimer, false)
  assert.equal(r.state.bruised, true)
  s = afterHitFlash(r.state)
  assert.equal(s.displayMood, 'bruised')
}
s = clearBruise(s)
assert.equal(s.displayMood, 'normal')
assert.equal(s.bruised, false)
assert.equal(s.hitCount, 0)

const c = clampPos(-100, 9999, 400, 800, 88, 8)
assert.equal(c.x, 8)
assert.equal(c.y, 800 - 88 - 8)

console.log('shibaPetLogic.selfcheck: ok')
