# 10 — Frontend (React SPA + PWA)

Directory `frontend/`. One application, modular by bounded context, served by Nginx
(compose/k8s) and talking only to `gateway-service` (`/api/<module>/…`).

> Design goal stated by the product owner: **modern, easy to interact with, responsive
> on any screen size, and usable by a person who has never used a screen**. Every
> decision below is subordinate to that goal: big targets, one task per screen, words +
> pictograms, no hidden gestures, strong feedback, undo instead of confirm dialogs where
> safe, RO/EN.

---

## 1. Stack (all packages are to be installed with `pnpm add`; pin versions published ≥ 7 days)

| Concern | Choice |
|---|---|
| Build | Vite 5 + React 18 + TypeScript (strict), `pnpm` workspaces (single app now; `packages/` ready) |
| UI kit | **MUI v6** (`@mui/material`, `@mui/icons-material`, `@mui/x-data-grid`, `@mui/x-date-pickers` + `dayjs`, `@mui/x-charts` or **Recharts** for KPI charts) with a custom theme (§3) |
| Routing | `react-router` v6 (data routers, lazy routes per module) |
| Server state | `@tanstack/react-query` v5 (+ devtools), `axios` with interceptors (auth refresh, error envelope → toast/i18n) |
| Client state | `zustand` (auth/session, UI prefs, offline queue) |
| Forms | `react-hook-form` + `zod` (+ `@hookform/resolvers`); generated zod schemas from OpenAPI where possible |
| API types | `openapi-typescript` + `openapi-fetch` **or** `orval` generating typed hooks per module from each service's `/v3/api-docs` (script `pnpm gen:api`) |
| i18n | `react-i18next` + `i18next-browser-languagedetector`, namespaces per module, RO default, EN; ICU plurals via `i18next-icu` |
| Dates/numbers | `dayjs` (ro/en locales, `Europe/Bucharest`), `Intl.NumberFormat` helpers (kg, L, %, RON) |
| Tables | MUI X DataGrid (desktop) + custom `CardList` (mobile) switching by breakpoint |
| Charts | Recharts (responsive containers, accessible tooltips) |
| PWA/offline | `vite-plugin-pwa` (Workbox), IndexedDB via `dexie` for offline queue & reference data cache |
| Realtime | native `EventSource` (SSE) to `/api/notification/stream`, reconnect w/ backoff |
| Barcode/QR | `@zxing/browser` for scanning lot/stock-unit codes with the camera; `qrcode` for printing codes |
| Printing | print CSS routes for labels/notes + server PDFs |
| Testing | `vitest` + `@testing-library/react` + `msw` (API mocks), `playwright` e2e, `@axe-core/playwright` accessibility checks, Storybook 8 for the design system |
| Quality | ESLint (typescript-eslint, react, jsx-a11y, import order), Prettier, `knip` (dead code), Husky + lint-staged, `commitlint` |

Folder layout:
```
frontend/
├── src/
│   ├── app/                 providers (theme, query, i18n, router, auth), routes.tsx, layout/
│   ├── shared/
│   │   ├── api/             axios client, auth refresh, error mapping, generated clients per module
│   │   ├── auth/            session store, RequireRole, usePermissions, scope helpers
│   │   ├── ui/              design-system components (§4)
│   │   ├── forms/           Field wrappers, StepperForm, NumberPad, QuantityInput, DatePick
│   │   ├── offline/         dexie db, queue, sync engine, OfflineBanner
│   │   ├── i18n/            setup, locales/{ro,en}/<ns>.json, error-code map
│   │   ├── hooks/           useBreakpoint, useSiteContext, useDuty, useScanner
│   │   └── utils/           format, codes, dates
│   ├── modules/
│   │   ├── identity/        login, mfa, pin, profile, sessions
│   │   ├── admin/           users, roles, assignments, employees, rosters, settings, catalogues
│   │   ├── parent-flock/
│   │   ├── hatchery/
│   │   ├── farm/
│   │   ├── slaughterhouse/
│   │   ├── notifications/   alert center, bell, ack dialog
│   │   └── reporting/       dashboards, trace, reports
│   └── main.tsx
├── public/ (icons, manifest)
├── e2e/ (playwright)
└── .storybook/
```
Each module: `routes.tsx`, `api/` (generated + hand-written hooks), `pages/`,
`components/`, `forms/schemas.ts`, `i18n/{ro,en}.json`.

