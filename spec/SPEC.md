# HTML2PPT Specification v1.1

> **Spec-First Rule**: Any changes to element mapping, CSS support, rendering behavior,
> or CLI interface MUST be documented in this spec BEFORE code is modified.

## Overview

HTML2PPT compiles standard **HTML + CSS** files into PowerPoint presentations (.pptx).
Users write slides using familiar web technologies — `<section>` elements define slides,
CSS flexbox handles layout, and standard HTML elements map to PPT shapes.

## Tech Stack

- **HTML Parser**: Jsoup 1.21.1
- **CSS Parser**: ph-css 7.0.4 (cascade, specificity, `<style>` blocks)
- **Layout Engine**: Facebook Yoga (pure Java) — `org.appliedenergistics.yoga:yoga:1.0.0`
- **PPT Engine**: Apache POI XSLF 5.3.0
- **CLI**: picocli 4.7.6
- **Testing**: JUnit 5 + AssertJ + JaCoCo (>85% line coverage on core)

## Implementation Status (2026-03-21)

The current implementation is production-usable for HTML-first slide generation and has:

- **24 visual test files**
- **115+ test slides**
- **270+ unit tests**
- **Passing `jacocoTestCoverageVerification` for core code**

Implemented capabilities include:

- Document structure, metadata, and CLI compile flow
- Flexbox layout, padding, margin, sizing, and absolute positioning
- Typography, inline formatting, hyperlinks, and text auto-height
- Lists, custom bullets, tables, colspan/rowspan
- Images, opacity, gradients, box shadows, code blocks
- Speaker notes, debug layout dumps, CSS shapes, object-fit
- Built-in master slide layouts

Planned but not yet implemented:

- Charts
- SVG support
- PowerPoint animations

## Release Automation

Repository publishing uses GitHub Actions for release builds.

Rules:

1. Source code is pushed to GitHub normally; release artifacts are not hand-built and uploaded as the canonical release output.
2. Pushing a version tag matching `v*` MUST trigger a GitHub Actions workflow that runs the Gradle quality gate and builds the distributable JAR.
3. The workflow MUST publish the generated `build/libs/*.jar` file as the GitHub Release asset for that tag.
4. The release pipeline SHOULD be repeatable from CI so the published artifact is reproducible from repository state.

## Reference Examples

The following visual tests are useful as executable examples of the spec:

| Capability | Example file | Notes |
|------------|--------------|-------|
| Typography and text styles | `examples/tests/01-typography.html` | headings, inline formatting, CJK |
| Flexbox layout | `examples/tests/03-flexbox-layout.html` | rows, columns, alignment, gaps |
| Tables | `examples/tests/05-tables.html` | styled tables |
| Lists and bullets | `examples/tests/06-lists.html`, `21-custom-bullets.html` | ordered/unordered/custom bullets |
| Images and object-fit | `examples/tests/13-images.html`, `23-object-fit.html` | sizing, contain, cover |
| Links | `examples/tests/14-links.html` | clickable `<a href>` |
| Opacity and shadows | `examples/tests/15-opacity.html`, `17-shadows.html` | alpha and box shadows |
| Code blocks | `examples/tests/16-code-blocks.html` | `<pre><code>` styling |
| Gradients | `examples/tests/18-gradients.html` | slide and shape gradients |
| Speaker notes | `examples/tests/20-speaker-notes.html` | `<aside class="notes">` |
| Master slides | `examples/tests/24-master-slides.html` | `data-layout` placeholder mapping |

## Document Structure

```html
<!DOCTYPE html>
<html>
<head>
  <style>
    /* CSS rules applied to all slides */
  </style>
</head>
<body>
  <section><!-- Slide 1 --></section>
  <section><!-- Slide 2 --></section>
</body>
</html>
```

### Rules

1. Each `<section>` direct child of `<body>` becomes one slide.
2. The `<style>` block in `<head>` provides shared CSS rules.
3. Inline `style="..."` attributes override `<style>` rules (standard CSS cascade).
4. The `<html>` element can have `data-layout="16x9"` (default) or `data-layout="4x3"`.
5. The `<html>` element can have `data-title`, `data-author`, `data-subject` for metadata.
6. A `<section>` may optionally set `data-layout` to use a built-in PowerPoint master layout.

## Slide Dimensions

| Layout | Width (pt) | Height (pt) |
|--------|-----------|-------------|
| 16:9   | 720       | 405         |
| 4:3    | 720       | 540         |

Each slide is a flex container by default. The root Yoga node for each slide has
`width` and `height` set to the slide dimensions, with `display: flex` and
`flex-direction: column` as defaults.

## Built-in Master Slide Layouts

