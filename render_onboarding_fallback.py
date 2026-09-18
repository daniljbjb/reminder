from __future__ import annotations

import re
from pathlib import Path
from xml.sax.saxutils import escape

from docx import Document
from docx.document import Document as DocumentType
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph as PdfParagraph,
    Spacer,
    Table as PdfTable,
    TableStyle,
)


SOURCE = Path(r"D:\projects\reminder\reminder-rest-onboarding-V3.docx")
OUTPUT = Path(r"D:\projects\reminder\.qa-v3\final\reminder-rest-onboarding-V3.pdf")


def register_fonts() -> None:
    pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
    pdfmetrics.registerFont(TTFont("Arial-Bold", r"C:\Windows\Fonts\arialbd.ttf"))
    pdfmetrics.registerFont(TTFont("Arial-Italic", r"C:\Windows\Fonts\ariali.ttf"))
    pdfmetrics.registerFont(TTFont("Consolas", r"C:\Windows\Fonts\consola.ttf"))


def iter_blocks(parent):
    root = parent.element.body if isinstance(parent, DocumentType) else parent._tc
    for child in root.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, parent)
        elif isinstance(child, CT_Tbl):
            yield Table(child, parent)


def has_page_break(paragraph: Paragraph) -> bool:
    if paragraph.paragraph_format.page_break_before is True:
        return True
    return bool(paragraph._p.xpath('.//w:br[@w:type="page"]'))


def is_code(paragraph: Paragraph) -> bool:
    if "List Bullet" in paragraph.style.name:
        return False
    if "\n" in paragraph.text and ("mvnw" in paragraph.text or "MAIL_PASSWORD" in paragraph.text):
        return True
    for run in paragraph.runs:
        name = run.font.name or ""
        if "Mono" in name or "Consolas" in name or "Cascadia" in name:
            return True
    return bool(re.match(r"^(public|private|protected|void|Page<|Optional<|ReminderDto |Clock |JwtDecoder |SecurityFilterChain )", paragraph.text.strip()))


def soft_breaks(text: str) -> str:
    text = escape(text)
    text = re.sub(r"([/_.,(){}&lt;&gt;=-])", r"\1&#8203;", text)
    text = text.replace("\n", "<br/>")
    return text


def make_styles():
    styles = getSampleStyleSheet()
    return {
        "body": ParagraphStyle("BodyRu", parent=styles["BodyText"], fontName="Arial", fontSize=10.7, leading=14.2, textColor=colors.HexColor("#202020"), spaceAfter=5.5, splitLongWords=True),
        "bullet": ParagraphStyle("BulletRu", parent=styles["BodyText"], fontName="Arial", fontSize=10.5, leading=14, leftIndent=16, firstLineIndent=-10, bulletIndent=2, spaceAfter=4.5, splitLongWords=True),
        "title": ParagraphStyle("TitleRu", parent=styles["Title"], fontName="Arial-Bold", fontSize=28, leading=32, alignment=TA_LEFT, textColor=colors.black, spaceAfter=12),
        "subtitle": ParagraphStyle("SubtitleRu", parent=styles["BodyText"], fontName="Arial", fontSize=15, leading=19, textColor=colors.HexColor("#3A3A3A"), spaceAfter=14),
        "h1": ParagraphStyle("H1Ru", parent=styles["Heading1"], fontName="Arial-Bold", fontSize=20, leading=24, textColor=colors.black, spaceBefore=12, spaceAfter=8, keepWithNext=True),
        "h2": ParagraphStyle("H2Ru", parent=styles["Heading2"], fontName="Arial-Bold", fontSize=15.5, leading=19, textColor=colors.black, spaceBefore=10, spaceAfter=6, keepWithNext=True),
        "h3": ParagraphStyle("H3Ru", parent=styles["Heading3"], fontName="Arial-Bold", fontSize=13.2, leading=16.5, textColor=colors.black, spaceBefore=8, spaceAfter=4, keepWithNext=True),
        "h4": ParagraphStyle("H4Ru", parent=styles["Heading4"], fontName="Arial-Bold", fontSize=11.5, leading=14, textColor=colors.black, spaceBefore=6, spaceAfter=4, keepWithNext=True),
        "source": ParagraphStyle("SourceRu", parent=styles["BodyText"], fontName="Arial-Italic", fontSize=9.2, leading=12, textColor=colors.HexColor("#555555"), spaceAfter=5, splitLongWords=True),
        "code": ParagraphStyle("CodeRu", parent=styles["Code"], fontName="Consolas", fontSize=8.6, leading=11.3, textColor=colors.HexColor("#17365D"), spaceAfter=5, splitLongWords=True, wordWrap="CJK", keepWithNext=True),
        "codeblock": ParagraphStyle("CodeBlockRu", parent=styles["Code"], fontName="Consolas", fontSize=8.8, leading=12, textColor=colors.HexColor("#202020"), leftIndent=10, rightIndent=10, borderPadding=6, backColor=colors.HexColor("#F2F4F6"), spaceBefore=3, spaceAfter=7),
        "footer": ParagraphStyle("FooterRu", parent=styles["BodyText"], fontName="Arial", fontSize=8.5, textColor=colors.HexColor("#666666"), alignment=TA_CENTER),
        "table": ParagraphStyle("TableRu", parent=styles["BodyText"], fontName="Arial", fontSize=8.4, leading=10.6, textColor=colors.HexColor("#202020"), splitLongWords=True),
        "table_head": ParagraphStyle("TableHeadRu", parent=styles["BodyText"], fontName="Arial-Bold", fontSize=8.5, leading=10.8, textColor=colors.white, alignment=TA_CENTER, splitLongWords=True),
    }


