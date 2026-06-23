from ebooklib import epub
import markdown

def md_to_epub(md_file, epub_file):
    with open(md_file, 'r', encoding='utf-8') as f:
        md_text = f.read()

    html_content = markdown.markdown(md_text)

    book = epub.EpubBook()
    book.set_identifier('duga-book-001')
    book.set_title('دوغا: نقار الخشب الروسي')
    book.set_language('ar')
    book.add_author('Jules')

    style = 'body { direction: rtl; text-align: right; font-family: sans-serif; }'
    nav_css = epub.EpubItem(uid="style_nav", file_name="style/nav.css", media_type="text/css", content=style)
    book.add_item(nav_css)

    c1 = epub.EpubHtml(title='دوغا: نقار الخشب الروسي', file_name='book.xhtml', lang='ar')
    c1.content = f'<html dir="rtl" lang="ar"><body>{html_content}</body></html>'
    c1.add_item(nav_css)

    book.add_item(c1)
    book.toc = (epub.Link('book.xhtml', 'الكتاب', 'intro'), )
    book.add_item(epub.EpubNcx())
    book.add_item(epub.EpubNav())
    book.spine = ['nav', c1]

    epub.write_epub(epub_file, book, {})

if __name__ == "__main__":
    md_to_epub('duga-book/book.md', 'duga-book/book.epub')
    print("EPUB generated.")
