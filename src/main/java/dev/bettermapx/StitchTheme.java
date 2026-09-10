package dev.bettermapx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Google Stitch design tokens and UI renderers for BetterMapX.
 * Recreates the Translucent Cyber-Fantasy HUD aesthetic:
 * dark glassmorphic panels, crisp metallic bevels, telemetry accents,
 * and stable bloom glow effects.
 */
public final class StitchTheme {
    private StitchTheme() {}

    // Surface & Glassmorphism Colors
    public static final int BG_SURFACE = 0xf00f131b;
    public static final int BG_SURFACE_LOW = 0xee181c24;
    public static final int PANEL_BG = 0xf0141822;
    public static final int PANEL_INNER = 0xfa0d1117;
    public static final int SHADOW_DROPDOWN = 0xb0000000;

    // Bevel & Border Colors
    public static final int BORDER_OUTER = 0xff3e4248;
    public static final int BORDER_HIGHLIGHT = 0xff5a5f66;
    public static final int BORDER_SHADOW = 0xff090a0b;
    public static final int BORDER_CYAN = 0x7006b6d4;

    // Button Colors
    public static final int BTN_BG = 0xee25282c;
    public static final int BTN_BORDER = 0xff101214;
    public static final int BTN_HIGHLIGHT = 0xff4d5259;
    public static final int BTN_SHADOW = 0xff18191b;
    public static final int BTN_HOVER_BG = 0xee33373d;
    public static final int BTN_HOVER_BORDER = 0xff06b6d4;
    public static final int BTN_HOVER_HIGHLIGHT = 0xff676d75;

    // Active Tab / Selected Colors
    public static final int TAB_ACTIVE_BG = 0xee1f3325;
    public static final int TAB_ACTIVE_BORDER = 0xff2e693f;
    public static final int TAB_ACTIVE_HIGHLIGHT = 0xff479e5f;
    public static final int TAB_ACTIVE_TEXT = 0xff4edea3;

    // Functional & Telemetry Accents
    public static final int CYAN_PRIMARY = 0xff4cd7f6;
    public static final int CYAN_ACCENT = 0xff06b6d4;
    public static final int CYAN_DEEP = 0xff00424f;
    public static final int EMERALD_GREEN = 0xff4edea3;
    public static final int EMERALD_ACCENT = 0xff10b981;
    public static final int AMBER_GOLD = 0xfff59e0b;
    public static final int VIOLET_ARCANE = 0xff8b5cf6;
    public static final int MAGENTA_MYTHICAL = 0xffd946ef;
    public static final int SHINY_GOLD = 0xfffbbf24;

    // Typography Colors
    public static final int TEXT_WHITE = 0xffffffff;
    public static final int TEXT_ON_SURFACE = 0xffdfe2ee;
    public static final int TEXT_MUTED = 0xff94a3b8;
    public static final int TEXT_DARK = 0xff64748b;
    public static final int TEXT_CYAN = 0xff38bdf8;

    // Textures
    public static final Identifier POKEBALL_TEXTURE = Identifier.of("bettermapx", "textures/gui/pokeball.png");

    /**
     * Renders a Stitch '.mc-bevel' / '.mc-panel' container.
     * Features crisp 1px metallic border, top/left sub-pixel highlight,
     * bottom/right inset shadow, and dark glass backplate.
     */
    public static void drawBevelPanel(DrawContext d, int x, int y, int w, int h) {
        drawBevelPanel(d, x, y, w, h, PANEL_BG, BORDER_OUTER);
    }

    public static void drawBevelPanel(DrawContext d, int x, int y, int w, int h, int bg, int border) {
        // Outer drop shadow
        d.fill(x + 2, y + h, x + w, y + h + 2, 0x55000000);
        d.fill(x + w, y + 2, x + w + 2, y + h, 0x55000000);

        // Outer border
        d.drawBorder(x, y, w, h, border);

        // Glassmorphic background
        d.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);

        // Top & Left inner metallic highlight
        d.fill(x + 1, y + 1, x + w - 1, y + 2, BORDER_HIGHLIGHT);
        d.fill(x + 1, y + 1, x + 2, y + h - 1, BORDER_HIGHLIGHT);

