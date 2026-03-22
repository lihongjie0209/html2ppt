#!/usr/bin/env python3
"""
Convert PowerPoint files to PNG images.
Uses Microsoft Office COM API (Windows only) for PPTX->PDF, then pdf2image for PDF->PNG.

Usage: python ppt_to_images.py input.pptx output_dir/

Requirements (Windows):
  pip install pdf2image pywin32
  Microsoft PowerPoint installed
  Poppler installed (for pdf2image) - conda install -c conda-forge poppler
"""

import sys
import os
import tempfile
from pathlib import Path

# Try COM API first (Microsoft Office), fallback to pdf2image only
try:
    import win32com.client
    HAS_WIN32 = True
except ImportError:
    HAS_WIN32 = False
    print("Warning: pywin32 not installed. Install with: pip install pywin32")

try:
    from pdf2image import convert_from_path
    HAS_PDF2IMAGE = True
except ImportError:
    HAS_PDF2IMAGE = False
    print("Warning: pdf2image not installed. Install with: pip install pdf2image")


def pptx_to_pdf_com(pptx_path: Path, pdf_path: Path) -> bool:
    """Convert PPTX to PDF using PowerPoint COM API."""
    if not HAS_WIN32:
        return False
    
    powerpoint = None
    presentation = None
    try:
        powerpoint = win32com.client.Dispatch("PowerPoint.Application")
        powerpoint.Visible = True  # Must be visible for export
        
        # Open presentation
        presentation = powerpoint.Presentations.Open(str(pptx_path.resolve()), WithWindow=False)
        
        # Export as PDF (ppSaveAsPDF = 32)
        presentation.SaveAs(str(pdf_path.resolve()), 32)
        
        return True
    except Exception as e:
        print(f"PowerPoint COM error: {e}")
        return False
    finally:
        if presentation:
            presentation.Close()
        if powerpoint:
            powerpoint.Quit()


def pptx_to_images_com(pptx_path: Path, output_dir: Path, base_name: str) -> list[Path]:
    """Export slides directly to images using PowerPoint COM API."""
    if not HAS_WIN32:
        return []
    
    powerpoint = None
    presentation = None
    output_paths = []
    
    try:
        powerpoint = win32com.client.Dispatch("PowerPoint.Application")
        powerpoint.Visible = True
        
        presentation = powerpoint.Presentations.Open(str(pptx_path.resolve()), WithWindow=False)
        
        # Export each slide as PNG
        for i, slide in enumerate(presentation.Slides, start=1):
            output_path = output_dir / f"{base_name}-slide{i}.png"
            slide.Export(str(output_path.resolve()), "PNG", 1920, 1080)
            output_paths.append(output_path)
            print(f"  Saved: {output_path.name}")
        
        return output_paths
    except Exception as e:
        print(f"PowerPoint COM error: {e}")
        return []
    finally:
        if presentation:
            presentation.Close()
        if powerpoint:
            powerpoint.Quit()


def pdf_to_images(pdf_path: Path, output_dir: Path, base_name: str) -> list[Path]:
    """Convert PDF pages to PNG images."""
    if not HAS_PDF2IMAGE:
        print("pdf2image not available")
        return []
    
    images = convert_from_path(pdf_path, dpi=150)
    
    output_paths = []
    for i, img in enumerate(images, start=1):
        output_path = output_dir / f"{base_name}-slide{i}.png"
        img.save(output_path, "PNG")
        output_paths.append(output_path)
        print(f"  Saved: {output_path.name}")
    
    return output_paths


def main():
    if len(sys.argv) < 3:
        print("Usage: python ppt_to_images.py input.pptx output_dir/")
        sys.exit(1)
    
    pptx_path = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    
    if not pptx_path.exists():
        print(f"File not found: {pptx_path}")
        sys.exit(1)
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    base_name = pptx_path.stem
    
    print(f"Converting: {pptx_path.name}")
    
    # Method 1: Direct export via PowerPoint COM (best quality)
    if HAS_WIN32:
        print("  Using PowerPoint COM API for direct export...")
        output_paths = pptx_to_images_com(pptx_path, output_dir, base_name)
        if output_paths:
            print(f"Done! Generated {len(output_paths)} images.")
            return
    
    # Method 2: PPTX -> PDF -> PNG
    if HAS_WIN32 and HAS_PDF2IMAGE:
        print("  Fallback: PPTX -> PDF -> PNG...")
        with tempfile.TemporaryDirectory() as tmp:
            pdf_path = Path(tmp) / f"{base_name}.pdf"
            if pptx_to_pdf_com(pptx_path, pdf_path):
                output_paths = pdf_to_images(pdf_path, output_dir, base_name)
                if output_paths:
                    print(f"Done! Generated {len(output_paths)} images.")
                    return
    
    print("Error: No conversion method available. Install pywin32 and/or PowerPoint.")
    sys.exit(1)


if __name__ == "__main__":
    main()
