# Rule: UI/UX & Date-fns Standards

## §1 Visuals & Styling
- Fonts: Display (`Outfit`), Body (`Inter`), Monospace (`JetBrains Mono`).
- Colors: HSL variables only. Banned: inline hex or hardcoded Tailwind colors. Frosted glass uses `backdrop-filter: blur(16px)`.
- UI: Interactive elements require hover/active/focus/disabled states. Toast alerts for API notices. Mobile-first layout; touch targets >= 44px.

## §2 Date-fns Standard [ERR-108]
- Library: `date-fns` is exclusive. Banned: `moment.js`, `dayjs`, `luxon`.
- Banned patterns:
  - `new Date().toISOString()` -> use `formatISO(new Date())`.
  - Native `toLocaleTimeString()` / `toLocaleDateString()` (causes SSR hydration mismatch).
  - Native `.getMonth()`, `.getDate()` -> use date-fns helpers.
  - Manual regex parsing -> use `parseISO` or `parse`.
- Allowed exceptions: `Date.now()` (state keys), `new Date().getFullYear()` (copyrights).

## §3 Accessibility & i18n
- Elements: Unique `id` on interactive controls. Associated `<label>` for forms. `aria-label` for icons.
- Translation: No hardcoded JSX text. Fetch keys from `TranslationDictionary`. Allow space for French translation expansion (+30%).
