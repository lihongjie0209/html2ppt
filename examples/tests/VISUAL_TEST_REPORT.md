# HTML2PPT Visual Test Report

## Overview
This document tracks all issues discovered during comprehensive visual testing,
along with their root causes and solutions.

## Test Files

| # | File | Slides | Scenario |
|---|------|--------|----------|
| 01 | 01-typography.html | 5 | Heading levels, inline formatting, font sizes, CJK text |
| 02 | 02-colors-backgrounds.html | 6 | Hex/RGB/named colors, slide backgrounds, text colors |
| 03 | 03-flexbox-layout.html | 6 | flex-direction, justify-content, align-items, flex-grow, gap |
| 04 | 04-cards-grids.html | 5 | Card patterns, metric dashboard, profile cards, bordered cards |
| 05 | 05-tables.html | 5 | Data tables, styled headers, CJK table content |
| 06 | 06-lists.html | 5 | Ordered/unordered lists, CJK lists, multi-column lists |
| 07 | 07-spacing-borders.html | 5 | Padding, margin, border widths/colors, border-radius |
| 08 | 08-text-alignment.html | 5 | text-align left/center/right, vertical-align, centered patterns |
| 09 | 09-nesting-depth.html | 5 | 3-level nesting, mixed direction, dashboard layout |
| 10 | 10-long-text.html | 5 | Long paragraphs, narrow columns, CJK wrapping |
| 11 | 11-css-cascade.html | 5 | Specificity, inheritance, class/inline overrides |
| 12 | 12-real-world.html | 5 | Dashboard, timeline, comparison, summary layouts |

**Total: 12 files, 62 slides**

---

## Issues Found & Fixed

### Issue 1: Default flex-direction incorrect (CRITICAL)

**Severity:** Critical — affected ~30% of slides  
**Affected slides:** 08-slide5, 09-slide5, 11-slide3, 12-slide2  
**Symptom:** Multiple `<p>` elements inside a `<div>` laid out horizontally instead of stacking vertically. Text overlapped and was truncated.

**Root cause:** `YogaLayoutEngine.applyStyle()` always defaulted to `flex-direction: row` (CSS flex spec default), but in normal HTML, `<div>` is a block element where children stack vertically. There was no way to distinguish between "user set `display: flex`" (should default to row) vs "user didn't set display" (should default to column/block flow).

**Fix:** Changed flex-direction default logic in `YogaLayoutEngine.java`:
```java
// Check if display:flex was explicitly set (vs default)
boolean explicitFlex = "flex".equals(style.get("display"));
String defaultDir = explicitFlex ? "row" : "column";
```
- If `display: flex` is explicitly set → default ROW (CSS flex spec)
- If `display` is NOT explicitly set → default COLUMN (normal block flow)

**Files changed:** `YogaLayoutEngine.java` lines 107-118

---

### Issue 2: Text measurement too narrow for bold text (MEDIUM)

**Severity:** Medium — caused text wrapping in headings and metrics  
**Affected slides:** 01-slide1, 04-slide2, 12-slide2  
**Symptom:** Short bold text at large font sizes ("Typography Test" at 44pt, "12.4K" at 28pt bold, "$84.2K" at 28pt bold) wrapped unexpectedly within their text boxes.

**Root cause:** Two issues combined:
1. `LATIN_WIDTH_RATIO` was 0.55, too narrow for actual rendering
2. No accounting for bold text being ~10% wider than regular text
3. No safety margin for per-character estimation errors

**Fix:** Three changes in `TextMeasurer.java`:
1. Increased `LATIN_WIDTH_RATIO` from 0.55 to 0.60
2. Added `BOLD_WIDTH_MULTIPLIER = 1.1` — bold text width is multiplied by 1.1
3. Added safety margin: `totalWidth + fontSize * 0.1` to prevent edge-case wrapping

Updated `YogaLayoutEngine.java` measure function to pass bold flag from `textBlock.style().isBold()`.

**Files changed:** `TextMeasurer.java`, `YogaLayoutEngine.java` line 62

---

### Issue 3: Border-only boxes invisible (MEDIUM)

**Severity:** Medium — boxes with border but no background fill didn't render  
**Affected slides:** 07-slide4 (4 border boxes)  
**Symptom:** Four boxes with different border widths/colors (1px black, 2px blue, 3px red, 4px green) were completely invisible.

**Root cause:** `PptRenderer.renderBox()` had early return: `if (box.children().isEmpty() && box.style().getBackgroundColor() == null) return;` — this skipped boxes that had borders but no background fill. Additionally, the shape creation was gated on `bgColor != null`, so border-only boxes never got a shape created.

