<script setup lang="ts">
import bruise1Url from '@/assets/shiba-pet/dog_bruise_1.png'
import bruise2Url from '@/assets/shiba-pet/dog_bruise_2.png'
import bruise3Url from '@/assets/shiba-pet/dog_bruise_3.png'
import hit1Url from '@/assets/shiba-pet/dog_hit_1.png'
import hit2Url from '@/assets/shiba-pet/dog_hit_2.png'
import idleUrl from '@/assets/shiba-pet/dog_idle_happy.png'
import greetGif from '@/assets/shiba-pet/anim/dog_greet_bow.webp'
import crawlGif from '@/assets/shiba-pet/anim/dog_react_crawl.webp'
import bowGif from '@/assets/shiba-pet/anim/dog_react_bow.webp'
import wagGif from '@/assets/shiba-pet/anim/dog_idle_wag.webp'
import type { ShibaAnim, ShibaMood } from './shibaPetLogic'

const props = withDefaults(defineProps<{
  mood?: ShibaMood
  anim?: ShibaAnim | null
  /** 变更以强制重播动画 */
  animKey?: number
}>(), {
  mood: 'idle',
  anim: null,
  animKey: 0,
})

const moodSrc: Record<ShibaMood, string> = {
  idle: idleUrl,
  hit_1: hit1Url,
  hit_2: hit2Url,
  bruise_1: bruise1Url,
  bruise_2: bruise2Url,
  bruise_3: bruise3Url,
}

const animSrc: Record<ShibaAnim, string> = {
  wag: wagGif,
  crawl: crawlGif,
  bow: bowGif,
  greet: greetGif,
}

const src = computed(() => {
  if (props.anim)
    return `${animSrc[props.anim]}?k=${props.animKey}`
  return moodSrc[props.mood]
})
</script>

<template>
  <div class="shiba-face" aria-hidden="true">
    <img
      class="shiba-face__img"
      :src="src"
      alt=""
      draggable="false"
    >
  </div>
</template>

<style scoped>
.shiba-face {
  width: 100%;
  height: 100%;
  user-select: none;
  pointer-events: none;
  background: transparent;
}

.shiba-face__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
  background: transparent;
}
</style>
