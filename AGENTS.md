# Hrcontract Engineering Guide

## Project focus

This repository is the active home for subsequent contract-system implementation. Use
`D:\WorkProject\ContractManage` as a read-only product/design reference unless the user explicitly
asks to change it. Do not copy its RuoYi/Element Plus runtime into this React application.

## Read before material frontend work

1. `Memory.md` and `项目开发进度与变更记录.md`
2. `合同管理系统需求设计.md`
3. `docs/FRONTEND_DESIGN_SYSTEM.md`
4. `docs/HEROUI_PRO_INTEGRATION.md`
5. current source, API behavior, and the narrowest relevant tests

## DESIGN, ANIMATION & UI PRINCIPLES
* The UI must feel intentional, coherent, and visually confident—not template-like or generically “AI-generated.”
* Establish a clear visual system before implementation: typography, spacing, color, radius, elevation, iconography, and interaction states must be consistent across the entire experience.
* Follow the existing product’s design language and interaction patterns. Avoid introducing new visual conventions unless explicitly required.
* Every screen should have a clear primary action, readable hierarchy, and strong information grouping. Remove noise before adding decoration.
* Use motion only when it improves comprehension, feedback, or continuity. Avoid decorative animation that competes with task completion.
* Prefer subtle, performant transitions for state changes, overlays, navigation, and feedback. Respect reduced-motion preferences.
* Fully implement all relevant UI states: loading, empty, error, disabled, success, hover, focus, active, and selected.
* Design responsively across screen sizes. Never assume a single viewport.
* Preserve accessibility: semantic structure, keyboard navigation, visible focus, sufficient contrast, accessible labels, and reduced-motion support.
* Use the project’s existing icon system. Do not mix unrelated icon styles.
* Before creating new components, inspect the codebase and reuse existing patterns when they are structurally appropriate.
* Validate final UI quality in the real rendered environment, not only by reading code.

## Frontend runtime boundary

- The user approved the React migration on 2026-09-11. The active runtime is React 19,
  TypeScript, Vite, Tailwind CSS v4, HeroUI v3 and `lucide-react`.
- Resolve HeroUI Pro components through `@hero-local/*` from the licensed local source at
  `D:\WorkProject\HeroUIPro\herouipro-v3\src\components` when authenticated package artifacts
  are unavailable. Do not replace them with guessed lookalike components.
- Query the official HeroUI Pro MCP before adding a Pro component and preserve its compound API,
  semantic states and accessibility behavior.
- Business pages own permissions, contract versions, lifecycle transitions and audit behavior.
  A visual component must never approve, sign, release or change contract state by itself.

## Verification

- Run `npm run build` after material frontend changes. Keep routine testing narrow unless the user requests a full suite.
- For user-flow claims, run the real frontend/backend and verify the browser route. A build alone is
  not end-to-end evidence.
- Never expose credentials, private contract content or production data in screenshots, logs or fixtures.
