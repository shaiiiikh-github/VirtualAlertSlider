import os
from PIL import Image, ImageDraw

def create_app_icon(size):
    # Create image with RGBA
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Radius for background squircle/rounded rectangle
    r = int(size * 0.22)
    
    # Draw dark slate background (#0F172A)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=(15, 23, 42, 255))
    
    # Subtle inner border (#1E293B)
    border_w = max(1, int(size * 0.02))
    draw.rounded_rectangle([border_w, border_w, size - 1 - border_w, size - 1 - border_w], radius=r-border_w, outline=(30, 41, 59, 255), width=border_w)
    
    # Draw Left Side: Vertical Slider Track
    # Safe area coords scale with size
    # Track center_x = 0.36 * size
    track_w = max(3, int(size * 0.08))
    track_x1 = int(size * 0.32)
    track_x2 = track_x1 + track_w
    track_y1 = int(size * 0.26)
    track_y2 = int(size * 0.74)
    track_r = max(2, int(track_w / 2))
    
    # Dark track (#334155)
    draw.rounded_rectangle([track_x1, track_y1, track_x2, track_y2], radius=track_r, fill=(51, 65, 85, 255))
    
    # Blue active section (bottom part of track #3B82F6)
    active_y1 = int(size * 0.48)
    draw.rounded_rectangle([track_x1, active_y1, track_x2, track_y2], radius=track_r, fill=(59, 130, 246, 255))
    
    # Slider Knob Thumb (#EF4444 - Alert Red)
    knob_w = int(size * 0.20)
    knob_h = int(size * 0.14)
    knob_cx = int((track_x1 + track_x2) / 2)
    knob_cy = int(size * 0.48)
    knob_x1 = knob_cx - int(knob_w / 2)
    knob_x2 = knob_x1 + knob_w
    knob_y1 = knob_cy - int(knob_h / 2)
    knob_y2 = knob_y1 + knob_h
    knob_r = max(2, int(knob_h * 0.3))
    
    draw.rounded_rectangle([knob_x1, knob_y1, knob_x2, knob_y2], radius=knob_r, fill=(239, 68, 68, 255))
    
    # White accent line inside knob
    line_w = int(knob_w * 0.4)
    line_x1 = knob_cx - int(line_w / 2)
    line_x2 = line_x1 + line_w
    line_y = knob_cy
    draw.line([line_x1, line_y, line_x2, line_y], fill=(255, 255, 255, 230), width=max(1, int(size * 0.02)))
    
    # Draw Right Side: Bell Icon & Sound Waves
    bell_cx = int(size * 0.66)
    bell_cy = int(size * 0.48)
    bell_w = int(size * 0.22)
    bell_h = int(size * 0.24)
    
    # Draw bell body in White (#FFFFFF)
    bx1 = bell_cx - int(bell_w / 2)
    bx2 = bell_cx + int(bell_w / 2)
    by1 = bell_cy - int(bell_h / 2)
    by2 = bell_cy + int(bell_h / 3)
    
    # Bell top cap dot
    cap_r = max(1, int(size * 0.025))
    draw.ellipse([bell_cx - cap_r, by1 - cap_r*2, bell_cx + cap_r, by1], fill=(255, 255, 255, 255))
    
    # Bell dome
    draw.rounded_rectangle([bx1 + int(bell_w*0.15), by1, bx2 - int(bell_w*0.15), by2], radius=int(bell_w*0.3), fill=(255, 255, 255, 255))
    # Bell flare rim at bottom
    rim_h = max(2, int(size * 0.03))
    draw.rounded_rectangle([bx1, by2 - rim_h, bx2, by2 + rim_h], radius=int(rim_h/2), fill=(255, 255, 255, 255))
    
    # Bell clapper dot at bottom center (#FFFFFF)
    clap_r = max(2, int(size * 0.035))
    draw.ellipse([bell_cx - clap_r, by2 + rim_h, bell_cx + clap_r, by2 + rim_h + clap_r*2], fill=(255, 255, 255, 255))
    
    # Sound wave arc (#38BDF8 - Sky Blue)
    wave_x = bx2 + int(size * 0.05)
    if wave_x + int(size * 0.08) < size - int(size * 0.08):
        wave_y1 = bell_cy - int(size * 0.08)
        wave_y2 = bell_cy + int(size * 0.08)
        draw.arc([wave_x, wave_y1, wave_x + int(size * 0.08), wave_y2], start=290, end=70, fill=(56, 189, 248, 255), width=max(1, int(size * 0.025)))
        
    return img

def main():
    res_dir = r"c:\Users\SHABBIR\.gemini\antigravity-ide\scratch\VirtualAlertSlider\app\src\main\res"
    
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    for folder, sz in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        img = create_app_icon(sz)
        img.save(os.path.join(folder_path, "ic_launcher.png"))
        img.save(os.path.join(folder_path, "ic_launcher_round.png"))
        print(f"Saved {folder}/ic_launcher.png ({sz}x{sz})")

if __name__ == "__main__":
    main()
