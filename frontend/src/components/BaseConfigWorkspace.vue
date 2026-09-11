<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import CategoryWorkspace from './CategoryWorkspace.vue'
import TemplateWorkspace from './TemplateWorkspace.vue'
import ConnectionWorkspace from './ConnectionWorkspace.vue'
import PermissionAuditWorkspace from './PermissionAuditWorkspace.vue'
import PartyConfigWorkspace from './PartyConfigWorkspace.vue'
import AiConfigWorkspace from './AiConfigWorkspace.vue'

type Type = { id: number; name: string; subtype: string; category: string; enabled: boolean }
defineProps<{ api: string; role: string; types: Type[] }>()

const sections = [
  { key: 'ai', title: 'AI 能力配置', desc: '配置 AI 服务、工作流和合同页面的调用能力' },
  { key: 'parties', title: '对方主体', desc: '维护客户、供应商、员工及其他合同签约主体' },
  { key: 'categories', title: '合同分类', desc: '维护合同大类、类型和子类型' },
  { key: 'templates', title: '合同模板', desc: '维护模板正文、版本和适用类型' },
  { key: 'connections', title: '外部连接', desc: '维护外部系统适配器与连接测试' },
  { key: 'permissions', title: '权限与审计', desc: '角色权限、数据范围与全局审计' },
] as const
const active = ref<string | null>('categories')

function toggle(key: string) {
  active.value = active.value === key ? null : key
}
</script>

<template>
  <section class="base-config-workspace page-section">
    <div class="page-header">
      <div>
        <p class="eyebrow">系统管理 / 基础配置</p>
        <h1>基础配置</h1>
        <p class="page-desc">集中维护合同分类、模板、外部连接与角色权限，各配置项按业务闭环分节展开。</p>
      </div>
    </div>

    <div class="base-config-accordion">
      <section v-for="section in sections" :key="section.key" class="config-accordion-item" :class="{ open: active === section.key }">
        <button class="config-accordion-head" :aria-expanded="active === section.key" @click="toggle(section.key)">
          <span class="config-accordion-title">
            <b>{{ section.title }}</b>
            <small>{{ section.desc }}</small>
          </span>
          <ChevronDown class="config-accordion-chevron" :class="{ rotated: active === section.key }" :size="18" />
        </button>
        <div v-show="active === section.key" class="config-accordion-body">
          <CategoryWorkspace v-if="section.key === 'categories'" :api="api" :role="role" />
          <TemplateWorkspace v-else-if="section.key === 'templates'" :api="api" :role="role" :types="types" />
          <PartyConfigWorkspace v-else-if="section.key === 'parties'" :api="api" :role="role" />
          <AiConfigWorkspace v-else-if="section.key === 'ai'" :api="api" :role="role" />
          <ConnectionWorkspace v-else-if="section.key === 'connections'" :api="api" :role="role" />
          <PermissionAuditWorkspace v-else-if="section.key === 'permissions'" :api="api" :role="role" :types="types" />
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.base-config-accordion {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}
.config-accordion-item {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}
.config-accordion-head {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 0;
  padding: 18px 22px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.config-accordion-head:hover {
  background: #f8fafc;
}
.config-accordion-title {
  display: grid;
  gap: 5px;
}
.config-accordion-title b {
  color: #344054;
  font-size: 15px;
  font-weight: 600;
}
.config-accordion-title small {
  color: #98a2b3;
  font-size: 12px;
}
.config-accordion-chevron {
  flex: 0 0 auto;
  color: #98a2b3;
  transition: transform .18s ease;
}
.config-accordion-chevron.rotated {
  transform: rotate(180deg);
}
.config-accordion-body {
  border-top: 1px solid #eef0f3;
}
/* 展开后隐藏子工作区的独立 page-header 与 padding，避免双重标题与内边距 */
.config-accordion-body :deep(.page-section) {
  padding: 0;
}
.config-accordion-body :deep(.page-header) {
  display: none;
}
.config-accordion-body :deep(.category-layout),
.config-accordion-body :deep(.template-layout),
.config-accordion-body :deep(.connection-layout),
.config-accordion-body :deep(.permission-layout) {
  margin-top: 0;
}
</style>
