# Cozitopia UI redesign brief

You are a product designer. Design a new UI for **Cozitopia**, a small app for people who share a home ("homies"). It must be **clean, calm and lovely**: warm and friendly, but structured and uncluttered. Give the result as annotated mockups (Figma, HTML/CSS or images) for phone (390x844) and desktop (1280x800). Also give a short design-system sheet.

### What the app does
- Accounts with email and password. Language select: EN / DE / FR (German text is about 30% longer).
- **My homes**: list your homes (role Admin or Homie), see which home is current, switch homes, rename the current home (admin only), create a home, accept or decline invitations.
- **Homies**: members of the current home (name, email, role). Admins change roles, remove members and invite by email + role. Everyone can leave the home.
- **Checklist** ("Happy Home Checklist"): import chores from CSV (download template, upload, export). A table of tasks: what, where, frequency, duration, assignee(s), repetition, next start, pinned flag. It shows a workload summary per homie and has an "assign all tasks" action. This is the densest screen.
- **Calendar**: upcoming chore occurrences, filter chips (All, Me, each homie, Everyone, Unassigned), export as .ics.
- **Profile**: display name, preferred weekday, preferred start time and latest end time (half-hour steps).
- Error and info messages appear as snackbars.

### Keep
- The palette: cream background #FFF7EC, coral primary #C4533F, light coral #FBD9CC, sage secondary #3E7A56, light sage #CFE8D4, mustard accent #E9B43C, navy text #22304A, muted text #5B5057, warm outline #B8A89C.
- A serif display font (Fraunces) for headlines and a clean sans-serif for body text.
- Soft, rounded shapes.

### Remove / avoid
- Large illustrated banners at the top of each screen.
- Sticker strips and decorative stickers used as avatars.
- Stretched layouts: on desktop, the current bottom navigation spreads 5 items over 2000px, and the content floats in a thin column.
- Many equal-weight buttons next to each other.
- Emoji.

### Design requirements
1. **Clear hierarchy**: page title + one-line subtitle, then content in a few clear sections or cards. Only one primary (coral) action for each screen.
2. **Navigation** that fits each form factor: bottom bar on phone; side rail or sidebar on desktop. The app name and current home are always visible.
3. **Charm in small details**, not in decoration: for example initial avatars in palette colours, soft chips for roles and states, a gentle empty state (small icon + one friendly sentence), subtle hover and press feedback. At most one small illustration in the app (for example on sign-in).
4. **Dense data stays readable**: the checklist table and the workload summary must work on phone (for example cards or expandable rows) and on desktop (table).
5. **Accessibility**: WCAG AA contrast, touch targets of 48dp or more, visible focus states, and colour is never the only signal.
6. **Implementable in Compose Multiplatform with Material 3** (Android + web/wasm). Express tokens as Material 3 roles (primary, secondary, tertiary, surface containers, outline, outlineVariant) and shapes (extraSmall to extraLarge).

### Deliverables
- Design tokens: colour roles (light theme; dark theme optional), type scale, spacing scale, corner radii, elevation/borders.
- Components: top bar, navigation (phone + desktop), section card, list row with avatar, chip, primary/secondary/text buttons, text field, select, empty state, table row / task card, snackbar.
- Screens for phone and desktop: Sign in / Sign up, My homes (with and without invitations, with no homes), Homies (admin view and homie view), Checklist (empty and with about 15 tasks), Calendar (empty and with occurrences), Profile.
- A one-paragraph design rationale and a list of "do / don't" rules for future screens.

### Reference
Current screenshot: cream page, large coral illustration with a house and a smiling cloud, a row of 6 stickers (cat, plant, dog, rainbow, heart, socks), then "My homes" with one row and two inline forms. The colours are kept; the layout and stickers are replaced.
