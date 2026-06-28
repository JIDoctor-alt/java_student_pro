<script setup lang="ts">
import { Layout } from 'ant-design-vue'
import { RouterView } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'

const ALayout = Layout
const ALayoutContent = Layout.Content
</script>

<template>
  <ALayout class="basic-layout">
    <GlobalHeader />

    <ALayoutContent class="basic-layout__content">
      <div class="basic-layout__content-inner">
        <RouterView />
      </div>
    </ALayoutContent>

    <GlobalFooter />
  </ALayout>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  width: 100%;
  min-width: 320px;
  min-height: 100%;
  margin: 0;
}

:global(body) {
  background: #f5f7fb;
}

.basic-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.basic-layout__content {
  min-height: calc(100vh - 64px - 56px);
  padding: 24px;
  padding-bottom: 80px;
}

.basic-layout__content-inner {
  width: 100%;
  max-width: 1200px;
  min-height: 100%;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .basic-layout__content {
    padding: 16px;
    padding-bottom: 80px;
  }
}
</style>