**Fix:** Changed the decision logic in `PptRenderer.renderBox()`:
1. Check for border properties in addition to background color
2. Create shape if `hasBorder || hasBackground`
3. Set `fillColor(null)` (transparent) for border-only boxes instead of skipping

**Files changed:** `PptRenderer.java` lines 112-150

---

### Issue 4: Tables don't span full width (MEDIUM)

**Severity:** Medium — table text wrapping due to narrow columns  
**Affected slides:** 05-slide2, 05-slide3  
**Symptom:** "Management" wrapped to "Managemen\nt", "Backend Dev" wrapped to "Backend\nDev" due to narrow table columns.

**Root cause:** Two issues:
1. `TableBlock` was a leaf node in Yoga without a measure function or explicit dimensions, so Yoga gave it minimal size
2. POI's `XSLFTable` didn't have column widths explicitly set

**Fix:**
1. In `YogaLayoutEngine.createYogaNode()`: Added table-specific handling — set `widthPercent(100)` and estimate height from row count
2. In `PptRenderer.renderTable()`: Added explicit column width distribution — `table.setColumnWidth(c, tableWidth / numCols)` for each column

**Files changed:** `YogaLayoutEngine.java` lines 80-84, `PptRenderer.java` lines 250-255

---

### Issue 5: Text overflow in constrained flex containers (CRITICAL)

**Severity:** Critical — text overflowed parent box boundaries  
**Affected slides:** 03-slide3, 03-slide4, and any slide with `flex: 1` containers  
**Symptom:** Text letters (A, B, C) appeared above/outside the blue item boxes. The boxes were squashed to very small heights (18px with 16px padding = 2px content area) while text needed 20px.

**Root cause:** When containers use `flex: 1` (which expands to `flex-grow:1; flex-shrink:1; flex-basis:0`), Yoga distributes available space. With limited vertical space and many items competing, boxes shrink below their content's minimum size. Text blocks had no minimum height constraint, so Yoga would report heights as small as 3px for 14pt text.

**Fix:** Two changes in `YogaLayoutEngine.createYogaNode()`:
1. **Text blocks**: Set `yogaNode.setMinHeight(fontSize * 1.4)` to enforce minimum line height
2. **Container boxes with text children**: Calculate minimum height from text children's font size + padding, set via `yogaNode.setMinHeight()`

```java
// For text blocks
float minLineHeight = (float) (fontSize * 1.4);
yogaNode.setMinHeight(minLineHeight);

// For containers with text children
if (child instanceof SlideNode.TextBlock textChild) {
    double fontSize = textChild.style().getFontSize(18);
    minContentHeight = Math.max(minContentHeight, (float) (fontSize * 1.4));
}
yogaNode.setMinHeight(minContentHeight + totalPadding);
```

**Files changed:** `YogaLayoutEngine.java` lines 58-85, 92-115

---

## Test Results After Fixes

### Verified Rendering Capabilities (all ✅)

| Category | Details |
|----------|---------|
| **Typography** | h1-h6 headings, bold, italic, underline, combinations |
| **Font sizes** | 12pt through 44pt, scaling correctly |
| **CJK text** | Chinese, Japanese, Korean characters render correctly |
| **Mixed text** | CJK + Latin mixed content in same text block |
| **Colors** | Hex (#RRGGBB), short hex (#RGB), named colors, rgb() |
| **Backgrounds** | Slide backgrounds, box backgrounds, dark themes |
| **Flex direction** | row, column (explicit and default block flow) |
| **Justify content** | flex-start, center, flex-end, space-between |
| **Align items** | flex-start, center, flex-end |
| **Flex grow** | Equal (1:1:1), proportional (1:2:1, 1:3:1) |
| **Gap & padding** | 5px, 10px, 20px, 30px variations |
| **Cards** | Metric cards, profile cards, feature grids |
| **Tables** | Full-width, styled headers, CJK content |
| **Lists** | Ordered (numbered), unordered (bulleted) |
| **Borders** | Border-only (no fill), border + background, border-radius |
| **Border radius** | 0px, 4px, 8px, 20px, 50px progression |
| **Text alignment** | left, center, right |
| **Nesting** | 3 levels deep, mixed direction nesting |
| **Long text** | English and CJK paragraph wrapping |
| **CSS cascade** | Specificity, inheritance, inline overrides |
| **Real-world layouts** | Dashboard, timeline, comparison, summary + footer |

### All 62 slides render correctly across 12 test files.

---

## Test Infrastructure

### Compilation
```bash
.\gradlew.bat run --args="compile <input.html> -o <output.pptx>"
```

### Rendering (requires PowerPoint installed)
```bash
python tools\pptx2png.py <input.pptx> <output_dir>
```

### Unit Tests
```bash
.\gradlew.bat test    # 180 tests, JaCoCo ≥85% coverage
```
