"""
pptx2png.py — Convert PPTX slides to PNG images via PowerPoint COM + PDF.
Usage: python pptx2png.py <input.pptx> [output_dir]

Requires: Windows + Microsoft PowerPoint installed, pymupdf, Pillow
"""
import sys, os, comtypes.client, fitz  # fitz = pymupdf

def pptx_to_images(pptx_path, output_dir=None, dpi=200):
    pptx_path = os.path.abspath(pptx_path)
    if output_dir is None:
        output_dir = os.path.join(os.path.dirname(pptx_path), "slides")
    os.makedirs(output_dir, exist_ok=True)

    base = os.path.splitext(os.path.basename(pptx_path))[0]
    pdf_path = os.path.join(output_dir, f"{base}.pdf")

    # Step 1: PPTX → PDF via PowerPoint COM
    print(f"Opening PowerPoint: {pptx_path}")
    powerpoint = comtypes.client.CreateObject("PowerPoint.Application")
    powerpoint.Visible = 1
    try:
        presentation = powerpoint.Presentations.Open(pptx_path, WithWindow=False)
        # SaveAs PDF (ppSaveAsPDF = 32)
        presentation.SaveAs(os.path.abspath(pdf_path), 32)
        print(f"PDF saved: {pdf_path}")
        presentation.Close()
    finally:
        powerpoint.Quit()

    # Step 2: PDF → PNG via pymupdf
    doc = fitz.open(pdf_path)
    zoom = dpi / 72.0
    mat = fitz.Matrix(zoom, zoom)
    for i, page in enumerate(doc):
        pix = page.get_pixmap(matrix=mat)
        out_file = os.path.join(output_dir, f"{base}-slide{i+1}.png")
        pix.save(out_file)
        print(f"  Slide {i+1} -> {out_file}")
    num_pages = len(doc)
    doc.close()
    os.remove(pdf_path)
    print(f"Done: {num_pages} slides rendered to {output_dir}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python pptx2png.py <input.pptx> [output_dir]")
        sys.exit(1)
    pptx_to_images(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
