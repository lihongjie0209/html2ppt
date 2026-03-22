# HTML2PPT

Compile **HTML + CSS** into native PowerPoint presentations.

HTML2PPT lets you define slides with standard web syntax, uses a Yoga-based flexbox layout engine to compute positions, and emits `.pptx` via Apache POI. The project follows a strict **spec-first** workflow: update `spec/SPEC.md` before changing behavior.

## Current Status

As of `2026-03-21`, the project includes:

- `24` visual test files
- `115+` visual test slides
- `270+` unit tests
- passing `./gradlew test jacocoTestCoverageVerification`

Implemented feature areas:

- document structure and metadata
- flexbox layout, spacing, sizing, absolute positioning
- text, inline formatting, hyperlinks, code blocks
- lists, custom bullets, tables, colspan/rowspan
- images, opacity, gradients, shadows, shapes, object-fit
- speaker notes, debug layout dumps, master slide layouts

Planned next:

- charts
- SVG support
- PowerPoint animations

## Spec-First Workflow

The authoritative behavior document is `spec/SPEC.md`.

Use this workflow for every change:

1. update `spec/SPEC.md`
2. implement code changes
3. add or update unit tests
4. verify with visual tests when rendering behavior changes

## Quick Start

Build:

```bash
./gradlew build
```

Compile HTML to PPTX:

```bash
./gradlew run --args="compile slides.html -o presentation.pptx"
```

Generate a debug layout dump:

```bash
./gradlew run --args="compile slides.html -o presentation.pptx --debug"
```

Render PPTX slides to PNG for visual inspection:

```bash
./gradlew renderSlides --args="presentation.pptx output/"
```

Run the quality gate:

```bash
./gradlew test jacocoTestCoverageVerification
```

Release from GitHub CI:

```bash
git push origin main
git tag v1.0.0
git push origin v1.0.0
```

Tags matching `v*` trigger the GitHub Actions release workflow, which runs `./gradlew test jar` and publishes the generated `build/libs/*.jar` as the GitHub Release asset.

## Specification Guide

The project has one main spec file, but it covers multiple feature areas. This table is the fastest way to navigate it.

| Feature area | Spec coverage | Example | Visual test |
|--------------|---------------|---------|-------------|
| Document structure and metadata | `Document Structure`, `Slide Dimensions`, `CLI Interface` | `<html data-layout="16x9" data-title="Deck">` | `01-typography.html` |
| Flexbox layout | `CSS Property Support → Layout Properties` | `display:flex`, `gap`, `justify-content` | `03-flexbox-layout.html` |
| Text and inline formatting | `HTML Element → PPT Mapping`, `Text Auto-Height` | `<h1>`, `<span>`, `<a href>` | `01-typography.html`, `14-links.html` |
| Lists and bullets | `List & Special Elements` | `<ul>`, `<ol>`, `list-style-type` | `06-lists.html`, `21-custom-bullets.html` |
| Tables | `Table Elements` | `<table>`, `colspan`, `rowspan` | `05-tables.html`, `19-table-colspan.html` |
| Images and object-fit | `Media Elements`, `Colors & Background` | `<img>`, `object-fit: cover` | `13-images.html`, `23-object-fit.html` |
| Effects and shapes | `Colors & Background`, `Border & Shape` | gradients, shadows, opacity, radius | `15-opacity.html`, `17-shadows.html`, `18-gradients.html`, `22-css-shapes.html` |
| Speaker notes and debug | `List & Special Elements`, `CLI Interface`, `Verification Strategy` | `<aside class="notes">`, `--debug` | `20-speaker-notes.html` |
| Master slides | `Built-in Master Slide Layouts` | `<section data-layout="title-content">` | `24-master-slides.html` |

## Core Authoring Model

Each top-level `<section>` under `<body>` becomes one slide.

Minimal example:

```html
<!DOCTYPE html>
<html data-layout="16x9" data-title="Roadmap" data-author="Team">
<head>
  <style>
    section { padding: 40px; }
    h1 { font-size: 36pt; color: #003366; }
  </style>
</head>
<body>
  <section>
    <h1>Quarterly Roadmap</h1>
    <p>HTML2PPT compiles this into a native slide.</p>
  </section>
</body>
</html>
```

## Examples by Spec Area

### 1. Flexbox layout

HTML2PPT uses Yoga to implement slide layout with familiar CSS flexbox semantics.

```html
<section style="display:flex; flex-direction:row; gap:20px; padding:40px;">
  <div style="flex:1; background:#f5f7fb; padding:20px;">
    <h2>Left</h2>
    <p>Flexible card</p>
  </div>
  <div style="flex:2; background:#eef2ff; padding:20px;">
    <h2>Right</h2>
    <p>Gets twice as much width</p>
  </div>
</section>
```

See: `spec/SPEC.md` → `CSS Property Support → Layout Properties`

Related test: `examples/tests/03-flexbox-layout.html`

### 2. Text, links, and inline formatting

Inline formatting is preserved inside text boxes.

```html
<section style="padding:40px;">
  <h1>Formatted text</h1>
  <p>
    Use <strong>bold</strong>, <em>italic</em>, <u>underline</u>,
    and <a href="https://example.com">hyperlinks</a>.
  </p>
</section>
```

See: `spec/SPEC.md` → `HTML Element → PPT Mapping`

Related tests: `examples/tests/01-typography.html`, `examples/tests/14-links.html`

