# Logout Redirect Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make clicking "退出登录" immediately navigate to the Login page instead of staying on Settings.

**Architecture:** Single-line change in the Settings page's Logout handler: replace `router.push({ name: 'Today' })` with `router.replace({ name: 'Login' })`. No store, router guard, or other files are touched.

**Tech Stack:** Vue 3 + Vue Router (auto-routes) + Vant UI

---

### Task 1: Apply the navigation fix

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/settings/index.vue:19`

- [ ] **Step 1: Edit the Logout function**

```diff
-      router.push({ name: 'Today' })
+      router.replace({ name: 'Login' })
```

- [ ] **Step 2: Verify the file compiles and lints**

Run in terminal (PowerShell):
```powershell
cd front/vue3-vant-mobile
npm run lint -- --fix src/pages/settings/index.vue
```
Expected: No errors related to this file.

- [ ] **Step 3: Manual smoke test (no automated test needed per ponytail rule for trivial one-liner)**

1. Run dev server if not running.
2. Log in → go to Settings → click "退出登录".
3. Confirm: immediately lands on Login page (no refresh required, no flash of Settings).
4. Log in again to confirm login flow still works.

- [ ] **Step 4: Commit**

```bash
git add front/vue3-vant-mobile/src/pages/settings/index.vue
git commit -m "fix: logout now directly navigates to Login page"
```