When a slide uses `<section data-layout="...">`, HTML2PPT creates the slide from the
default PowerPoint master instead of a blank slide and maps top-level text blocks into
the layout's placeholders.

| `data-layout` value | PowerPoint layout | Placeholder mapping |
|---------------------|-------------------|---------------------|
| `title` | `TITLE` | First large heading → title, next heading/text → subtitle |
| `title-content`, `title-body` | `TITLE_AND_CONTENT` | First large heading → title, remaining top-level text blocks → content placeholder |
| `section`, `section-header` | `SECTION_HEADER` | First large heading → title, remaining top-level text blocks → body placeholder |
| `title-only` | `TITLE_ONLY` | First large heading → title |
| `blank` | `BLANK` | No placeholder filling; children render with normal HTML layout |
| `two-content`, `two-column` | `TWO_OBJ` | First large heading → title, remaining text flows into available body/content placeholders |
| `picture-caption` | `PIC_TX` | First large heading → title, remaining text flows into body/content placeholders |

Rules:

1. Placeholder filling only considers top-level `TextBlock` children of the slide.
2. Placeholder text is explicitly cleared when no matching HTML content is provided, so
   default master prompt text must never leak into output.
3. `SECTION_HEADER` does **not** have a subtitle placeholder in the default PowerPoint
   master; subtitle-like HTML content (for example an `<h2>`) maps into the body
   placeholder instead.
4. If a requested master layout is unavailable, HTML2PPT falls back to a blank slide.

## HTML Element → PPT Mapping

### Container Elements

| HTML Element | PPT Shape | Default Styles | Notes |
|-------------|-----------|----------------|-------|
| `<section>` | `XSLFSlide` | `display:flex; flex-direction:column` | Each = one slide |
| `<div>` | `XSLFGroupShape` | `display:flex` | Generic container |

### Text Elements

| HTML Element | PPT Shape | Default font-size | Default font-weight | Notes |
|-------------|-----------|-------------------|---------------------|-------|
| `<h1>` | `XSLFTextBox` | 36pt | bold | Heading level 1 |
| `<h2>` | `XSLFTextBox` | 28pt | bold | Heading level 2 |
| `<h3>` | `XSLFTextBox` | 24pt | bold | Heading level 3 |
| `<h4>` | `XSLFTextBox` | 20pt | bold | Heading level 4 |
| `<h5>` | `XSLFTextBox` | 18pt | bold | Heading level 5 |
| `<h6>` | `XSLFTextBox` | 16pt | bold | Heading level 6 |
| `<p>` | `XSLFTextBox` | 18pt | normal | Paragraph |
| `<span>` | Text run | inherit | inherit | Inline text styling |

### Inline Formatting Elements

These elements modify text runs within a text box. They do not create separate PPT shapes.

| HTML Element | Effect |
|-------------|--------|
| `<b>`, `<strong>` | Bold text run |
| `<i>`, `<em>` | Italic text run |
| `<u>` | Underline text run |
| `<a href>` | Hyperlink text run (underlined, link color default if unspecified) |
| `<br>` | Line break within paragraph |

### List & Special Elements

| HTML Element | PPT Shape | Notes |
|-------------|-----------|-------|
| `<ul>`, `<ol>` | `XSLFGroupShape` | Contains vertically stacked list item text blocks |
| `<li>` | `XSLFTextBox` | Rendered with bullet/number prefix |
| `<pre><code>` | `XSLFTextBox` | Monospace block with code defaults |
| `<code>` | Text run / `XSLFTextBox` | Inline code at block level becomes styled text |
| `<aside class="notes">` | Speaker notes | Extracted from slide body, not rendered as visible content |

### Media Elements

| HTML Element | PPT Shape | Required Attributes | Notes |
|-------------|-----------|-------------------|-------|
| `<img>` | `XSLFPictureShape` | `src` (file path) | Supports PNG, JPG, GIF, SVG; honors `object-fit` |

### Table Elements

| HTML Element | PPT Shape | Notes |
|-------------|-----------|-------|
| `<table>` | `XSLFTable` | Container |
| `<tr>` | Table row | |
| `<td>`, `<th>` | `XSLFTableCell` | `<th>` defaults to bold |

### Shape Elements

| HTML Element | PPT Shape | Notes |
|-------------|-----------|-------|
| `<hr>` | `XSLFAutoShape` (line) | Horizontal rule |

## CSS Property Support

### Layout Properties (via Yoga)

#### Container (Flex Parent)

