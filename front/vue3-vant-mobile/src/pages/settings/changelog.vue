<script setup lang="ts">
import userChangelogRaw from '~docs/USER_CHANGELOG.md?raw'
import { parseUserChangelog } from './parseUserChangelog'

defineOptions({ name: 'SettingsChangelog' })

const sections = parseUserChangelog(userChangelogRaw)
</script>

<template>
  <div class="changelog-page">
    <div v-if="sections.length === 0" class="changelog-empty">
      暂无更新说明
    </div>
    <section v-for="sec in sections" :key="sec.version" class="changelog-section">
      <header class="changelog-head">
        <span class="changelog-ver">{{ sec.version }}</span>
        <span class="changelog-date">{{ sec.date }}</span>
      </header>
      <ul class="changelog-list">
        <li v-for="(item, i) in sec.items" :key="i">
          {{ item }}
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.changelog-page {
  padding: 12px 16px 24px;
}

.changelog-empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--van-text-color-3);
  font-size: 14px;
}

.changelog-section {
  margin-bottom: 20px;
}

.changelog-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}

.changelog-ver {
  font-size: 16px;
  font-weight: 600;
  color: var(--van-text-color);
}

.changelog-date {
  font-size: 12px;
  color: var(--van-text-color-3);
}

.changelog-list {
  margin: 0;
  padding-left: 1.2em;
  color: var(--van-text-color-2);
  font-size: 14px;
  line-height: 1.6;
}

.changelog-list li + li {
  margin-top: 4px;
}
</style>

<route lang="json5">
{
  name: 'SettingsChangelog'
}
</route>
