# Logout Redirect Design

**Date**: 2026-06-24  
**Status**: Approved for implementation

## Problem

Clicking "退出登录" in Settings does not navigate to the Login page. The app remains on the Settings page until a manual refresh triggers the router guard. Root cause: `Logout()` calls `router.push({ name: 'Today' })` after clearing the token, relying on the `beforeEach` guard to redirect. This guard redirect does not reliably update the URL in the current navigation context.

## Decision

Adopt **方案 1** (minimal change): replace the post-logout navigation target with a direct `router.replace({ name: 'Login' })`.

- No redirect query parameter is needed (user explicitly wants default behavior to be the Login page).
- `replace` is preferred over `push` to avoid leaving an unauthenticated page in browser history.

## Change

**File**: `front/vue3-vant-mobile/src/pages/settings/index.vue`

```diff
-      router.push({ name: 'Today' })
+      router.replace({ name: 'Login' })
```

All other logic (`userStore.logout()`, confirmation dialog, etc.) remains unchanged.

## Verification

After the change:
1. Click "退出登录" → immediately lands on Login page.
2. No manual refresh required.
3. Login flow continues to work (redirect query still supported for other entry points).

## Scope

Single-line change. No new dependencies, no store modifications, no router guard changes. YAGNI: we do not preserve "return to previous page" behavior on logout.