| CSS Property | Yoga Mapping | Supported Values | Default |
|-------------|-------------|-----------------|---------|
| `display` | YogaDisplay | `flex`, `none` | `flex` |
| `flex-direction` | setFlexDirection | `row`, `column`, `row-reverse`, `column-reverse` | `row` |
| `justify-content` | setJustifyContent | `flex-start`, `center`, `flex-end`, `space-between`, `space-around`, `space-evenly` | `flex-start` |
| `align-items` | setAlignItems | `flex-start`, `center`, `flex-end`, `stretch` | `stretch` |
| `align-content` | setAlignContent | `flex-start`, `center`, `flex-end`, `stretch`, `space-between`, `space-around` | `stretch` |
| `flex-wrap` | setFlexWrap | `wrap`, `nowrap` | `nowrap` |
| `gap` | setGap(Gutter.ALL) | `<length>` | `0` |
| `row-gap` | setGap(Gutter.ROW) | `<length>` | — |
| `column-gap` | setGap(Gutter.COLUMN) | `<length>` | — |

#### Item (Flex Child)

| CSS Property | Yoga Mapping | Supported Values | Default |
|-------------|-------------|-----------------|---------|
| `flex-grow` | setFlexGrow | `<number>` | `0` |
| `flex-shrink` | setFlexShrink | `<number>` | `1` |
| `flex-basis` | setFlexBasis | `auto`, `<length>` | `auto` |
| `flex` | shorthand | `<grow> [<shrink>] [<basis>]` | — |
| `align-self` | setAlignSelf | `auto`, `flex-start`, `center`, `flex-end`, `stretch` | `auto` |

#### Spacing

| CSS Property | Yoga Mapping | Supported Values |
|-------------|-------------|-----------------|
| `padding` | setPadding(Edge.ALL) | `<length>` (shorthand: 1-4 values) |
| `padding-top` | setPadding(Edge.TOP) | `<length>` |
| `padding-right` | setPadding(Edge.RIGHT) | `<length>` |
| `padding-bottom` | setPadding(Edge.BOTTOM) | `<length>` |
| `padding-left` | setPadding(Edge.LEFT) | `<length>` |
| `margin` | setMargin(Edge.ALL) | `<length>`, `auto` (shorthand: 1-4 values) |
| `margin-top` | setMargin(Edge.TOP) | `<length>`, `auto` |
| `margin-right` | setMargin(Edge.RIGHT) | `<length>`, `auto` |
| `margin-bottom` | setMargin(Edge.BOTTOM) | `<length>`, `auto` |
| `margin-left` | setMargin(Edge.LEFT) | `<length>`, `auto` |

#### Sizing

| CSS Property | Yoga Mapping | Supported Values |
|-------------|-------------|-----------------|
| `width` | setWidth | `auto`, `<length>`, `<percentage>` |
| `height` | setHeight | `auto`, `<length>`, `<percentage>` |
| `min-width` | setMinWidth | `<length>` |
| `min-height` | setMinHeight | `<length>` |
| `max-width` | setMaxWidth | `<length>` |
| `max-height` | setMaxHeight | `<length>` |

#### Positioning

| CSS Property | Yoga Mapping | Supported Values |
|-------------|-------------|-----------------|
| `position` | setPositionType | `relative` (default), `absolute` |
| `top` | setPosition(Edge.TOP) | `<length>` |
| `right` | setPosition(Edge.RIGHT) | `<length>` |
| `bottom` | setPosition(Edge.BOTTOM) | `<length>` |
| `left` | setPosition(Edge.LEFT) | `<length>` |

### Visual Properties (mapped to POI)

#### Colors & Background

| CSS Property | POI Mapping | Supported Values |
|-------------|-------------|-----------------|
| `background-color` | setFillColor | hex (`#RGB`, `#RRGGBB`), `rgb()`, named colors |
| `background` | solid fill / gradient fill | solid colors or `linear-gradient(...)` |
| `color` | setFontColor | hex, `rgb()`, named colors |
| `opacity` | setAlpha | `0` - `1` |
| `box-shadow` | outer shadow effect | offset, blur, color, alpha |
| `object-fit` | image placement/clipping | `fill`, `contain`, `cover`, `none`, `scale-down` |
| `object-position` | reserved | parsed for future use; not yet fully rendered |

#### Typography

| CSS Property | POI Mapping | Supported Values |
|-------------|-------------|-----------------|
| `font-size` | setFontSize | `<length>` (px, pt) |
| `font-weight` | setBold | `bold`, `normal`, `700`, `400` |
| `font-style` | setItalic | `italic`, `normal` |
| `text-decoration` | setUnderlined | `underline`, `none` |
| `text-align` | setTextAlign | `left`, `center`, `right` |
| `vertical-align` | setVerticalAlignment | `top`, `middle`, `bottom` |
| `font-family` | setFontFamily | font name string |
| `line-height` | setLineSpacing | number or `<length>` |

#### Border & Shape

