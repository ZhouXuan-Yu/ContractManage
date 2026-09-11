/**
 * Design-time bridge to HeroUI Pro.
 *
 * This Vue application must not import React packages. The map records the official
 * HeroUI concepts that each local pattern follows, while local components keep the
 * runtime replaceable and preserve contract-domain behavior.
 */
export const HERO_UI_PRO_REFERENCE = {
  appShell: ['app-layout', 'sidebar'],
  pageHeader: ['breadcrumbs', 'toolbar'],
  dashboardMetrics: ['kpi', 'kpi-group', 'widget'],
  ledger: ['data-grid', 'search-field', 'pagination', 'chip'],
  contextualActions: ['action-bar', 'sheet'],
  creation: ['stepper', 'form', 'text-field', 'select', 'date-picker'],
  detail: ['tabs', 'timeline', 'tooltip'],
  files: ['drop-zone', 'list-view', 'progress-bar'],
  emptyAndFailure: ['empty-state', 'alert', 'toast', 'skeleton'],
} as const

export type HeroUiReferencePattern = keyof typeof HERO_UI_PRO_REFERENCE

export const HERO_UI_TOKEN_ALIAS = {
  background: '--background',
  foreground: '--foreground',
  surface: '--surface',
  surfaceSecondary: '--surface-secondary',
  muted: '--muted',
  accent: '--accent',
  success: '--success',
  warning: '--warning',
  danger: '--danger',
  border: '--border',
  focus: '--focus',
} as const
