<script setup lang="ts">
import { Layout } from 'ant-design-vue'

const ALayoutFooter = Layout.Footer
</script>

<template>
  <ALayoutFooter class="global-footer">
    <span>CODE原创项目 by </span>
    <a href="https://www.baidu.cn" target="_blank" rel="noopener noreferrer">ZhenQ</a>
  </ALayoutFooter>
</template>

<style scoped>
.global-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9;
  height: 56px;
  padding: 16px 24px;
  color: rgba(0, 0, 0, 0.65);
  text-align: center;
  background: #fff;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
}
</style>
