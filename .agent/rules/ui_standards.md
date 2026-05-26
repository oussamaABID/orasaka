# Rule: UI/UX & Frontend Standards

## §1 Design System

- **Typography**: Display (`Outfit`), Body (`Inter`), Monospace (`JetBrains Mono`).
- **Colors**: HSL variables exclusively. Banned: inline hex colors or hardcoded Tailwind color classes. Frosted glass panels use `backdrop-filter: blur(16px)`.
- **Interactions**: All interactive elements require `hover`, `active`, `focus`, and `disabled` states. Toast alerts for API notifications. Mobile-first layouts with touch targets ≥ 44px.
- **Icons**: Centralized Lucide icons from `Icon.tsx`. Banned: direct imports from external icon libraries.

## §2 Date-fns Standard (ERR-108)

- **Library**: `date-fns` is the exclusive date formatting library.
- **Banned libraries**: `moment.js`, `dayjs`, `luxon`.
- **Banned patterns**:
  - `new Date().toISOString()` → use `formatISO(new Date())`
  - Native `toLocaleTimeString()` / `toLocaleDateString()` (causes SSR hydration mismatch)
  - Native `.getMonth()`, `.getDate()` → use date-fns helpers
  - Manual regex date parsing → use `parseISO` or `parse`
- **Allowed exceptions**: `Date.now()` for state keys, `new Date().getFullYear()` for copyright notices.

## §3 Accessibility & i18n

- **Elements**: Unique `id` attributes on all interactive controls. Associated `<label>` for form fields. `aria-label` for icon-only buttons.
- **Translation**: No hardcoded JSX text. Fetch display strings from `TranslationDictionary`. Allow +30% space for French translation expansion.

## §4 Workspace Architecture

All client applications are grouped under `orasaka-apps/orasaka-ui/` as an npm workspace:

- **`orasaka-web-client/`**: Client-facing Next.js 16 App Router application (port 3000). Cinematic dark-mode, React 19, input-blocking.
- **`orasaka-web-admin/`**: Isolated SecOps Administration Console (port 3001). Must never share runtime state with `orasaka-web-client`.
- **`orasaka-mobile-client/`**: Expo SDK 53 cross-platform mobile app. 6-screen typed navigation stack — Login, Register, ForgotPassword, ResetPassword, ChatStream, Subscription.
- **`orasaka-cli/`**: Developer automation CLI. All UI logging must use `logWarning`/`logError` from `ui/prompts.ts` — never raw `console.log`.
- **`orasaka-shared/`**: Shared TypeScript types and Zod validation schemas. All client packages import types from here — zero type duplication across packages.

## §5 Input Blocking (ERR-126)

Text entry surfaces must lock when `isSending || isGenerating`. This applies to **all** client platforms (web, admin, mobile):
- Disable textareas, menus, attachments, and submit buttons.
- Applies to both web (`orasaka-web-client`, `orasaka-web-admin`) and mobile (`orasaka-mobile-client`).

## §6 Form Event Typing

Form `onSubmit` handlers must use React 19+ native types:
- `React.SubmitEventHandler<HTMLFormElement>` or `React.SubmitEvent<HTMLFormElement>`
- **Banned**: `React.FormEvent`
