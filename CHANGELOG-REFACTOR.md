# Changelog - App Refactor (Tag: app-refactor)

## 🎨 Major UI/UX Redesign

### Color Scheme
**Before:** Green theme (#00A651 - TD Bank inspired)  
**After:** Dark Blue theme (#1A365D - Professional navy)

All UI elements updated:
- Navigation bar: Dark blue (#1A365D)
- Buttons: Dark blue instead of green
- Links and accents: Navy blue (#003366)
- Hover states: Updated to match new theme

### Branding Changes
- **"EasyWeb"** → **"Crazy Web"** (all instances)
- **"Borod Bank"** - kept unchanged
- Logo "BB" updated with new color scheme

### Layout Restructuring (Anti-Phishing Measures)
**Previous Layout:**
- Side-by-side design (login form left, info panel right)
- 1:1 clone of TD Bank EasyWeb

**New Layout:**
- Centered login card (max-width: 600px)
- Info panels moved below form in horizontal flex-row
- Three info cards displayed side-by-side
- Distinctly different from original TD Bank design

### Disclaimer Banner
Added prominent warning banner on all pages:
- Yellow background (#fef3c7) with orange border
- Text: "DEMO ENVIRONMENT: This is a mock application for open-source pipeline demonstration purposes only. Do not enter real credentials."
- Present on: Login, Register, Dashboard, Transactions

### Generic Terminology
Replaced banking-specific terms with developer-friendly language:

| Original | New |
|----------|-----|
| "128-bit Multi-factor Security Guarantee" | "Standard Demo Authentication" |
| "How we protect you" | "Mock Security Layer" |
| "EasyWeb Login" | "Crazy Web Login" |
| "Open Your Account" | "Open Your Demo Account" |
| "Register online now" | "Register Demo Account" |

## 🔧 Technical Changes

### Backend
- **SecurityConfig.java**: Added `/css/**`, `/js/**`, `/images/**` to permitAll() for static resources
- **Transaction.java**: Renamed field `type` → `description` for consistency
- **AccountServiceIT.java**: Updated tests to use `getDescription()` instead of `getType()`

### Configuration
- **application-dev.properties**: Hot reload configuration for rapid development
- **application-local.properties**: H2 in-memory database for local testing
- **start-dev.sh**: Quick startup script with port conflict detection

### Frontend
- **app.css**: Complete redesign (893 lines)
  - Dark blue color variables
  - Centered login layout styles
  - Horizontal info card grid
  - Disclaimer banner styles
  - Responsive design improvements

- **login.html**: Restructured layout with centered form and info cards below
- **register.html**: Updated to match new design
- **dashboard.html**: Added disclaimer banner
- **transactions.html**: Added disclaimer banner

## 📊 Statistics

```
12 files changed, 1377 insertions(+), 523 deletions(-)
```

**New Files:**
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-local.properties`
- `src/main/resources/static/css/app.css`
- `start-dev.sh`

**Modified Files:**
- `pom.xml` (dependency updates)
- `SecurityConfig.java` (static resources fix)
- `Transaction.java` (field rename)
- `login.html`, `register.html`, `dashboard.html`, `transactions.html` (redesign)
- `AccountServiceIT.java` (test updates)

## 🎯 Anti-Phishing Features

1. ✅ Different color scheme (blue vs green)
2. ✅ Different layout structure (centered vs side-by-side)
3. ✅ Prominent disclaimer warnings
4. ✅ Generic terminology instead of banking-specific
5. ✅ Clear "demo environment" messaging

## 🚀 Development Improvements

### Quick Start
```bash
cd borod-bank-ci
./start-dev.sh
```

### Hot Reload
- DevTools enabled for automatic restart
- CSS/HTML changes apply in 2-3 seconds
- No need to rebuild for frontend changes

### Local Testing
- H2 in-memory database (no MySQL required)
- Profile: `local,dev`
- Port: 8080 (or 8081 if 8080 is busy)

## 📝 Commit Details

**Commit:** `6b7c018`
**Tag:** `v3.5-ui-redesign`
**Branch:** `main`
**Date:** 2026-02-20

## 🔗 Links

- **Repository:** https://github.com/Synaptix-Innovations/borod-bank-ci
- **Tag:** https://github.com/Synaptix-Innovations/borod-bank-ci/releases/tag/v3.5-ui-redesign
- **Commit:** https://github.com/Synaptix-Innovations/borod-bank-ci/commit/6b7c018

## 📋 Testing Checklist

- [x] Login page displays with dark blue theme
- [x] Disclaimer banner visible on all pages
- [x] "Crazy Web" branding throughout
- [x] Centered login layout (not side-by-side)
- [x] Info cards display horizontally below form
- [x] CSS loads correctly (static resources fix)
- [x] Hot reload works for development
- [x] Application starts with start-dev.sh
- [x] All tests pass
- [x] Backend functionality unchanged

## 🎨 Screenshots

To view the new design:
1. Start application: `./start-dev.sh`
2. Open: http://localhost:8080/login
3. Use hard refresh: Ctrl+Shift+R

---

**This refactor maintains all backend functionality while providing a distinct UI that clearly indicates this is a demonstration application, not a production banking system.**
