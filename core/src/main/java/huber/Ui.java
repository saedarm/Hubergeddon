package huber;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Fonts, textures and a tiny immediate-mode button/panel kit on one SpriteBatch. 1280x720 virtual screen. */
public class Ui {
    public static final float W = 1280, H = 720;

    public final SpriteBatch batch = new SpriteBatch();
    public final FitViewport vp = new FitViewport(W, H);
    public BitmapFont tiny, small, mid, body, bold, big, huge;
    public float mx, my;
    public boolean clicked;
    public String tip;
    private final ObjectMap<String, Texture> textures = new ObjectMap<>();
    private final GlyphLayout gl = new GlyphLayout();
    private final Vector2 tmp = new Vector2();
    private NinePatch round, shadow;
    private float fontScale = -1;

    public Ui() {
        round = new NinePatch(tex("img/round.png"), 18, 18, 18, 18);
        shadow = new NinePatch(tex("img/shadow.png"), 34, 34, 34, 34);
        makeFonts();
    }

    /** Fonts are rendered at the real pixel size so they stay sharp when the window is big. */
    public void makeFonts() {
        float k = Math.max(1f, Gdx.graphics.getBackBufferHeight() / H);
        if (Math.abs(k - fontScale) < 0.2f) return;
        if (small != null) { tiny.dispose(); mid.dispose(); small.dispose(); body.dispose(); bold.dispose(); big.dispose(); huge.dispose(); }
        fontScale = k;
        FreeTypeFontGenerator reg = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Poppins-Regular.ttf"));
        FreeTypeFontGenerator bld = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Poppins-Bold.ttf"));
        tiny = font(reg, 12, k);
        small = font(reg, 14, k);
        mid = font(bld, 15, k);
        body = font(reg, 17, k);
        bold = font(bld, 19, k);
        big = font(bld, 30, k);
        huge = font(bld, 52, k);
        reg.dispose();
        bld.dispose();
    }

    private BitmapFont font(FreeTypeFontGenerator g, int size, float k) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = Math.round(size * k);
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "…–—’“”•★";
        BitmapFont f = g.generateFont(p);
        f.getData().setScale(1f / k);
        f.setUseIntegerPositions(false);
        f.getData().markupEnabled = false;
        return f;
    }

    public Texture tex(String path) {
        Texture t = textures.get(path);
        if (t == null) {
            t = new Texture(Gdx.files.internal(path), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            textures.put(path, t);
        }
        return t;
    }

    public void begin(boolean click) {
        vp.apply();
        batch.setProjectionMatrix(vp.getCamera().combined);
        tmp.set(Gdx.input.getX(), Gdx.input.getY());
        vp.unproject(tmp);
        mx = tmp.x; my = tmp.y;
        clicked = click;
        tip = null;
        batch.begin();
    }

    public void end() { batch.end(); }

    // ------------------------------------------------------------------ drawing

    public void rect(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(tex("img/white.png"), x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void panel(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        round.draw(batch, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void shadowPanel(float x, float y, float w, float h, Color c) {
        batch.setColor(1, 1, 1, 0.8f);
        shadow.draw(batch, x - 14, y - 18, w + 28, h + 28);
        panel(x, y, w, h, c);
    }

    public void img(String path, float x, float y, float w, float h) { img(path, x, y, w, h, Color.WHITE); }

    /** A straight line segment of the given thickness. */
    public void line(float x1, float y1, float x2, float y2, float thick, Color c) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        Texture t = tex("img/white.png");
        batch.setColor(c);
        batch.draw(t, x1, y1 - thick / 2, 0, thick / 2, len, thick, 1, 1, ang, 0, 0, t.getWidth(), t.getHeight(), false, false);
        batch.setColor(Color.WHITE);
    }

    public void img(String path, float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(tex(path), x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void imgRot(String path, float cx, float cy, float size, float rot, Color c) {
        Texture t = tex(path);
        batch.setColor(c);
        batch.draw(t, cx - size / 2, cy - size / 2, size / 2, size / 2, size, size, 1, 1, rot, 0, 0, t.getWidth(), t.getHeight(), false, false);
        batch.setColor(Color.WHITE);
    }

    /** Left-aligned, y is the top of the text. Returns the width. */
    public float text(BitmapFont f, String s, float x, float y, Color c) {
        f.setColor(c);
        gl.setText(f, s);
        f.draw(batch, gl, x, y);
        return gl.width;
    }

    public void textC(BitmapFont f, String s, float cx, float y, Color c) {
        f.setColor(c);
        gl.setText(f, s);
        f.draw(batch, gl, cx - gl.width / 2, y);
    }

    /** Wrapped text. Returns the height used. */
    public float textW(BitmapFont f, String s, float x, float y, float w, int align, Color c) {
        f.setColor(c);
        gl.setText(f, s, c, w, align, true);
        f.draw(batch, gl, x, y);
        return gl.height;
    }

    public float measureW(BitmapFont f, String s, float w) {
        gl.setText(f, s, Color.WHITE, w, Align.left, true);
        return gl.height;
    }

    public float width(BitmapFont f, String s) { gl.setText(f, s); return gl.width; }

    // ------------------------------------------------------------------ input

    public boolean hover(float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean button(String label, float x, float y, float w, float h, Color base, boolean enabled) {
        boolean over = enabled && hover(x, y, w, h);
        Color c = enabled ? (over ? base.cpy().lerp(Color.WHITE, 0.18f) : base) : new Color(0.35f, 0.36f, 0.4f, 1);
        panel(x, y - 3, w, h, c.cpy().mul(0.6f, 0.6f, 0.6f, 1));
        panel(x, y + (over && Gdx.input.isButtonPressed(0) ? -2 : 0), w, h, c);
        BitmapFont f = h >= 50 && width(big, label) < w - 28 ? big : bold;
        gl.setText(f, label);
        textC(f, label, x + w / 2, y + h / 2 + gl.height / 2 + 1, enabled ? Color.WHITE : new Color(0.75f, 0.75f, 0.78f, 1));
        return over && clicked;
    }

    public void dispose() {
        batch.dispose();
        for (Texture t : textures.values()) t.dispose();
        tiny.dispose(); mid.dispose(); small.dispose(); body.dispose(); bold.dispose(); big.dispose(); huge.dispose();
    }
}
