from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from bidi.algorithm import get_display
import arabic_reshaper
import markdown
import os
import re

def reshape_text(text):
    if not text: return ""
    reshaped_text = arabic_reshaper.reshape(text)
    return get_display(reshaped_text)

def md_to_pdf(md_file, pdf_file):
    with open(md_file, 'r', encoding='utf-8') as f:
        md_text = f.read()

    html_text = markdown.markdown(md_text, extensions=['extra'])

    # Simple conversion of HTML tags to ReportLab-friendly tags
    html_text = html_text.replace('<strong>', '<b>').replace('</strong>', '</b>')
    html_text = html_text.replace('<em>', '<i>').replace('</em>', '</i>')

    # Register font
    font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    if os.path.exists(font_path):
        pdfmetrics.registerFont(TTFont('DejaVu', font_path))
        font_name = 'DejaVu'
    else:
        font_name = 'Helvetica'

    doc = SimpleDocTemplate(pdf_file, pagesize=A4)
    styles = getSampleStyleSheet()

    # Custom RTL Style
    rtl_style = ParagraphStyle(
        name='Arabic',
        fontName=font_name,
        fontSize=12,
        leading=18,
        alignment=2, # Right alignment
        wordWrap='RTL'
    )

    h1_style = ParagraphStyle(name='H1', parent=rtl_style, fontSize=18, leading=24, spaceAfter=12)
    h2_style = ParagraphStyle(name='H2', parent=rtl_style, fontSize=14, leading=20, spaceAfter=10)

    story = []

    # Split by lines and basic tags for a simple Story
    lines = html_text.split('\n')
    for line in lines:
        if not line.strip(): continue

        # Clean tags to get raw text for reshaping, but keep <b> and <i>
        clean_text = re.sub('<[^<]+?>', '', line)
        if not clean_text.strip(): continue

        reshaped = reshape_text(clean_text)

        if line.startswith('<h1>'):
            story.append(Paragraph(reshaped, h1_style))
        elif line.startswith('<h2>'):
            story.append(Paragraph(reshaped, h2_style))
        else:
            story.append(Paragraph(reshaped, rtl_style))
        story.append(Spacer(1, 12))

    doc.build(story)

if __name__ == "__main__":
    md_to_pdf('duga-book/book.md', 'duga-book/book.pdf')
    print("PDF generated with improved layout.")