## 2. UX principles (binding)

1. **One primary action per screen**, bottom-right (desktop) or full-width bottom bar
   (mobile). Button labels are verbs with icons: "Salvează fișa", "Trimite".
2. **Touch targets ≥ 48×48 px**, spacing ≥ 8 px; font base 16 px, min 14 px; inputs
   height 56 px on mobile ("large" density), 40 px desktop.
3. **Guided flows (wizard)** for every data-entry task longer than 5 fields: step list
   visible, one topic per step, big "Înapoi/Continuă", summary step before submit.
4. **Numbers via NumberPad**: numeric inputs open a large on-screen keypad with
   +/− steppers (`QuantityInput`), unit shown inside the field, live validation with
   plain-language messages ("Prea mare: casa are 20 000 păsări").
5. **Pictogram + word everywhere** (mortality = dead-bird icon + "Mortalitate"). Icon
   set: MUI icons + custom poultry SVG set (`shared/ui/icons/poultry/*`: egg, chick,
   bird, feed bag, water drop, syringe, truck, thermometer, carcass, snowflake).
6. **Colour is never the only signal**: status chips carry icon + text; red/amber/green
   plus shape.
7. **Read-back confirmation**: after submit show a short summary card ("Ați înregistrat
   12 morți, 1 850 kg furaj…") with "Corectează" (undo window 5 min where allowed).
8. **No destructive confirm dialogs for routine work**; soft-delete/undo via snackbar.
   Only irreversible actions (dispatch, close lot, sign FCI) use a full-screen
   confirmation with checklist.
9. **Context is always visible**: top app bar shows current site + house/lot + date in a
   `ContextBar`; switching is a big selector with photos/colours per house.
10. **Offline-first for field work**: daily sheets, egg rounds, weights, readings queue
    locally and sync; the banner shows "Fără internet – 3 fișe în așteptare".
11. **Language toggle visible on login and in profile** (RO/EN); all text from i18n,
    no hard-coded strings (ESLint rule `i18next/no-literal-string`).
12. **Accessibility**: WCAG 2.1 AA, keyboard navigation, `aria-*`, focus rings,
    `prefers-reduced-motion`, high-contrast theme option, text scaling to 200 % without
    overflow (tested).
13. **Help per screen**: "?" opens a short illustrated help sheet (markdown in
    `help/ro/*.md`) and optionally a 20-second looping GIF/video of the task.
14. **Kiosk mode** for shared tablets in houses: PIN login, auto-logout 10 min, large
    home grid of tasks ("Fișa de azi", "Cântărire", "Alerte").

## 3. Theme & responsive system
- MUI theme: primary deep green `#1B5E20`, secondary amber `#F9A825`, error `#C62828`,
  success `#2E7D32`, info `#0277BD`; neutral greys; border radius 12; shadows soft;
  typography Inter (variable, self-hosted) with RO diacritics; density switch
  (`comfortable` default on touch devices, `compact` optional on desktop).
- Breakpoints: `xs <600` (phone, 1 column, bottom nav), `sm 600–900` (tablet portrait,
  2 columns, bottom nav), `md 900–1200` (tablet landscape, rail nav), `lg ≥1200`
  (desktop, permanent drawer). `useBreakpoint()` returns `phone|tablet|desktop`.
- Layout: `AppShell` = `TopBar` (logo, ContextBar, alert bell, user) + `Nav` (drawer /
  rail / bottom tabs with ≤ 5 items per module) + `<Outlet/>` + `OfflineBanner` +
  `Snackbar` host.
- DataGrid on desktop; the same `columns` definition renders a `CardList` on
  phone/tablet (`ResponsiveTable` component) with the first 3 columns as card title/
  subtitle/badge and an expandable detail.
- Dark mode supported but default light (barn lighting); high-contrast toggle.

## 4. Design-system components (`shared/ui`, each with Storybook story + tests)
`AppShell`, `ContextBar`, `SiteSwitcher`, `HouseGrid` (tiles with status colour, birds,
age, alert badge), `StatusChip`, `KpiTile` (value, unit, trend arrow, vs standard),
`TrendChart`, `StandardBandChart` (actual vs breed standard band), `ResponsiveTable`,
`FilterBar` (chips + drawer on mobile), `StepperForm`, `NumberPad`, `QuantityInput`,
`DateStrip` (horizontal day picker with today highlighted), `EmployeePicker` (avatars,
on-duty first), `CodeScanner`, `PhotoCapture`, `SignaturePad` (vet signature on FCI),
`AlertBanner`, `AckDialog` (reason required), `EmptyState` (illustration + one action),
`ConfirmChecklist`, `PrintButton`, `ExportMenu` (CSV/XLSX/PDF), `HelpSheet`,
`LanguageToggle`, `OfflineBanner`, `SyncStatus`, `PermissionGate`.

## 5. Cross-cutting features
- **Auth flow**: login (email/username + password) → MFA step → forced password change
  → home. Access token in memory, refresh token in `httpOnly` cookie? → gateway does not
  set cookies; keep refresh token in `localStorage` encrypted-at-rest is not possible —
  **decision**: store refresh token in `localStorage` with device binding header and
  rotation (already server-side); clear on logout; kiosk devices never store refresh
  beyond session. Silent refresh 60 s before expiry; 401 → single-flight refresh → retry.
- **Permission model in UI**: `usePermissions()` exposes `can(perm, scopeType, scopeId)`;
  routes guarded by `RequireRole`; buttons hidden (not just disabled) when not allowed;
  MANAGER sees everything read-only (forms render as read-only summaries).
- **Site context**: after login, user picks site (if > 1) → persisted; all queries pass
  `siteId`; ContextBar always shows it.
- **Error handling**: envelope `code` → `t('errors.' + code, params)`; 422 shows an
  inline `AlertBanner` with details (e.g. withdrawal end date); 403 scope → explanation;
  409 version conflict → "Altcineva a modificat; reîncarcă" with diff of fields.
- **Notifications**: bell with unread count (SSE), Alert Center page (filters by
  severity/site), CRITICAL ack dialog with mandatory reason; web-push opt-in.
- **Offline queue**: `dexie` tables `queue(ops)`, `refdata(cache)`, `drafts`; mutations
  marked `offlineCapable` in hooks get queued with client UUID (idempotency key sent as
  `Idempotency-Key` header; backend sheets accept it); conflict (`409`) → surfaces in
  `SyncStatus` with manual resolution screen; refdata (houses, lots, employees, feed
  types, medications) cached daily.
- **Scanning**: lot codes, stock unit codes, egg batch codes on printed labels (QR);
  `CodeScanner` → navigates to entity or fills field.
- **Printing/PDF**: server PDFs opened in new tab via `?access_token=` short-lived
  (document token endpoint) or blob fetch; label printing CSS for 100×50 mm labels.
- **Exports**: every list has `ExportMenu` calling `…/export?format=`.
- **Telemetry**: `web-vitals` + error boundary reporting to `/api/reporting/client-logs`
  (optional), Sentry-compatible adapter interface.

## 6. Module screens (routes, roles, key components). Build order: identity → admin
(minimum: users/roles/assignments/settings) → **farm** → notifications → hatchery →
parent flock → slaughterhouse → reporting → remaining admin (HR, catalogues).

### 6.1 identity (`/login`, `/mfa`, `/pin`, `/forgot`, `/reset/:token`, `/activate/:token`, `/profile`, `/profile/sessions`, `/profile/security`)
Large single-column forms; language toggle; "Arată parola"; kiosk PIN screen = big
employee tiles + numeric pad.

### 6.2 admin (`/admin/...`, SYSTEM_ADMIN / HR_ADMIN / site managers partially)
- `users` list + `users/:id` (profile, status actions, access tab with assignments by
  site shown as cards: role chip + site + validity; "Adaugă acces" wizard: pick role →
  pick site (filtered by role's scope types) → validity).
- `roles` matrix view (roles × permissions grouped by module with toggle switches,
  system roles locked), `sites` directory (read), `settings` (grouped by module; each
  setting as a card with description RO, unit, default, per-site override table,
  history drawer), `feature-flags`.
- HR: `employees` (cards with photo, position, site, status), `employees/:id`
  (tabs: profile, certifications with expiry badges, absences, user link),
  `rosters` (week grid: rows employees, columns days, drag shift chips on desktop;
  tap-to-assign on mobile; publish button; copy previous week), `duty-today` board.
- Catalogues: breeds + standards (table + curve preview chart + CSV import dialog),
  suppliers, medications (withdrawal days highlighted), vaccines, feed types, units.

### 6.3 farm (`/farm/...`)
- **Home / Today board** (`/farm`): `HouseGrid` for the selected farm: each tile shows
  house code, lot age (zi 23), live birds, today's sheet status (missing/draft/
  submitted), red badge for alerts, quick actions. Role FARM_WORKER sees only own houses.
- **Daily sheet wizard** (`/farm/houses/:houseId/sheet/:date`): steps: 1 Mortalitate
  (dead/culled with cause tiles + NumberPad; disposal), 2 Furaj (feed type pre-selected,
  kg, silo), 3 Apă (meter reading → litres auto), 4 Climat (min/max temp, RH, NH₃
  optional), 5 Observații (behaviour tiles, litter score 1–5 stars, free text, photo),
  6 Sumar & Trimite → read-back card. `DateStrip` to switch day; offline capable.
- **Lot page** (`/farm/lots/:id`): header (code, house, age, birds, target), KPI tiles
  (mortality, ADG, FCR, EPEF, est. weight, density) with trend vs standard
  (`StandardBandChart`), tabs: Zilnic (sheets table), Greutăți, Tratamente & timp de
  așteptare (timeline bar showing blocked period), Livrări, Costuri, Trasabilitate,
  Jurnal. Eligibility banner: green "Poate fi livrat" or red list of reasons.
- **Weight sampling** (`/farm/lots/:id/weigh`): tally UI — big "+ pasăre" entries with
  weight (or total + count), running average, uniformity gauge; FARM_WEIGHER home
  shortcut.
- **Vet** (`/farm/vet`): visits, treatments wizard (lot → medication (search with
  withdrawal days shown) → dose calculator (birds × dose → total, stock check with
  batches) → dates → summary showing computed withdrawal end date in red), lab
  samplings, disease events, treatment register print.
- **Reception** (`/farm/receptions/new`): from announcement card (hatchery, order,
  planned qty) → counting (boxes, sample counts, DOA, weak, weight) → house check
  (capacity, READY) → confirm → lot created screen with printable lot QR.
- **Deliveries** (`/farm/deliveries`): list by status (kanban on desktop), wizard
  create (lot → type → slaughterhouse → date → planned birds/weight; eligibility
  evaluated live and blocking with explanation), order page with timeline
  (planned → approved → confirmed slot → feed withdrawal (timer "furaj oprit de 6 h
  20 min") → FCI (generate/review/sign with `SignaturePad`/send) → trucks loading
  (add truck card, birds per truck, weights) → dispatch → received feedback).
- **Sanitation** (`/farm/houses/:id/sanitation`): checklist timeline; set READY.
- **Inventory** (`/farm/inventory`): stock cards with level bar vs reorder point,
  receipt wizard (scan delivery note photo), batch QA, counts.
- **Costs** (`/farm/lots/:id/costs`): per category donut, cost/kg, manual entry.
- **Farm KPIs** (`/farm/kpi`): lots comparison table + charts; export.

### 6.4 hatchery (`/hatchery/...`)
Home: **Incubator board** (setters/hatchers tiles with batch, day of incubation,
next action due, temp/RH last reading, alarm badge) + today's task list (candling due,
transfers due, hatches due, expected egg arrivals). Screens: incoming eggs → reception
wizard (check sample → accept/downgrade/reject), egg store (batches by age colour,
FEFO), setting wizard (pick setter → pick egg batches FEFO-suggested → qty per trolley →
program → confirm → planned dates), batch page (timeline day 0–21, readings chart,
candling entry with tray-based counter, transfer, hatch & grading entry with
reconciliation helper, waste), chick batches (available/boxed, QC holds), chick
transfer wizard (farm/house announced by `lot.planned` cards → grades/qty → transport →
approve/dispatch → delivery note), vaccinations, readings entry (per shift form or
sensor view), sanitation, inventory, costs, KPIs (per origin flock chart).

### 6.5 parent-flock (`/parent-flock/...`)
Home: **House board** (flock code, age weeks, hens/males, today's laying %, sheet
status). Daily sheet wizard: 1 Mortalitate (hens/males), 2 Furaj, 3 Apă, 4 **Ouă pe
runde** (add round: time, total, settable, floor, cracked, dirty… with running laying
% display), 5 Climat/Observații, 6 Sumar. Egg room: batches/age, inspections, store
readings, downgrade; weekly body weights tally; health (vaccination schedule calendar
with due badges, vaccination entry, treatments with egg withdrawal warning, disease,
biosecurity checklist, samplings); egg transfer orders (wizard + delivery note);
flock page (KPIs vs standard: laying curve, hatching-egg %, eggs/hen housed, body
weight, uniformity); inventory; costs (cost per hatching egg).

### 6.6 slaughterhouse (`/slaughterhouse/...`)
Home: **Day board**: kill plan timeline per line (slots with farm/lot, status), trucks
arrived/in lairage with timers (rest min reached ✔), open sessions, cold store occupancy
gauges, open CRITICAL alerts. Screens: kill plan (drag slots desktop, list mobile;
publish), reception wizard (truck → FCI check (view PDF) → weighing gross (scale value or
manual) → lairage start → DOA → tare → summary), ante-mortem form (inspector; passed/
condemned counters by cause; decision tiles; hold), session page (open → parameters →
counters → CCP entries with limit indicators → post-mortem causes grid → grading →
cuts table with yield % live → packaging runs → close summary with yield gauge),
cold store map (chambers as cards with temp, occupancy; stock units list; scan to move/
block), QA (holds, breaches with decision, micro samplings), sales: customers,
price lists (grid editor), sales orders (lines with price resolve), pick list (FEFO
suggestions, scan to confirm), dispatch orders (loading screen: scan units, temps,
seals; approve; dispatch; print aviz), invoices (issue from dispatches, preview,
export), claims; inventory; costs; KPIs.

### 6.7 notifications (`/alerts`, bell drawer)
Alert Center: filters (severity chips, site, status), list cards (icon, title with
params, site, time, occurrences), detail with link to entity, ack dialog with reason,
resolve; preferences page.

### 6.8 reporting (`/dashboard`, `/trace`, `/reports`)
Manager dashboard: company map/sites overview tiles, KPIs vs previous period,
upcoming deliveries, cold-store occupancy, CRITICAL alerts; drill down to site
dashboards. Traceability: search box / scan code → chain visualised as a vertical
timeline with nodes (flock → eggs → incubation → chicks → lot → session → product →
customer) and expandable details; print. Reports catalogue: run (parameters form) →
document list; schedules.

## 7. PWA & devices
- `manifest.webmanifest` (name "PPMS", icons, `display: standalone`, theme colour),
  Workbox: precache app shell; runtime: `NetworkFirst` for API GET lookups with 24 h
  fallback; `BackgroundSync` not relied upon — custom queue in dexie with manual
  "Sincronizează" and auto on `online`.
- Install prompt banner on tablets; kiosk setup page (`/kiosk/setup` SYSTEM_ADMIN
  enters device token) stores device token; `pin-login` flow.
- Camera permissions UX; large-print mode toggle (font ×1.25).
- Tested matrix: Chrome Android (phone 360×800, tablet 800×1280), iPad Safari, desktop
  Chrome/Edge/Firefox 1366×768 & 1920×1080.

## 8. i18n
Namespaces: `common`, `errors` (one key per backend `code`), `alerts` (per alertCode),
`identity`, `admin`, `farm`, `hatchery`, `parentFlock`, `slaughterhouse`,
`reporting`, `help`. RO is source of truth; EN complete before release; missing-key
reporter in dev; dates `dd.MM.yyyy`, numbers `1.234,5` in RO.

## 9. Testing & quality
- Unit/component: vitest + RTL for every design-system component and form schema; msw
  handlers generated from OpenAPI examples.
- E2E (Playwright) critical journeys: login+MFA; farm worker daily sheet (phone
  viewport, offline then sync); vet treatment then blocked delivery message; manager
  approves delivery after withdrawal; hatchery full cycle (seeded); slaughterhouse
  reception → dispatch; alert ack. Each run also executes `axe` scans (no serious
  violations) and visual snapshots per breakpoint.
- Lighthouse CI budgets: PWA installable, performance ≥ 85 on mobile for home boards,
  accessibility ≥ 95.
- Storybook with `a11y` addon; Chromatic optional.

## 10. Task list
| # | Task | Acceptance |
|---|---|---|
| FE0 | Scaffold Vite+TS+pnpm, ESLint/Prettier/Husky, MUI theme, i18n, router, query client, axios + auth refresh, `gen:api` script, Storybook, vitest, Playwright, PWA plugin | `pnpm dev` shows login; CI green |
| FE1 | Design system components (§4) with stories + tests; AppShell responsive at 4 breakpoints | Storybook review on phone/tablet/desktop |
| FE2 | identity module (login, MFA, PIN kiosk, forgot/reset/activate, profile, sessions, forced change) | e2e |
| FE3 | admin core: users, access assignments wizard, roles matrix, settings, sites | e2e create user + grant FARM_WORKER |
| FE4 | farm: Today board, daily sheet wizard (offline), lot page, weights | e2e phone offline sync |
| FE5 | farm: vet (treatments wizard), reception wizard, deliveries + FCI + sign, sanitation, inventory, costs, KPIs | e2e withdrawal block |
| FE6 | notifications: bell, SSE, alert center, ack | e2e |
| FE7 | hatchery module | e2e cycle |
| FE8 | parent-flock module | e2e daily sheet with egg rounds |
| FE9 | slaughterhouse module (ops) | e2e reception→session |
| FE10 | slaughterhouse sales (customers, prices, orders, picking, dispatch, invoices, claims) | e2e dispatch refuses expired |
| FE11 | reporting: dashboards, trace, reports | e2e trace from dispatch code |
| FE12 | admin HR (employees, rosters, duty board) + catalogues | e2e roster publish |
| FE13 | Help sheets, kiosk mode, high-contrast/large print, Lighthouse budgets, EN translation complete | audits pass |

## Exit criteria
Every backend endpoint used by a flow has a screen; all journeys pass e2e on phone and
desktop viewports; axe no serious issues; RO/EN complete; PWA installable; offline daily
sheet demo works.
