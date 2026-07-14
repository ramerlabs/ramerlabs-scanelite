# ScanElite — UI/UX & Functional Specification

**Product:** ScanElite  
**Company:** RamerLabs · [https://ramerlabs.com](https://ramerlabs.com)  
**Credits:** A product by RamerLabs · ramerlabs.com  
**Status:** Approved product configuration (v1.0.0-spec)  
**Default theme:** Premium Dark Mode  
**Machine-readable config:** [`config/scanelite.config.json`](../config/scanelite.config.json)  
**Brand assets:** [`brand/icon.png`](../brand/icon.png), [`brand/logo.png`](../brand/logo.png)

---

## 1. Product vision

ScanElite is a premium mobile document scanner that turns the phone camera into a high-end desk scanner experience:

1. Point at a document  
2. See real-time edge guidance  
3. Capture (manual or auto)  
4. Auto flatten + enhance to a “real scanner” crisp page  
5. Review / tweak pages  
6. Share instantly as PDF or JPEG to WhatsApp, Telegram, Facebook, or Email  

**Feel:** high-end, modern, fast, uncluttered — document always center stage.

---

## 2. Design system

### 2.1 Color palette (dark-first)

| Token | Hex | Usage |
|--------|------|--------|
| `bgPrimary` | `#14181F` | App chrome, camera veil |
| `bgElevated` | `#1C222C` | Sheets, panels, bottom bars |
| `bgSurface` | `#242B36` | Cards, grid tiles |
| `textPrimary` | `#F5F7FA` | Titles, primary labels |
| `textSecondary` | `#9AA3B2` | Hints, meta |
| `accentGold` | `#C9A227` | Primary CTAs, brand accent |
| `accentEmerald` | `#2ECC8A` | Success, “locked / ready to capture” |
| `edgeNeon` | `#4DA3FF` | Live edge overlay (default) |
| `edgeGold` | `#E6C35C` | Alternate edge style / premium lock |
| `danger` | `#E45D5D` | Delete / discard |
| `divider` | `#2E3644` | Hairline separators |

**Rules**
- Prefer flat surfaces over heavy gradients.  
- Soft 8–16dp radii; avoid bulky card chrome.  
- One strong accent per screen (gold CTA or emerald ready-state).  
- High contrast between document preview and UI chrome.

### 2.2 Typography

- Modern geometric/humanist sans (e.g. Plus Jakarta Sans / Inter-class).  
- Large titles only where needed (Home, Share hub).  
- Dense but readable labels in camera HUD (avoid wrapping).  
- Numeric page indexes use tabular figures when available.

### 2.3 Motion & feedback

| Moment | Motion |
|--------|--------|
| Edge lock | Overlay snaps from dashed → solid emerald, light haptic |
| Auto-capture | Soft shutter scale-in + brief freeze frame |
| Page add (batch) | Tile flies into bottom filmstrip |
| Filter switch | Crossfade ~180–220ms |
| Share sheet | Modal elevate from bottom, spring settle |

Keep transitions ≤ 300ms unless storytelling (export success).

### 2.4 Iconography & brand

- App icon: charcoal field + gold document mark + emerald scan cue (`brand/icon.png`).  
- In-app logo: wordmark + mark on splash / About (`brand/logo.png`).  
- Always show credits: **A product by RamerLabs · ramerlabs.com** on splash (subtle), Settings → About, and Share success footer.

---

## 3. Information architecture

```
Splash
 └─ Home (Library)
      ├─ New Scan → Camera
      │    ├─ Single / Batch toggle
      │    ├─ Capture → Process → Page Editor
      │    └─ Done → Review Grid → Export / Share Hub
      ├─ Open Document → Review Grid → Export / Share Hub
      └─ Settings / About (credits)
```

**Primary bottom actions while reviewing:** Edit · Filter · Export · Share  

**Avoid:** deep nested settings; prefer one-level sheets.

---

## 4. Screens & layouts

### 4.1 Splash

- Full dark charcoal.  
- Centered logo lockup.  
- Microcredit at bottom: `ramerlabs.com`.  
- Auto advance ~1.2s or tap to skip.

### 4.2 Home / Library

- Top: “ScanElite” wordmark (small) + search.  
- FAB or primary gold button: **New Scan**.  
- Grid/list of recent documents (thumbnail of page 1, title, page count, date).  
- Empty state: illustration of scan frame + CTA “Scan your first document”.

### 4.3 Camera & Capture (premium focus screen)

**Layout**
- Full-bleed camera preview.  
- Top HUD: Close · Flash · Grid · Help.  
- Center: live edge polygon overlay.  
- Bottom bar:  
  - Mode toggle: **Single** | **Batch**  
  - Large shutter (gold ring)  
  - Gallery pick (import image)  
  - Batch filmstrip (Batch only)

**Mode toggle**
- Segmented control, thumb-friendly, persists last mode.  
- Single: capture → process → page editor → finalize options.  
- Batch: capture loops; each accepted page appends to filmstrip; **Done** opens Review Grid.

**Real-time edge detection**
- Soft neon blue (`#4DA3FF`) polygon highlighting detected page borders.  
- When confidence high + frame stable: border transitions to emerald and thickens slightly.  
- Optional user setting: Gold edge style (`#E6C35C`).

**Auto-capture**
- Enabled by default (toggle in top HUD).  
- Triggers only when:  
  - 4 corners confident  
  - Motion below threshold for ~450–600ms  
  - Document fills ≥ ~45% of view  
- Show subtle “Hold steady…” → “Capturing…”  
- Manual shutter always available and overrides countdown.

**Accessibility**
- Large shutter hit target (≥56dp).  
- Announce mode changes via TalkBack labels.

### 4.4 Processing (brief intermediate)

- Non-blocking full-screen or overlay: “Enhancing…” with slim gold progress.  
- Pipeline order (see §5).  
- Skippable preview once first pass completes (Advanced: “Show original”).

### 4.5 Page Editor (post-capture)

- Full-bleed corrected page.  
- Bottom filter chips: **Magic Color** · **B&W** · **Original Photo**.  
- Tools: Recrop / Adjust corners · Rotate L/R · Retake · Delete.  
- Primary CTA:  
  - Single → **Continue** (Review or Share)  
  - Batch → **Add next** / **Done**

### 4.6 Review Grid (critical for Batch)

- Dark elevated surface.  
- Title: document name (editable).  
- Thumbnail grid (2–3 columns) with page badges.  
- Long-press / drag-and-drop reorder.  
- Per-page overflow: Rotate · Recrop · Filter · Delete.  
- Bottom sticky: **Export** · **Share**.

### 4.7 Export sheet

- Format: **PDF** (default for multi-page) · **JPEG** (single page default; multi → ZIP of JPEGs or page picker).  
- Quality: Standard / High (default High).  
- Filename field.  
- Confirm **Save to Files / Gallery** + optional open Share Hub.

### 4.8 Share Hub

Dedicated, highly accessible panel (half-sheet or full):

| Target | Behavior |
|--------|----------|
| WhatsApp | System share / WhatsApp intent with PDF or JPEG |
| Telegram | Telegram share intent |
| Facebook | Share sheet targeting Feed / Groups via system Facebook share |
| Email | `ACTION_SEND` with attachment; subject “ScanElite — {doc name}”; body short courtesy line + credits footer optional |

Always show format pills at top of Share Hub (PDF / JPEG) before one-tap targets.

### 4.9 Settings & About

- Auto-capture on/off  
- Edge style: Neon Blue / Gold  
- Default filter: Magic Color / B&W / Original  
- Default export: PDF / JPEG  
- Haptics on/off  
- **About / Credits:** logo, version, **A product by RamerLabs**, link `https://ramerlabs.com`

---

## 5. Functional requirements (processing pipeline)

### 5.1 Capture

| ID | Requirement |
|----|-------------|
| C1 | Support Single and Batch modes from camera UI |
| C2 | Real-time quadrilateral edge detection overlay |
| C3 | Auto-capture when frame stable and confidence high |
| C4 | Manual capture always available |
| C5 | Optional import from gallery into same pipeline |

### 5.2 Alignment & enhancement (“Real Scanner” output)

| ID | Requirement |
|----|-------------|
| P1 | Perspective transform: crop to quad, warp to rectangle |
| P2 | Flatten appearance (deskew / unwarp as supported by CV stack) |
| P3 | **Clear Copy / Magic Color:** whiten paper, deepen ink, reduce shadows |
| P4 | **B&W:** high-contrast binary-like text mode, readable |
| P5 | **Original Photo:** corrected geometry only, natural color |
| P6 | Processing must complete under ~1.5s/page on mid-tier devices when possible |

### 5.3 Management

| ID | Requirement |
|----|-------------|
| M1 | Review grid for all pages in a session/document |
| M2 | Reorder pages (drag) |
| M3 | Rotate 90° increments |
| M4 | Re-crop / corner adjust before finalize |
| M5 | Persist drafts locally (Room/SQLite or files + index) |

### 5.4 Export & share

| ID | Requirement |
|----|-------------|
| E1 | Export multi-page **PDF** |
| E2 | Export **JPEG** (high resolution) |
| E3 | One-tap share: WhatsApp, Telegram, Facebook, Email |
| E4 | Email opens prefilled compose with attachment |
| E5 | Share Hub reachable in ≤2 taps from Review |

---

## 6. User flows (happy paths)

### 6.1 Single Scan → PDF → WhatsApp

1. Home → **New Scan**  
2. Ensure mode = Single, auto-capture on  
3. Align page; edge goes emerald → auto shutter  
4. Magic Color applied by default  
5. Optional recrop → Continue  
6. Review (1 page) → Share → PDF → WhatsApp  

### 6.2 Batch Scan → reorder → Email

1. New Scan → Batch  
2. Capture N pages (filmstrip grows)  
3. **Done** → Review Grid  
4. Drag to reorder, rotate one page  
5. Share → PDF → Email  

### 6.3 Gallery import → B&W → JPEG save

1. Camera → gallery icon  
2. Select photo → perspective confirm  
3. Filter = B&W → Continue  
4. Export → JPEG → Save  

---

## 7. Component inventory

- `EdgeOverlay` — animated polygon (neon / gold / emerald states)  
- `ModeToggle` — Single | Batch  
- `ShutterButton` — gold ring + states (idle / locking / firing)  
- `BatchFilmstrip` — horizontal thumbnails + count badge  
- `FilterChipRow` — Magic Color / B&W / Original  
- `PageGrid` — reorderable review tiles  
- `ShareHubSheet` — format + four targets  
- `CreditsFooter` — RamerLabs · ramerlabs.com  

---

## 8. Non-functional requirements

- **Performance:** 60fps camera preview target; processing off UI thread.  
- **Privacy:** documents stored on-device by default; no silent upload.  
- **Permissions:** Camera (required), Photos/Media (import/export), optional Notifications.  
- **Reliability:** never lose batch pages on process failure — keep original + retry.  
- **Brand:** credits visible in About and splash; do not expose internal license-server URLs in customer UI.

---

## 9. Implementation notes (recommended Android stack)

Aligned with RamerLabs mobile practice:

- Kotlin + Jetpack Compose  
- CameraX for preview/capture  
- ML Kit Document Scanner **or** OpenCV-based quad detect + warp (choose during build phase)  
- Coil for thumbnails  
- Room for document index  
- Material 3 dark color scheme mapped to tokens above  
- System share intents for WhatsApp / Telegram / Facebook / Email  

---

## 10. Acceptance criteria (v1)

- [ ] Single & Batch modes work from one camera screen  
- [ ] Live edge highlight visible before capture  
- [ ] Auto-capture fires only when stable + confident  
- [ ] Output pages look flattened and “scanned,” not skewed photos  
- [ ] Magic Color / B&W / Original available  
- [ ] Review grid supports reorder, rotate, re-crop  
- [ ] Share Hub exports PDF/JPEG to WhatsApp, Telegram, Facebook, Email  
- [ ] Icon + logo ship with app; credits link to https://ramerlabs.com  

---

## 11. Out of scope (v1)

- Cloud OCR / searchable PDF (candidate v1.1)  
- Account sync / multi-device  
- Paid unlock walls (can integrate RamerLabs License Manager later)  
- iOS parity (Phase 2)

---

## Credits

**ScanElite** is a product of **RamerLabs**.  
Website: [https://ramerlabs.com](https://ramerlabs.com)  
Support: support@ramerlabs.com  

All rights reserved © RamerLabs.