| CSS Property | POI Mapping | Supported Values |
|-------------|-------------|-----------------|
| `border` | setLineColor + setLineWidth | shorthand: `<width> <style> <color>` |
| `border-color` | setLineColor | color value |
| `border-width` | setLineWidth | `<length>` |
| `border-radius` | Shape type → roundRect | `<length>` (>0 switches to rounded rect) |

### Unit Support

| Unit | Handling | Notes |
|------|---------|-------|
| `px` | 1px = 1pt | Direct mapping |
| `pt` | Direct | Native PPT unit |
| `%` | Percentage of parent | Yoga supports this |
| `em` | Multiply by current font-size | |
| `rem` | Multiply by root font-size (18pt default) | |
| Unitless | Context-dependent | `line-height: 1.5` = 150% |

### Color Format Support

| Format | Example | Notes |
|--------|---------|-------|
| 6-char hex | `#FF0000` | → `FF0000` for POI |
| 3-char hex | `#F00` | → `FF0000` |
| `rgb()` | `rgb(255, 0, 0)` | → `FF0000` |
| Named colors | `red`, `blue`, `white` | W3C named colors |

## CSS Cascade & Specificity

Style resolution follows standard CSS rules:

1. **Parse**: Extract all `<style>` blocks from `<head>`.
2. **Match**: For each element, find all matching CSS rules.
3. **Specificity**: Sort by CSS specificity (inline > #id > .class > element).
4. **Cascade**: Later rules override earlier rules at same specificity.
5. **Inline**: `style="..."` attributes have highest priority.
6. **Inheritance**: `color`, `font-size`, `font-weight`, `font-style`, `font-family`,
   `text-align`, `line-height`, `text-decoration` inherit from parent.

### Supported Selectors

| Selector Type | Example | Notes |
|--------------|---------|-------|
| Element | `p`, `div`, `h1` | |
| Class | `.card`, `.title` | |
| ID | `#header` | |
| Descendant | `section .card p` | |
| Child | `section > div` | |
| Multiple | `h1, h2, h3` | |
| Combined | `div.card` | Element + class |

## Text Auto-Height

When a text element (`<p>`, `<h1>`-`<h6>`) does not have an explicit `height`, Yoga
computes its height via a measure function:

1. **Estimate character width**: `fontSize * 0.6` (average for CJK/Latin mix)
2. **Calculate text width**: `charCount * charWidth`
3. **Calculate lines**: `ceil(textWidth / availableWidth)` (min 1)
4. **Calculate height**: `lines * fontSize * 1.4` (line-height default)

This ensures text boxes grow to fit their content.

## Compilation Pipeline

```
input.html
    │
    ▼
HtmlParser (Jsoup) → Document
    │
    ├─► CssParser (ph-css) → List<CssRule>
    │
    ▼
StyleResolver → ComputedStyle per element
    │
    ▼
DomConverter → List<SlideNode> (one per <section>)
    │
    ▼
YogaLayoutEngine → Computed LayoutResult (x, y, w, h) per node
    │
    ▼
PptRenderer (Apache POI) → .pptx
```

## CLI Interface

```
html2ppt compile <input.html> [-o output.pptx] [--debug]
```

- Default output: same name as input with `.pptx` extension
- `--debug`: writes a `.layout.txt` file alongside the `.pptx` with the computed layout tree (node types, coordinates, CSS properties)
- Visual render helper: `gradlew renderSlides --args="<input.pptx> <outputDir>"`
- Example: `html2ppt compile presentation.html` → `presentation.pptx`
- Example: `html2ppt compile presentation.html --debug` → `presentation.pptx` + `presentation.layout.txt`

## Verification Strategy

Changes are validated in three layers:

1. **Unit tests** for parser, layout engine, renderer, CLI, and utilities
2. **Visual tests** in `examples/tests/` that compile representative HTML to PPTX
3. **Image export** of generated PPTX files for manual visual inspection when needed

The project currently enforces a JaCoCo coverage gate for core code via:

```bash
./gradlew test jacocoTestCoverageVerification
```

## Error Handling

- Missing `<section>` → warning: "No slides found"
- Invalid CSS property value → ignore (skip property, log warning)
- Missing image `src` → throw with descriptive message
- Unsupported HTML element → treat as `<div>` container (log warning)
- Empty `<section>` → create blank slide

## Internal Model

```java
sealed interface SlideNode {
    ComputedStyle style();
    List<SlideNode> children();
    LayoutResult layout();
}

// Implementations:
// Slide    — one slide, children are top-level elements
// Box      — <div>, generic flex container → XSLFGroupShape
// TextBlock — <p>, <h1>-<h6>, contains TextRun list → XSLFTextBox
// ImageBlock — <img> → XSLFPictureShape
// TableBlock — <table> → XSLFTable
// ShapeBlock — <hr>, shapes → XSLFAutoShape
```