def footer(canvas, document) -> None:
    canvas.saveState()
    canvas.setFont("Arial", 8.5)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawCentredString(letter[0] / 2, 0.36 * inch, f"Страница {document.page}")
    canvas.restoreState()


def paragraph_flowable(paragraph: Paragraph, styles):
    text = paragraph.text.strip()
    if not text:
        return Spacer(1, 3)
    style_name = paragraph.style.name
    if style_name == "Title":
        style = styles["title"]
    elif style_name == "Subtitle":
        style = styles["subtitle"]
    elif style_name == "Heading 1":
        style = styles["h1"]
    elif style_name == "Heading 2":
        style = styles["h2"]
    elif style_name == "Heading 3":
        style = styles["h3"]
    elif style_name == "Heading 4":
        style = styles["h4"]
    elif text.startswith("Исходник"):
        style = styles["source"]
    elif is_code(paragraph):
        style = styles["codeblock"] if "\n" in paragraph.text else styles["code"]
    elif "List Bullet" in style_name:
        return PdfParagraph(soft_breaks(text), styles["bullet"], bulletText="•")
    else:
        style = styles["body"]
    return PdfParagraph(soft_breaks(text), style)


def table_flowable(table: Table, styles):
    rows = []
    for row_index, row in enumerate(table.rows):
        cells = []
        for cell in row.cells:
            text = "<br/>".join(soft_breaks(p.text.strip()) for p in cell.paragraphs if p.text.strip())
            cells.append(PdfParagraph(text or " ", styles["table_head"] if row_index == 0 else styles["table"]))
        rows.append(cells)
    count = len(rows[0])
    usable = letter[0] - 1.56 * inch
    if count == 4:
        widths = [0.55 * inch, 2.55 * inch, 1.8 * inch, usable - 4.9 * inch]
    else:
        widths = [1.3 * inch, 2.35 * inch, usable - 3.65 * inch]
    pdf_table = PdfTable(rows, colWidths=widths, repeatRows=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1F3F66")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#D9D9D9")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for row_index in range(1, len(rows)):
        if row_index % 2 == 0:
            commands.append(("BACKGROUND", (0, row_index), (-1, row_index), colors.HexColor("#EEF4F8")))
    pdf_table.setStyle(TableStyle(commands))
    return pdf_table


def main() -> None:
    register_fonts()
    styles = make_styles()
    source = Document(SOURCE)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    doc = BaseDocTemplate(
        str(OUTPUT),
        pagesize=letter,
        leftMargin=0.78 * inch,
        rightMargin=0.78 * inch,
        topMargin=0.70 * inch,
        bottomMargin=0.62 * inch,
        title="Онбординг по REST сервису reminder",
        author="Codex",
    )
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="main")
    doc.addPageTemplates(PageTemplate(id="default", frames=frame, onPage=footer))

    story = []
    for block in iter_blocks(source):
        if isinstance(block, Paragraph):
            if has_page_break(block) and story:
                story.append(PageBreak())
            story.append(paragraph_flowable(block, styles))
        else:
            story.extend([Spacer(1, 4), table_flowable(block, styles), Spacer(1, 7)])

    doc.build(story)
    print(OUTPUT)


if __name__ == "__main__":
    main()