### 3. Lists and custom bullets

Lists are rendered as vertically stacked text blocks, with support for custom bullet styles.

```html
<section style="padding:40px;">
  <h1>Release checklist</h1>
  <ul style="list-style-type: check; color:#14532d;">
    <li>Unit tests passing</li>
    <li>Visual tests reviewed</li>
    <li>README updated</li>
  </ul>
</section>
```

See: `spec/SPEC.md` → `List & Special Elements`

Related tests: `examples/tests/06-lists.html`, `examples/tests/21-custom-bullets.html`

### 4. Tables

Tables become native PowerPoint tables, including `colspan` and `rowspan`.

```html
<section style="padding:30px;">
  <table style="width:100%;">
    <tr>
      <th colspan="2">Quarterly Summary</th>
    </tr>
    <tr>
      <td>Revenue</td>
      <td>$1.2M</td>
    </tr>
  </table>
</section>
```

See: `spec/SPEC.md` → `Table Elements`

Related tests: `examples/tests/05-tables.html`, `examples/tests/19-table-colspan.html`

### 5. Images and object-fit

Images use native PPT picture shapes and support `object-fit`.

```html
<section style="padding:40px;">
  <h1>Product image</h1>
  <img
    src="images/product.jpg"
    style="width:320px; height:180px; object-fit:cover; border-radius:12px;"
  />
</section>
```

See: `spec/SPEC.md` → `Media Elements`, `Colors & Background`

Related tests: `examples/tests/13-images.html`, `examples/tests/23-object-fit.html`

### 6. Effects: gradients, shadows, opacity, shapes, code blocks

Visual effects are supported directly in HTML/CSS authoring.

```html
<section style="padding:40px; background:linear-gradient(to right, #0f172a, #1e3a8a);">
  <div style="background:rgba(255,255,255,0.12); box-shadow:0 8px 24px rgba(0,0,0,0.25); padding:24px; border-radius:16px;">
    <pre><code>npm run build
npm run test</code></pre>
  </div>
</section>
```

See: `spec/SPEC.md` → `Colors & Background`, `Border & Shape`

Related tests: `examples/tests/15-opacity.html`, `17-shadows.html`, `18-gradients.html`, `16-code-blocks.html`, `22-css-shapes.html`

### 7. Speaker notes and debug output

Speaker notes are declared in HTML and emitted into PowerPoint notes pages. Debug mode writes a layout tree dump.

```html
<section style="padding:40px;">
  <h1>Presenter view</h1>
  <p>Visible content on the slide.</p>
  <aside class="notes">
    Mention the rollout date and key risks here.
  </aside>
</section>
```

Compile with debug:

```bash
./gradlew run --args="compile slides.html -o slides.pptx --debug"
```

See: `spec/SPEC.md` → `List & Special Elements`, `CLI Interface`, `Verification Strategy`

Related tests: `examples/tests/20-speaker-notes.html`

### 8. Master slides

Slides can opt into built-in PowerPoint master layouts using `data-layout`.

```html
<section data-layout="title-content">
  <h1>Key Features</h1>
  <p>HTML + CSS syntax</p>
  <p>Flexbox layout</p>
  <p>Master slide templates</p>
</section>

<section data-layout="section">
  <h1>Getting Started</h1>
  <h2>Installation and Basic Usage</h2>
</section>
```

See: `spec/SPEC.md` → `Built-in Master Slide Layouts`

Related test: `examples/tests/24-master-slides.html`

## CLI Reference

Basic compile:

```bash
./gradlew run --args="compile input.html"
```

Compile to a specific output:

```bash
./gradlew run --args="compile input.html -o output.pptx"
```

Compile with debug layout dump:

```bash
./gradlew run --args="compile input.html -o output.pptx --debug"
```

Render a generated PPTX to PNG:

```bash
./gradlew renderSlides --args="output.pptx output/"
```

## Testing and Verification

Unit and coverage:

```bash
./gradlew test jacocoTestCoverageVerification
```

Visual test workflow:

```bash
./gradlew run --args="compile examples/tests/24-master-slides.html -o examples/tests/output/24-master-slides.pptx"
python examples/tests/scripts/ppt_to_images.py examples/tests/output/24-master-slides.pptx examples/tests/output
```

The `examples/tests/` directory is the best place to see the current feature surface in action.

## GitHub Release Workflow

Releases are built in GitHub Actions, not by manually uploading local artifacts.

Workflow summary:

- push code to `main`
- push a version tag such as `v1.0.0`
- GitHub Actions runs `./gradlew test jar`
- the workflow publishes `build/libs/*.jar` to the GitHub Release for that tag

## Project Structure

```text
html2ppt/
├── src/main/java/com/html2ppt/
│   ├── cli/       # CLI entrypoint
│   ├── css/       # CSS parsing and cascade
│   ├── layout/    # Yoga layout engine + debug dump
│   ├── model/     # SlideNode / ComputedStyle model
│   ├── parser/    # HTML → SlideNode conversion
│   ├── renderer/  # PPTX generation
│   └── tools/     # auxiliary rendering tools
├── spec/SPEC.md   # authoritative specification
└── examples/tests # visual test corpus
```

## Notes

- `spec/SPEC.md` is the source of truth for supported behavior.
- `examples/tests/` doubles as executable documentation.
- For rendering behavior changes, prefer updating both unit tests and visual tests.
