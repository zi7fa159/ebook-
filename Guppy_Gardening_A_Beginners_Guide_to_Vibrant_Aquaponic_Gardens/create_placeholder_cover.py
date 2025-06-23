from PIL import Image, ImageDraw, ImageFont
import os

# Define image properties
width = 1800
height = 2700
background_color = (0, 128, 128) # Dark Teal (R:0, G:128, B:128)
text_color = (255, 255, 255) # White
font_size_title = 120
font_size_subtitle = 70
font_size_author = 50
placeholder_text_color = (200, 200, 200) # Lighter gray for placeholder note

# Text content
title_text = "Guppy Gardening"
subtitle_text = "A Beginner's Guide to\nVibrant Aquaponic Gardens"
author_text = "AI Publishing Pro"
placeholder_note = "Placeholder Cover Image"

# Define output path - save in the same directory as the script
img_path = "cover.png"

# Create image
img = Image.new('RGB', (width, height), color = background_color)
d = ImageDraw.Draw(img)

try:
    font_path_bold = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
    font_path_regular = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    font_path_italic = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf"

    if not os.path.exists(font_path_bold): font_path_bold = "arialbd.ttf"
    if not os.path.exists(font_path_regular): font_path_regular = "arial.ttf"
    if not os.path.exists(font_path_italic): font_path_italic = "ariali.ttf"

    font_title = ImageFont.truetype(font_path_bold, font_size_title)
    font_subtitle = ImageFont.truetype(font_path_regular, font_size_subtitle)
    font_author = ImageFont.truetype(font_path_italic, font_size_author)
    font_placeholder = ImageFont.truetype(font_path_regular, font_size_subtitle - 10)

except IOError:
    font_title = ImageFont.load_default()
    font_subtitle = ImageFont.load_default()
    font_author = ImageFont.load_default()
    font_placeholder = ImageFont.load_default()
    print("Default fonts loaded as DejaVu or Arial were not found. Placeholder quality may be reduced.")

def get_text_dimensions(draw_context, text_content, font_object):
    if hasattr(draw_context, 'textbbox'):
        bbox = draw_context.textbbox((0, 0), text_content, font=font_object)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
    else:
        text_width, text_height = draw_context.textsize(text_content, font=font_object)
    return text_width, text_height

title_w, title_h = get_text_dimensions(d, title_text, font_title)
d.text(((width - title_w) / 2, height * 0.2), title_text, fill=text_color, font=font_title)

subtitle_w, subtitle_h = get_text_dimensions(d, subtitle_text, font_subtitle)
d.text(((width - subtitle_w) / 2, height * 0.35), subtitle_text, fill=text_color, font=font_subtitle, align="center")

author_w, author_h = get_text_dimensions(d, author_text, font_author)
d.text(((width - author_w) / 2, height * 0.55), author_text, fill=text_color, font=font_author)

placeholder_w, placeholder_h = get_text_dimensions(d, placeholder_note, font_placeholder)
d.text(((width - placeholder_w) / 2, height * 0.85), placeholder_note, fill=placeholder_text_color, font=font_placeholder)

img.save(img_path)
print(f"Placeholder cover.png saved to: {os.path.abspath(img_path)}")