        // Bottom & Right inner shadow
        d.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BORDER_SHADOW);
        d.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BORDER_SHADOW);
    }

    /**
     * Renders a Stitch '.mc-bevel-btn' button.
     */
    public static void drawBevelButton(DrawContext d, int x, int y, int w, int h, boolean hovered, boolean active, boolean selected) {
        if (selected) {
            d.drawBorder(x, y, w, h, TAB_ACTIVE_BORDER);
            d.fill(x + 1, y + 1, x + w - 1, y + h - 1, TAB_ACTIVE_BG);
            d.fill(x + 1, y + 1, x + w - 1, y + 2, TAB_ACTIVE_HIGHLIGHT);
            d.fill(x + 1, y + 1, x + 2, y + h - 1, TAB_ACTIVE_HIGHLIGHT);
            d.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xff0e1e13);
            d.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xff0e1e13);
            return;
        }

        int bg = hovered ? BTN_HOVER_BG : BTN_BG;
        int border = hovered ? BTN_HOVER_BORDER : BTN_BORDER;
        int highlight = hovered ? BTN_HOVER_HIGHLIGHT : BTN_HIGHLIGHT;
        int shadow = BTN_SHADOW;

        if (!active) {
            bg = 0xbb18191c;
            border = 0xff25282c;
            highlight = 0xff2c3036;
            shadow = 0xff101214;
        }

        // Base frame
        d.drawBorder(x, y, w, h, border);
        d.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);

        // Bevel highlights
        d.fill(x + 1, y + 1, x + w - 1, y + 2, highlight);
        d.fill(x + 1, y + 1, x + 2, y + h - 1, highlight);
        d.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, shadow);
        d.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, shadow);

        // Tactical cyan micro-glow on hover
        if (hovered && active) {
            d.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, 0x9906b6d4);
        }
    }

    /**
     * Renders a Stitch 32x16 pill toggle switch with 10px circular slider thumb.
     */
    public static void drawToggleSwitch(DrawContext d, int x, int y, boolean enabled, float thumbAnim) {
        int trackW = 32, trackH = 16;
        int trackBg = enabled ? 0xee003824 : 0xee181c24;
        int trackBorder = enabled ? 0xff10b981 : 0xff3d494c;

        // Pill track
        d.fill(x + 2, y, x + trackW - 2, y + trackH, trackBg);
        d.fill(x, y + 2, x + trackW, y + trackH - 2, trackBg);
        d.drawBorder(x, y, trackW, trackH, trackBorder);

        // Thumb position interpolation (x+3 to x+19)
        int thumbStartX = x + 3;
        int thumbEndX = x + trackW - 13;
        int thumbX = (int) Math.round(thumbStartX + (thumbEndX - thumbStartX) * Math.clamp(thumbAnim, 0f, 1f));
        int thumbY = y + 3;

        int thumbColor = enabled ? CYAN_PRIMARY : 0xff64748b;
        int thumbBorder = enabled ? 0xffffffff : 0xff334155;

        // Thumb circular-ish 10x10
        d.fill(thumbX + 1, thumbY, thumbX + 9, thumbY + 10, thumbColor);
        d.fill(thumbX, thumbY + 1, thumbX + 10, thumbY + 9, thumbColor);
        d.drawBorder(thumbX, thumbY, 10, 10, thumbBorder);

        if (enabled) {
            // Inner cyan core
            d.fill(thumbX + 3, thumbY + 3, thumbX + 7, thumbY + 7, 0xffffffff);
        }
    }

    /**
     * Renders a badge / chip in the Stitch style.
     */
    public static void drawChip(DrawContext d, TextRenderer tr, int x, int y, String text, int textColor, int bgColor, int borderColor) {
        int textW = tr.getWidth(text);
        int chipW = textW + 8;
        int chipH = 13;

        d.fill(x, y, x + chipW, y + chipH, bgColor);
        d.drawBorder(x, y, chipW, chipH, borderColor);
        d.drawText(tr, text, x + 4, y + 3, textColor, false);
    }

    /**
     * Procedural Stitch Pokéball icon (drawn if PNG is unavailable or inline).
     */
    public static void drawProceduralPokeball(DrawContext d, int x, int y, int size) {
        int r = size / 2;
        int cx = x + r;
        int cy = y + r;

        // Dark outer boundary
        d.fill(cx - r + 1, cy - r, cx + r - 1, cy + r, 0xff0a0d14);
        d.fill(cx - r, cy - r + 1, cx + r, cy + r - 1, 0xff0a0d14);

        // Upper red hemisphere
        d.fill(cx - r + 1, cy - r + 1, cx + r - 1, cy, 0xffdc2626);

        // Lower white hemisphere
        d.fill(cx - r + 1, cy + 1, cx + r - 1, cy + r - 1, 0xfff1f5f9);

        // Center black seam
        d.fill(cx - r + 1, cy, cx + r - 1, cy + 1, 0xff0f172a);

        // Center button outer & inner
        int btnR = Math.max(2, size / 5);
        d.fill(cx - btnR, cy - btnR, cx + btnR + 1, cy + btnR + 1, 0xff0f172a);
        d.fill(cx - btnR + 1, cy - btnR + 1, cx + btnR, cy + btnR, 0xffffffff);
    }
}
