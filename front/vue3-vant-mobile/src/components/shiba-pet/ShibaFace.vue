<script setup lang="ts">
import type { ShibaMood } from './shibaPetLogic'

withDefaults(defineProps<{ mood?: ShibaMood }>(), { mood: 'normal' })
</script>

<template>
  <svg
    class="shiba-face"
    viewBox="0 0 88 88"
    width="88"
    height="88"
    role="img"
    aria-hidden="true"
  >
    <!-- 耳 -->
    <ellipse cx="22" cy="28" rx="12" ry="16" fill="#E8A86B" transform="rotate(-20 22 28)" />
    <ellipse cx="66" cy="28" rx="12" ry="16" fill="#E8A86B" transform="rotate(20 66 28)" />
    <ellipse cx="22" cy="30" rx="6" ry="9" fill="#F5C9A0" transform="rotate(-20 22 30)" />
    <ellipse cx="66" cy="30" rx="6" ry="9" fill="#F5C9A0" transform="rotate(20 66 30)" />
    <!-- 头 -->
    <circle cx="44" cy="48" r="30" fill="#F0B27A" />
    <ellipse cx="44" cy="58" rx="16" ry="12" fill="#FFE6C8" />
    <!-- 眼：normal / bruised 睁眼；hit 眯成线 -->
    <g v-if="mood !== 'hit'">
      <ellipse cx="32" cy="46" rx="4" ry="5" fill="#3a2a20" />
      <ellipse cx="56" cy="46" rx="4" ry="5" fill="#3a2a20" />
    </g>
    <g v-else>
      <path d="M26 46 Q32 42 38 46" stroke="#3a2a20" stroke-width="2" fill="none" />
      <path d="M50 46 Q56 42 62 46" stroke="#3a2a20" stroke-width="2" fill="none" />
    </g>
    <!-- 鼻口 -->
    <ellipse cx="44" cy="56" rx="4" ry="3" fill="#3a2a20" />
    <path d="M44 59 Q40 64 36 62" stroke="#3a2a20" stroke-width="1.5" fill="none" />
    <path d="M44 59 Q48 64 52 62" stroke="#3a2a20" stroke-width="1.5" fill="none" />
    <!-- hit 标识 -->
    <g v-if="mood === 'hit'">
      <text x="68" y="22" font-size="12" fill="#e74c3c" font-family="sans-serif">疼</text>
      <circle cx="58" cy="40" r="4" fill="#ff8a80" opacity="0.85" />
    </g>
    <!-- bruised -->
    <g v-if="mood === 'bruised'">
      <ellipse cx="32" cy="46" rx="8" ry="6" fill="#7b68a6" opacity="0.45" />
      <rect x="50" y="38" width="14" height="8" rx="2" fill="#fff8e7" stroke="#e8b05e" stroke-width="1" />
      <line x1="57" y1="38" x2="57" y2="46" stroke="#e8b05e" stroke-width="1" />
    </g>
  </svg>
</template>

<style scoped>
.shiba-face {
  display: block;
  user-select: none;
  pointer-events: none;
}
</style>
