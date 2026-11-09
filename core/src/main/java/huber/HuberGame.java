package huber;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/** The app: title, join, lobby and table screens, plus turning game events into effects and sound. */
public class HuberGame extends ApplicationAdapter {
    enum Screen { TITLE, JOIN, HELP, GAME, RECONNECT }

    // colours
    static final Color INK = new Color(0.07f, 0.08f, 0.12f, 0.9f), INK2 = new Color(0.12f, 0.13f, 0.19f, 0.95f);
    static final Color GOLD = new Color(0.98f, 0.8f, 0.08f, 1), RED = new Color(0.86f, 0.15f, 0.15f, 1), GREEN = new Color(0.13f, 0.7f, 0.33f, 1);
    static final Color BLUE = new Color(0.23f, 0.51f, 0.96f, 1), PURPLE = new Color(0.55f, 0.36f, 0.96f, 1), ORANGE = new Color(0.92f, 0.45f, 0.1f, 1);
    static final Color GREY = new Color(0.62f, 0.64f, 0.7f, 1), BRICK = new Color(0.55f, 0.2f, 0.14f, 0.95f), SOFT = new Color(0.85f, 0.87f, 0.92f, 1);

    private Ui ui;
    private Screen screen = Screen.TITLE;
    private Session session;
    private Preferences prefs;
    private final StringBuilder name = new StringBuilder(), address = new StringBuilder();
    private int focus;   // 1 = name field, 2 = address field
    private boolean click, muted, paused;
    private String notice;
    private float noticeT, time;
    private volatile Session joined;
    private volatile String joinError;
    private volatile boolean joining;
    private final ObjectMap<String, Sound> sfx = new ObjectMap<>();
    private final ObjectMap<String, Float> sfxCool = new ObjectMap<>();
    private final ArrayList<String> myIps = new ArrayList<>();

    // table effects
    private float rollAnim, bannerT, bannerMax, batchDelay;
    private String banner, bannerSub;
    private Color bannerColor = RED;
    private final float[] shake = new float[6];
    private final String[] bubble = new String[6];
    private final float[] bubbleT = new float[6];
    private final Array<Floater> floaters = new Array<>();
    private final ArrayDeque<String> log = new ArrayDeque<>();

    // automation for testing: -Dauto=game|lobby|title -Dshots=dir -Dshotat=1,4,9
    private String shotsDir;
    private final ArrayList<Float> shotAt = new ArrayList<>();
    private int shotN;

    static class Floater {
        int seat; String text; Color color; String icon; float t, delay;
    }

    /** A portrait flying between a player's card and Huber. */
    static class Flight {
        int seat; boolean enter; float x0, y0, s0, x1, y1, s1, t, dur = 0.75f, delay;
    }

    /** A zap bolt from one seat to another. */
    static class Bolt {
        int from, to, amt; float t, delay;
    }

    /** Icons and puffs. homing: curve from (x,y) to (tx,ty). Otherwise: velocity and gravity. */
    static class Part {
        String img; float x, y, tx, ty, vx, vy, size, t, life, delay, rot, spin; boolean homing; Color color = Color.WHITE; String text;
    }

    // music, chat, reconnecting, report card
    private Music lobbyMusic, gameMusic, playing;
    private boolean musicOn = true, chatOpen, swallowTyped;
    private final StringBuilder chatMsg = new StringBuilder();
    private final ArrayDeque<String[]> lobbyChat = new ArrayDeque<>();
    private String lastHost, reconnectMsg;
    private int lastPort;
    private boolean lastTls;
    private volatile int reconnectTry;
    private Game recordedFor;
    private String careerLine = "";
    private final Array<Flight> flights = new Array<>();
    private final Array<Bolt> bolts = new Array<>();
    private final Array<Part> parts = new Array<>();
    private float screenShake;
    private String bannerImg;

    @Override
    public void create() {
        ui = new Ui();
        prefs = Gdx.app.getPreferences("hubergeddon");
        name.append(prefs.getString("name", "Sam"));
        address.append(prefs.getString("address", ""));
        muted = prefs.getBoolean("muted", false);
        musicOn = prefs.getBoolean("music", true);
        lobbyMusic = Gdx.audio.newMusic(Gdx.files.internal("music/lobby.ogg"));
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("music/game.ogg"));
        lobbyMusic.setLooping(true);
        gameMusic.setLooping(true);
        for (String s : new String[]{"roll", "click", "zap", "hit", "heal", "battery", "star", "enter", "leave", "ko", "win", "buy", "mishap", "sweep", "turn", "ghost", "power"})
            sfx.put(s, Gdx.audio.newSound(Gdx.files.internal("sfx/" + s + ".wav")));
        findIps();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchUp(int x, int y, int pointer, int button) { if (button == 0) click = true; return true; }
            @Override public boolean keyTyped(char c) { typed(c); return true; }
            @Override public boolean keyDown(int key) { return keyPressed(key); }
        });
        String auto = System.getProperty("auto", "");
        shotsDir = System.getProperty("shots");
        for (String s : System.getProperty("shotat", "").split(",")) if (!s.isBlank()) shotAt.add(Float.parseFloat(s));
        if (auto.equals("game") || auto.equals("game6")) {
            session = Session.local(name.toString());
            if (auto.equals("game6")) { session.addBot(); session.addBot(); }
            session.pick(0);
            session.fastBots = true;
            session.start();
            session.game.p[0].bot = true;
            screen = Screen.GAME;
        } else if (auto.equals("yield") || auto.equals("power") || auto.equals("over")) {
            session = Session.local(name.toString());
            session.pick(0);
            session.start();
            Game g = session.game;
            g.cur = 1;
            g.p[0].loc = Game.HUBER; g.p[0].hp = 6; g.p[0].vp = 12;
            if (auto.equals("yield")) { g.phase = Game.Phase.YIELD; g.askYield[0] = true; }
            if (auto.equals("power")) { g.cur = 0; g.phase = Game.Phase.POWER; g.offer[0] = Card.SHIP_IT.ordinal(); g.offer[1] = Card.SPREADSHEET.ordinal(); }
            if (auto.equals("over")) {
                g.phase = Game.Phase.OVER; g.winner = 0; g.p[0].vp = 21;
                int[][] st = {{4, 3, 1, 3, 9, 7, 5, 3, 1, 0}, {1, 1, 4, 2, 6, 12, 8, 1, 0, 0}, {2, 2, 2, 5, 11, 9, 2, 4, 1, 3}, {0, 0, 0, 1, 2, 10, 1, 0, 0, 6}};
                for (int i = 0; i < g.p.length; i++) {
                    Game.P q = g.p[i]; int[] v = st[i % 4];
                    q.sHuberTurns = v[0]; q.sMishaps = v[1]; q.sFled = v[2]; q.sBigZap = v[3]; q.sDealt = v[4]; q.sTaken = v[5]; q.sHealed = v[6]; q.sBought = v[7]; q.sKOs = v[8]; q.sMoans = v[9];
                }
            }
            session.events.clear();
            screen = Screen.GAME;
        } else if (auto.equals("lobby")) {
            try { session = Session.host(name.toString(), Net.PORT); session.pick(0); screen = Screen.GAME; } catch (Exception e) { notice = e.toString(); }
        } else if (auto.equals("help")) screen = Screen.HELP;
        String join = System.getProperty("join");
        if (join != null) { address.setLength(0); address.append(join); connect(); }
    }

    private void findIps() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address) myIps.add(a.getHostAddress());
            }
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ input

    private void typed(char c) {
        if (swallowTyped) { swallowTyped = false; return; }
        StringBuilder f = focus == 1 ? name : focus == 2 ? address : focus == 3 ? chatMsg : null;
        if (f == null) return;
        if (c == '\b') { if (f.length() > 0) f.setLength(f.length() - 1); }
        else if (c >= 32 && c < 127 && f.length() < (focus == 1 ? 16 : focus == 2 ? 40 : 120)) f.append(c);
    }

    private boolean online() { return session != null && session.role != Session.Role.LOCAL; }

    private boolean keyPressed(int key) {
        if (focus == 3) {   // chat box
            if (key == Input.Keys.ENTER) { if (session != null) session.sendChat(chatMsg.toString()); chatMsg.setLength(0); focus = 0; }
            if (key == Input.Keys.ESCAPE) { chatMsg.setLength(0); focus = 0; }
            return true;
        }
        if (key == Input.Keys.T && focus == 0 && screen == Screen.GAME && online()) {
            focus = 3; swallowTyped = true; chatMsg.setLength(0);
            return true;
        }
        if (key == Input.Keys.ESCAPE) {
            if (screen == Screen.GAME && session != null && session.game != null) paused = !paused;
            else if (screen == Screen.GAME) leave(null);
            else if (screen != Screen.TITLE) screen = Screen.TITLE;
            return true;
        }
        if (focus != 0) {
            if (key == Input.Keys.ENTER || key == Input.Keys.TAB) focus = 0;
            return true;
        }
        if (key == Input.Keys.M) { muted = !muted; prefs.putBoolean("muted", muted).flush(); }
        if (key == Input.Keys.N) { musicOn = !musicOn; prefs.putBoolean("music", musicOn).flush(); }
        if (session != null && session.game != null && screen == Screen.GAME && !paused) {
            Game g = session.game;
            if (session.myTurn()) {
                if (key == Input.Keys.SPACE && g.phase == Game.Phase.ROLL && g.rollsLeft > 0) session.act(Game.A_ROLL, 0);
                if (key == Input.Keys.ENTER && g.phase == Game.Phase.ROLL && g.rolled) session.act(Game.A_DONE, 0);
                else if (key == Input.Keys.ENTER && g.phase == Game.Phase.BUY) session.act(Game.A_END, 0);
                if (key >= Input.Keys.NUM_1 && key <= Input.Keys.NUM_8 && g.phase == Game.Phase.ROLL) session.act(Game.A_TOGGLE, key - Input.Keys.NUM_1);
            }
            if (key == Input.Keys.F && session.canControl()) session.toggleFastBots();
        }
        return true;
    }

    private void play(String s) {
        if (muted) return;
        Float last = sfxCool.get(s);
        if (last != null && time - last < 0.07f) return;
        sfxCool.put(s, time);
        Sound snd = sfx.get(s);
        if (snd != null) snd.play(0.6f, MathUtils.random(0.95f, 1.05f), 0);
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        time += dt;
        if (joined != null) { session = joined; joined = null; joining = false; screen = Screen.GAME; resetTable(); }
        if (joinError != null) { notice = joinError; noticeT = 5; joinError = null; joining = false; }
        if (session != null) {
            session.busy = rollAnim > 0 || (bannerT > 0 && bannerT > bannerMax - 1.1f) || (paused && session.role == Session.Role.LOCAL);
            session.update(dt);
            if (session.closedReason != null) {
                boolean retry = session.lost && session.role == Session.Role.CLIENT && session.game != null && lastHost != null;
                String why = session.closedReason;
                leave(retry ? null : why);
                if (retry) startReconnect();
            }
        }
        if (session != null) { drainEvents(); drainChat(); }
        tick(dt);
        updateMusic();

        ScreenUtils.clear(0.04f, 0.04f, 0.07f, 1);
        float sx = screenShake > 0 ? MathUtils.random(-1f, 1f) * screenShake * 14 : 0, sy = screenShake > 0 ? MathUtils.random(-1f, 1f) * screenShake * 10 : 0;
        ui.vp.getCamera().position.set(Ui.W / 2 + sx, Ui.H / 2 + sy, 0);
        ui.vp.getCamera().update();
        ui.begin(click);
        click = false;
        switch (screen) {
            case TITLE: drawTitle(); break;
            case JOIN: drawJoin(); break;
            case HELP: drawHelp(); break;
            case RECONNECT: drawReconnect(); break;
            case GAME:
                if (session == null) screen = Screen.TITLE;
                else if (session.game == null) drawLobby();
                else drawTable();
                break;
        }
        if (notice != null && noticeT > 0) {
            float w = Math.min(900, ui.width(ui.bold, notice) + 60);
            ui.shadowPanel(640 - w / 2, 650, w, 50, new Color(0.6f, 0.1f, 0.1f, 0.97f));
            ui.textC(ui.bold, notice, 640, 684, Color.WHITE);
        }
        if (ui.tip != null) drawTip(ui.tip);
        ui.end();
        screenshot();
    }

    private void tick(float dt) {
        noticeT -= dt;
        rollAnim = Math.max(0, rollAnim - dt);
        bannerT = Math.max(0, bannerT - dt);
        for (int i = 0; i < 6; i++) { shake[i] = Math.max(0, shake[i] - dt); bubbleT[i] = Math.max(0, bubbleT[i] - dt); }
        for (int i = floaters.size - 1; i >= 0; i--) {
            Floater f = floaters.get(i);
            if (f.delay > 0) f.delay -= dt; else f.t += dt;
            if (f.t > 1.6f) floaters.removeIndex(i);
        }
        screenShake = Math.max(0, screenShake - dt * 2.2f);
        for (int i = flights.size - 1; i >= 0; i--) {
            Flight f = flights.get(i);
            if (f.delay > 0) { f.delay -= dt; continue; }
            f.t += dt;
            if (f.t >= f.dur) {
                if (f.enter) { puff(f.x1, f.y1 - f.s1 * 0.4f, 10); screenShake = Math.max(screenShake, 0.25f); }
                flights.removeIndex(i);
            }
        }
        for (int i = bolts.size - 1; i >= 0; i--) {
            Bolt b = bolts.get(i);
            if (b.delay > 0) b.delay -= dt; else b.t += dt;
            if (b.t > 0.42f) bolts.removeIndex(i);
        }
        for (int i = parts.size - 1; i >= 0; i--) {
            Part q = parts.get(i);
            if (q.delay > 0) { q.delay -= dt; continue; }
            q.t += dt;
            if (!q.homing) { q.x += q.vx * dt; q.y += q.vy * dt; q.vy -= 380 * dt; q.rot += q.spin * dt; }
            if (q.t >= q.life) parts.removeIndex(i);
        }
    }

    private void updateMusic() {
        Music want = (session != null && session.game != null && screen == Screen.GAME) ? gameMusic : lobbyMusic;
        boolean on = musicOn && !muted;
        if (playing != want || !on) {
            if (playing != null) playing.stop();
            playing = null;
            if (on) { want.setVolume(want == gameMusic ? 0.32f : 0.4f); want.play(); playing = want; }
        }
    }

    // ------------------------------------------------------------------ positions for effects

    /** Centre and size of a player's portrait on their card. */
    private float[] seatPos(Game g, int i) {
        float[] r = panelRect(i, g.p.length);
        float ps = Math.min(72, r[2] * 0.36f);
        return new float[]{r[0] + 6 + ps / 2, r[1] + r[3] - ps / 2 - 8, ps};
    }

    /** Centre and size of the Huber spot (loc 1) or the Rose lot (loc 2). */
    private float[] spotPos(Game g, int loc) {
        float x = 396, y = 232, w = 428;
        boolean rose = g.roseOpen();
        if (loc == Game.ROSE) return new float[]{x + w * 0.8f, y + 170, 90};
        return new float[]{rose ? x + w * 0.36f : x + w / 2, y + 150, rose ? 128 : 160};
    }

    /** Where a seat "is" right now: in its Huber spot if it's there, otherwise its card. */
    private float[] where(Game g, int i) {
        if (i >= 0 && i < g.p.length && g.p[i].alive && g.p[i].loc != Game.SUBURBS) return spotPos(g, g.p[i].loc);
        return seatPos(g, i);
    }

    private boolean flyingIn(int seat) {
        for (Flight f : flights) if (f.seat == seat && f.enter) return true;
        return false;
    }

    private void puff(float x, float y, int n) {
        for (int k = 0; k < n; k++) {
            Part q = new Part();
            q.img = "img/circle.png";
            q.x = x; q.y = y;
            q.vx = MathUtils.random(-160f, 160f); q.vy = MathUtils.random(40f, 200f);
            q.size = MathUtils.random(10f, 22f);
            q.life = MathUtils.random(0.4f, 0.7f);
            q.color = new Color(0.85f, 0.75f, 0.65f, 1);
            parts.add(q);
        }
    }

    /** Icons that fly from the dice tray to a player's card. */
    private void stream(Game g, int seat, String img, int n, float delay) {
        float[] to = seatPos(g, seat);
        for (int k = 0; k < Math.min(n, 8); k++) {
            Part q = new Part();
            q.img = img; q.homing = true;
            q.x = 60 + MathUtils.random(0, 480); q.y = 80;
            q.tx = to[0]; q.ty = to[1];
            q.size = 30; q.life = 0.75f; q.delay = delay + k * 0.07f;
            parts.add(q);
        }
    }

    private void screenshot() {
        if (shotsDir == null || shotN >= shotAt.size() || time < shotAt.get(shotN)) return;
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Pixmap flipped = new Pixmap(pm.getWidth(), pm.getHeight(), pm.getFormat());
        for (int y = 0; y < pm.getHeight(); y++) flipped.drawPixmap(pm, 0, y, pm.getWidth(), 1, 0, pm.getHeight() - 1 - y, pm.getWidth(), 1);
        PixmapIO.writePNG(Gdx.files.absolute(shotsDir + "/shot" + shotN + ".png"), flipped);
        pm.dispose(); flipped.dispose();
        shotN++;
        if (shotN >= shotAt.size()) Gdx.app.exit();
    }

    private void leave(String why) {
        if (session != null) session.close();
        session = null;
        paused = false;
        screen = Screen.TITLE;
        if (why != null) { notice = why; noticeT = 5; }
    }

    private void resetTable() {
        floaters.clear(); log.clear(); flights.clear(); bolts.clear(); parts.clear();
        recordedFor = null;
        bannerT = 0; rollAnim = 0;
        for (int i = 0; i < 6; i++) { bubbleT[i] = 0; shake[i] = 0; }
    }

    // ------------------------------------------------------------------ events -> effects

    private void drainEvents() {
        batchDelay = 0;
        Game g = session.game;
        for (Game.Ev e; (e = session.events.poll()) != null; ) {
            switch (e.type) {
                case Game.E_LOG: addLog(e.text); break;
                case Game.E_ROLL: rollAnim = 0.5f; play("roll"); break;
                case Game.E_DMG: shake[e.a] = 0.45f; floater(e.a, "-" + e.b, RED, "img/icon_heart.png"); play("hit"); if (e.b >= 3) screenShake = Math.max(screenShake, 0.6f); break;
                case Game.E_HEAL:
                    floater(e.a, "+" + e.b, GREEN, "img/icon_heart.png"); play("heal");
                    if (g != null && "hearts".equals(e.text)) stream(g, e.a, "img/icon_heart.png", e.b, 0.1f);
                    break;
                case Game.E_ZAP:
                    if (g != null) { Bolt b = new Bolt(); b.from = e.a; b.to = e.b; b.amt = e.c; b.delay = Math.max(0, batchDelay - 0.3f); bolts.add(b); }
                    play("zap");
                    break;
                case Game.E_EVENT:
                    setBanner("HUBER-WIDE EVENT: " + Game.EVENTS[e.b][0], e.text, new Color(0.12f, 0.35f, 0.6f, 0.97f), 4.4f);
                    bannerImg = "img/event_" + e.b + ".png";
                    play("mishap");
                    break;
                case Game.E_VP: floater(e.a, (e.b > 0 ? "+" : "") + e.b, e.b > 0 ? GOLD : GREY, "img/icon_star.png"); if (e.b > 0) play("star"); break;
                case Game.E_BAT:
                    floater(e.a, (e.b > 0 ? "+" : "") + e.b, e.b > 0 ? GREEN : GREY, "img/icon_battery.png");
                    if (e.b > 0) play("battery");
                    if (g != null && e.b > 0 && "rolled".equals(e.text)) stream(g, e.a, "img/icon_battery.png", e.b, 0.15f);
                    break;
                case Game.E_ENTER:
                    if (g != null) {
                        Flight f = new Flight();
                        float[] a = seatPos(g, e.a), b = spotPos(g, e.b);
                        f.seat = e.a; f.enter = true; f.x0 = a[0]; f.y0 = a[1]; f.s0 = a[2]; f.x1 = b[0]; f.y1 = b[1]; f.s1 = b[2];
                        f.delay = batchDelay * 0.5f;
                        flights.add(f);
                    }
                    play("enter");
                    break;
                case Game.E_LEAVE:
                    if (g != null) {
                        Flight f = new Flight();
                        float[] a = spotPos(g, e.b), b = seatPos(g, e.a);
                        f.seat = e.a; f.enter = false; f.x0 = a[0]; f.y0 = a[1]; f.s0 = a[2]; f.x1 = b[0]; f.y1 = b[1]; f.s1 = b[2]; f.dur = 0.6f;
                        flights.add(f);
                    }
                    play("leave");
                    break;
                case Game.E_KO:
                    if (g != null) {
                        float[] a = seatPos(g, e.a);
                        Part q = new Part();
                        q.img = "img/icon_ghost.png"; q.x = a[0]; q.y = a[1]; q.size = 70; q.life = 2.4f; q.delay = 0.3f; q.text = "ghost";
                        parts.add(q);
                        screenShake = Math.max(screenShake, 0.7f);
                    }
                    if (g != null) setBanner("KNOCKED OUT", g.p[e.a].name + " is out. They'll haunt the place as a ghost.", new Color(0.3f, 0.3f, 0.35f, 0.97f), 3f);
                    play("ko");
                    break;
                case Game.E_WIN: play("win"); break;
                case Game.E_BUY:
                    if (g != null) {
                        float[] to = seatPos(g, e.a);
                        Part q = new Part();
                        q.img = "card"; q.homing = true; q.text = Card.ALL[e.b].title;
                        q.color = Card.ALL[e.b].isPower() ? PURPLE : Card.ALL[e.b].keep ? BLUE : ORANGE;
                        q.x = 1052; q.y = 400; q.tx = to[0]; q.ty = to[1]; q.size = 1; q.life = 0.8f;
                        parts.add(q);
                    }
                    play("buy");
                    break;
                case Game.E_SAY: if (e.a >= 0 && e.a < 6) { bubble[e.a] = e.text; bubbleT[e.a] = 3.4f; } break;
                case Game.E_MISHAP: setBanner("ONLY IN HUBER", e.text, BRICK, 4.2f); bannerImg = "img/mishap_" + e.b + ".png"; play("mishap"); break;
                case Game.E_POWER: setBanner("NEW POWER: " + Card.ALL[e.b].title.toUpperCase(), Card.ALL[e.b].text, PURPLE, 3f); play("power"); break;
                case Game.E_GHOST: setBanner("GHOST TURN", e.text, new Color(0.35f, 0.4f, 0.55f, 0.97f), 2.6f); play("ghost"); break;
                case Game.E_TURN: if (g != null && e.a == session.mySeat && g.p[e.a].alive) play("turn"); break;
                case Game.E_SWEEP: play("sweep"); break;
                default: break;
            }
        }
    }

    private void addLog(String s) {
        log.addLast(s);
        while (log.size() > 60) log.removeFirst();
    }

    private void floater(int seat, String text, Color c, String icon) {
        if (seat < 0) return;
        Floater f = new Floater();
        f.seat = seat; f.text = text; f.color = c; f.icon = icon; f.delay = batchDelay;
        batchDelay += 0.38f;
        floaters.add(f);
    }

    private void setBanner(String title, String sub, Color c, float t) {
        banner = title; bannerSub = sub; bannerColor = c; bannerT = t; bannerMax = t; bannerImg = null;
    }

    private void drainChat() {
        Game g = session.game;
        for (String[] c; (c = session.chat.poll()) != null; ) {
            int seat = Integer.parseInt(c[0]);
            lobbyChat.add(c);
            while (lobbyChat.size() > 6) lobbyChat.removeFirst();
            if (seat < 0) { notice = c[2]; noticeT = 4; addLog(c[2]); play("turn"); continue; }
            addLog(c[1] + ": " + c[2]);
            if (g != null && seat < g.p.length && seat < 6) { bubble[seat] = c[2]; bubbleT[seat] = 4.5f; }
            play("click");
        }
    }

    // ------------------------------------------------------------------ reconnecting

    private void startReconnect() {
        screen = Screen.RECONNECT;
        reconnectTry = 1;
        reconnectMsg = null;
        final String host = lastHost, nm = name.toString(), key = clientKey();
        final int port = lastPort;
        final boolean tls = lastTls;
        new Thread(() -> {
            for (int k = 1; k <= 8; k++) {
                reconnectTry = k;
                try { Thread.sleep(k == 1 ? 1000 : 2500); } catch (InterruptedException ignored) {}
                if (screen != Screen.RECONNECT) return;
                try { joined = Session.join(host, port, tls, nm, key); return; } catch (Exception ignored) {}
            }
            reconnectTry = 0;
            joinError = "Couldn't get back into the game. The server may be down.";
        }, "reconnect").start();
    }

    private void drawReconnect() {
        backdrop(0.5f);
        ui.shadowPanel(390, 270, 500, 180, INK);
        ui.textC(ui.big, "Connection dropped", 640, 420, Color.WHITE);
        ui.textC(ui.body, reconnectTry > 0 ? "Getting your seat back... (try " + reconnectTry + " of 8)" : "Couldn't reconnect.", 640, 370, SOFT);
        if (ui.button("Give up", 540, 290, 200, 46, RED, true)) { screen = Screen.TITLE; reconnectTry = 0; }
        if (reconnectTry == 0 && screen == Screen.RECONNECT) screen = Screen.TITLE;
    }

    // ------------------------------------------------------------------ title

    private void backdrop(float dim) {
        ui.img("img/backdrop.png", 0, 0, Ui.W, Ui.H);
        if (dim > 0) ui.rect(0, 0, Ui.W, Ui.H, new Color(0, 0, 0, dim));
    }

    private void drawTitle() {
        backdrop(0.25f);
        float bob = MathUtils.sin(time * 1.6f) * 4;
        ui.img("img/logo.png", 640 - 440, 470 + bob, 880, 208);
        ui.shadowPanel(390, 70, 500, 390, INK);
        ui.text(ui.bold, "Your name", 420, 440, SOFT);
        field(420, 368, 440, name, 1);
        float y = 300;
        if (ui.button("Play vs Bots", 420, y, 440, 52, GREEN, true)) startLocal();
        if (ui.button("Host Online Game", 420, y - 62, 440, 52, BLUE, true)) startHost();
        if (ui.button("Join Online", 420, y - 124, 215, 52, PURPLE, true)) { saveName(); screen = Screen.JOIN; }
        if (ui.button("How to Play", 645, y - 124, 215, 52, ORANGE, true)) screen = Screen.HELP;
        if (ui.button("Quit", 420, y - 186, 440, 40, new Color(0.3f, 0.32f, 0.38f, 1), true)) Gdx.app.exit();
        ui.textC(ui.small, "A game about Huber Heights, Ohio. Not affiliated with Huber Heights. Sorry, Huber.   M = mute", 640, 38, GREY);
    }

    private void field(float x, float y, float w, StringBuilder sb, int id) {
        boolean on = focus == id;
        ui.panel(x, y, w, 46, on ? new Color(1, 1, 1, 1) : new Color(0.85f, 0.86f, 0.9f, 1));
        String caret = on && ((int) (time * 2) % 2 == 0) ? "|" : "";
        ui.text(ui.bold, sb + caret, x + 14, y + 33, new Color(0.1f, 0.1f, 0.15f, 1));
        if (ui.clicked) { if (ui.hover(x, y, w, 46)) focus = id; else if (focus == id) focus = 0; }
    }

    /** A random id this computer keeps, so the server can give you your seat back after a dropped connection. */
    private String clientKey() {
        String k = prefs.getString("clientKey", "");
        if (k.isEmpty()) { k = java.util.UUID.randomUUID().toString(); prefs.putString("clientKey", k).flush(); }
        return k;
    }

    private void saveName() {
        if (name.toString().isBlank()) { name.setLength(0); name.append("Player"); }
        prefs.putString("name", name.toString().trim()).putString("address", address.toString().trim()).flush();
        focus = 0;
    }

    private void startLocal() {
        saveName();
        session = Session.local(name.toString());
        resetTable();
        screen = Screen.GAME;
    }

    private void startHost() {
        saveName();
        try {
            session = Session.host(name.toString(), Net.PORT);
            resetTable();
            screen = Screen.GAME;
        } catch (Exception e) {
            notice = "Couldn't open port " + Net.PORT + ". Is another copy already hosting?";
            noticeT = 5;
        }
    }

    private void drawJoin() {
        backdrop(0.35f);
        ui.shadowPanel(340, 170, 600, 380, INK);
        ui.textC(ui.big, "Join Online", 640, 515, Color.WHITE);
        ui.textW(ui.body, "Type the server's address, like hubergeddon-sam.fly.dev, or a friend's address from their lobby: 192.168.1.23 on the same Wi-Fi, a Tailscale 100.x address, or their public IP if they forwarded port 7777. A sleeping server takes a few seconds to wake up.",
            380, 470, 520, Align.left, SOFT);
        ui.text(ui.bold, "Host address", 380, 360, SOFT);
        field(380, 290, 520, address, 2);
        boolean can = !joining && address.toString().trim().length() > 0;
        if (ui.button(joining ? "Connecting..." : "Connect", 380, 210, 330, 56, GREEN, can)) connect();
        if (ui.button("Back", 720, 210, 180, 56, new Color(0.3f, 0.32f, 0.38f, 1), !joining)) screen = Screen.TITLE;
    }

    private void connect() {
        saveName();
        joining = true;
        String a = address.toString().trim();
        // tls://host forces TLS, tcp://host forces plain. Fly.io addresses use TLS automatically.
        boolean tls = false, forced = false;
        if (a.startsWith("tls://")) { tls = true; forced = true; a = a.substring(6); }
        else if (a.startsWith("tcp://")) { forced = true; a = a.substring(6); }
        if (!forced && (a.endsWith(".fly.dev") || a.contains(".fly.dev:"))) tls = true;
        int port = Net.PORT;
        int colon = a.lastIndexOf(':');
        if (colon > 0) { try { port = Integer.parseInt(a.substring(colon + 1)); a = a.substring(0, colon); } catch (Exception ignored) {} }
        final String host = a;
        final int p = port;
        final boolean useTls = tls;
        lastHost = a; lastPort = port; lastTls = tls;
        final String nm = name.toString();
        new Thread(() -> {
            try { joined = Session.join(host, p, useTls, nm, clientKey()); }
            catch (Exception e) { joinError = "Couldn't reach " + host + ":" + p + ". Check the address and that they're hosting."; }
        }, "join").start();
    }

    private void drawHelp() {
        backdrop(0.55f);
        ui.shadowPanel(60, 40, 1160, 640, INK);
        ui.textC(ui.big, "How to Play", 640, 660, GOLD);
        String left =
            "GOAL: Get 20 VP (or whatever the lobby set), or be the last one standing.\n\n" +
            "YOUR TURN: Roll your dice up to 3 times. Click dice to keep them between rolls (or press 1-8). Space rolls, Enter stops.\n\n" +
            "THE DICE\n" +
            "1, 2, 3: Three of a kind scores that many VP. Each extra matching die is +1.\n" +
            "Zap: 1 damage each. From the Suburbs you hit whoever is in Huber. From Huber you hit everyone in the Suburbs.\n" +
            "Heart: heal 1. Hearts DON'T heal you in Huber. Three hearts unlocks one of your character's personal powers.\n" +
            "Battery: money for the shop.\n\n" +
            "THE SHOP: KEEP cards stay with you. ONE-SHOT cards happen once. Sweep (2) swaps all three.";
        String right =
            "HUBER HEIGHTS\n" +
            "If Huber is empty when you finish rolling, you move in (+1 VP).\n" +
            "Every turn you start in Huber: +2 VP, then an ONLY IN HUBER mishap.\n" +
            "Zapped while in Huber? You may flee. Whoever zapped you moves in.\n" +
            "5-6 players: the Rose Music Center lot is a second Huber spot.\n\n" +
            "HUBER-WIDE EVENTS hit the whole table every 3 rounds: council meetings, snow emergencies, another car wash...\n\n" +
            "KNOCKED OUT? You become a ghost and haunt the table each turn.\n\n" +
            "ONLINE: T to chat. If your connection drops, the game reconnects you to your seat. Turn timers hand idle players to autopilot.\n\n" +
            "KEYS: M mute all, N music on/off, F fast bots, Esc pause.";
        ui.textW(ui.body, left, 100, 610, 520, Align.left, SOFT);
        ui.textW(ui.body, right, 660, 610, 520, Align.left, SOFT);
        if (ui.button("Back", 540, 60, 200, 50, ORANGE, true)) screen = Screen.TITLE;
    }

    // ------------------------------------------------------------------ lobby

    private void drawLobby() {
        Session s = session;
        backdrop(0.45f);
        ui.shadowPanel(40, 20, 1200, 680, INK);
        String title = s.role == Session.Role.LOCAL ? "Set up a game vs bots" : s.role == Session.Role.HOST ? "Hosting a game" : s.onServer() ? "Online lobby" : "Joined a game";
        ui.text(ui.big, title, 70, 685, Color.WHITE);
        if (s.role == Session.Role.HOST) {
            StringBuilder ips = new StringBuilder();
            for (String ip : myIps) {
                if (ips.length() > 0) ips.append("   ");
                ips.append(ip).append(ip.startsWith("100.") ? " (Tailscale?)" : "");
            }
            ui.textW(ui.small, "Friends join with your address: " + (ips.length() == 0 ? "(no network found)" : ips.toString()) + "   port " + Net.PORT +
                ".  Same Wi-Fi: the 192.168... one. Over the internet: Tailscale, or forward TCP port 7777.", 70, 640, 1140, Align.left, SOFT);
        } else if (s.role == Session.Role.CLIENT) {
            String leader = "the host";
            for (int i = 0; i < s.seats.size(); i++) if (s.isLeaderSeat(i)) leader = s.seats.get(i).name;
            ui.text(ui.body, s.canControl() ? "You're the lobby leader. Set the options, add bots, then press START when everyone's in."
                : "Pick your character. Waiting for " + leader + " to start.", 70, 642, SOFT);
        }

        // seats
        int mine = s.myLobbySeat();
        ui.text(ui.bold, "Players (" + s.seats.size() + "/6)", 70, 595, GOLD);
        for (int i = 0; i < s.seats.size(); i++) {
            Net.Seat seat = s.seats.get(i);
            float y = 508 - i * 64;
            ui.panel(70, y, 520, 58, i == mine ? new Color(0.2f, 0.25f, 0.4f, 1) : INK2);
            if (seat.who >= 0) ui.img(Cast.ALL[seat.who].portrait(), 76, y + 4, 50, 50);
            else { ui.img("img/circle.png", 76, y + 4, 50, 50, GREY); ui.textC(ui.big, "?", 101, y + 45, INK); }
            String kind = seat.kind == Net.Seat.BOT ? "BOT" : seat.kind == Net.Seat.HOST ? (s.role == Session.Role.LOCAL ? "YOU" : "HOST")
                : s.isLeaderSeat(i) ? "LEADER" : "FRIEND";
            if (i == mine && s.role == Session.Role.CLIENT) kind += " (YOU)";
            ui.text(ui.bold, seat.kind == Net.Seat.BOT ? Cast.ALL[seat.who].name : seat.name, 138, y + 50, Color.WHITE);
            ui.text(ui.small, kind + (seat.kind != Net.Seat.BOT ? "  ·  " + (seat.who >= 0 ? "playing " + Cast.ALL[seat.who].name : "still choosing") : ""), 138, y + 24, GREY);
            if (s.canControl() && seat.kind == Net.Seat.BOT && ui.button("Change", 400, y + 11, 90, 36, PURPLE, true)) s.cycleBot(i);
            if (s.canControl() && s.removable(i) && ui.button("Remove", 498, y + 11, 84, 36, RED, true)) s.removeSeat(i);
        }

        // chat (online only)
        if (online()) {
            float cy = 98;
            ui.panel(70, cy, 520, 74, INK2);
            if (lobbyChat.isEmpty()) ui.text(ui.small, "Press T to chat.", 82, cy + 44, GREY);
            float ly = cy + 66;
            int skip = Math.max(0, lobbyChat.size() - 3), k = 0;
            for (String[] c : lobbyChat) {
                if (k++ < skip) continue;
                String line = c[0].equals("-1") ? c[2] : c[1] + ": " + c[2];
                ui.text(ui.small, clip(ui.small, line, 496), 82, ly, c[0].equals("-1") ? GOLD : Color.WHITE);
                ly -= 21;
            }
        }

        // character picker
        ui.text(ui.bold, "Pick your character" + (s.opts.dupes ? "  (duplicates allowed)" : ""), 630, 595, GOLD);
        for (int c = 0; c < Cast.ALL.length; c++) {
            Cast k = Cast.ALL[c];
            float x = 630 + (c % 4) * 145, y = 420 - (c / 4) * 150;
            boolean takenByOther = s.taken(c, mine);
            boolean isMine = mine >= 0 && mine < s.seats.size() && s.seats.get(mine).who == c;
            boolean over = ui.hover(x, y, 132, 142);
            ui.panel(x, y, 132, 142, isMine ? GOLD : over && !takenByOther ? new Color(0.25f, 0.28f, 0.4f, 1) : INK2);
            ui.img(k.portrait(), x + 24, y + 44, 84, 84, takenByOther ? new Color(0.4f, 0.4f, 0.4f, 1) : Color.WHITE);
            ui.textC(ui.width(ui.bold, k.name) > 124 ? ui.mid : ui.bold, k.name, x + 66, y + 38, isMine ? INK : Color.WHITE);
            ui.textC(ui.tiny, takenByOther ? "taken" : k.human ? "townie" : "alien", x + 66, y + 16, isMine ? INK : GREY);
            if (over) ui.tip = k.name + ": " + k.tagline + "\nPowers: " + powersOf(c);
            if (over && ui.clicked && !takenByOther) s.pick(c);
        }

        // game options
        ui.text(ui.bold, "Game options", 630, 262, GOLD);
        boolean ctl = s.canControl();
        for (int o = 0; o < Session.OPT_NAMES.length; o++) {
            float x = 630 + (o % 2) * 292, y = 214 - (o / 2) * 40;
            String label = Session.OPT_NAMES[o] + ": " + s.optValue(o);
            boolean over = ui.hover(x, y, 284, 34);
            ui.panel(x, y, 284, 34, ctl && over ? new Color(0.3f, 0.33f, 0.48f, 1) : INK2);
            ui.text(ui.small, clip(ui.small, label, 266), x + 10, y + 24, ctl ? Color.WHITE : SOFT);
            if (over) ui.tip = optTip(o) + (ctl ? "\nClick to change." : "\nOnly the host or lobby leader can change this.");
            if (ctl && over && ui.clicked) s.cycleOpt(o);
        }

        // bottom buttons
        if (s.canControl()) {
            if (ui.button("+ Add Bot", 70, 36, 160, 48, BLUE, s.seats.size() < 6)) s.addBot();
            if (ui.button("START", 1000, 36, 210, 52, GREEN, s.canStart())) { s.start(); resetTable(); }
        }
        if (ui.button("Leave", 240, 36, 120, 48, RED, true)) leave(null);
        if (focus == 3) drawChatBox(380, 36, 600);
    }

    private String optTip(int o) {
        switch (o) {
            case 0: return "How many VP wins. 15 is a quick game, 25 a long one.";
            case 1: return "NOT ALLOWED: nobody can flee Huber when zapped. Your lease runs out after one wake-up instead.";
            case 2: return "Power Draft: everyone picks a personal power before their first roll.";
            case 3: return "Huber-wide events hit the whole table every 3 rounds.";
            case 4: return "How well the bots play. Easy bots make mistakes. Uncle Rob After Three Beers plays sharp and talks loud.";
            case 5: return "Online games: how long someone can take before autopilot finishes their turn.";
            case 6: return "Let two players pick the same character (two Uncle Robs, say).";
            case 7: return "Bots take their turns much faster.";
            default: return "";
        }
    }

    private String powersOf(int who) {
        StringBuilder b = new StringBuilder();
        for (Card c : Card.ALL) if (c.owner == who) { if (b.length() > 0) b.append(", "); b.append(c.title); }
        return b.toString();
    }

    // ------------------------------------------------------------------ table

    private float[] panelRect(int i, int n) {
        float gap = 8, w = Math.min(240, (Ui.W - 32 - gap * (n - 1)) / n);
        float total = n * w + (n - 1) * gap;
        float x = (Ui.W - total) / 2 + i * (w + gap);
        return new float[]{x, 600, w, 112};
    }

    private void drawTable() {
        Game g = session.game;
        backdrop(0.1f);
        for (int i = 0; i < g.p.length; i++) drawPanel(g, i);
        drawLog(16, 232, 368, 352);
        drawHuber(g, 396, 232, 428, 352);
        drawShop(g, 840, 232, 424, 352);
        drawTray(g, 16, 14, 808, 206);
        drawMine(g, 840, 14, 424, 206);
        drawEffects(g);
        drawFloaters(g);
        drawBubbles(g);
        if (bannerT > 0) drawBanner();
        if (g.phase == Game.Phase.OVER) drawGameOver(g);
        else if (session.myTurn() && g.phase == Game.Phase.YIELD) drawYield(g);
        else if (session.myTurn() && g.phase == Game.Phase.POWER) drawPower(g);
        if (paused) drawPause();
        if (focus == 3) drawChatBox(16, 228, 808);
    }

    private void drawChatBox(float x, float y, float w) {
        ui.shadowPanel(x, y, w, 46, new Color(0.1f, 0.2f, 0.35f, 0.98f));
        String caret = ((int) (time * 2) % 2 == 0) ? "|" : "";
        ui.text(ui.bold, "Say: ", x + 12, y + 33, GOLD);
        ui.text(ui.body, chatMsg + caret, x + 64, y + 32, Color.WHITE);
        ui.text(ui.tiny, "Enter to send, Esc to cancel", x + w - 170, y + 30, GREY);
    }

    private void drawEffects(Game g) {
        // portraits flying in and out of Huber
        for (Flight f : flights) {
            if (f.delay > 0 || f.seat >= g.p.length) continue;
            float u = Math.min(1, f.t / f.dur);
            float e = f.enter ? 1 - (1 - u) * (1 - u) : u * u;
            float x = MathUtils.lerp(f.x0, f.x1, e), y = MathUtils.lerp(f.y0, f.y1, e) + MathUtils.sin(u * MathUtils.PI) * (f.enter ? 120 : 70);
            float size = MathUtils.lerp(f.s0, f.s1, e);
            float sxs = 1, sys = 1;
            if (f.enter && u > 0.82f) { float k = (u - 0.82f) / 0.18f; sxs = 1 + 0.18f * MathUtils.sin(k * MathUtils.PI); sys = 1 - 0.15f * MathUtils.sin(k * MathUtils.PI); }
            float rot = f.enter ? 0 : u * 540;
            ui.img("img/circle.png", x - size * 0.45f, y - size * 0.62f - MathUtils.sin(u * MathUtils.PI) * (f.enter ? 120 : 70), size * 0.9f, size * 0.2f, new Color(0, 0, 0, 0.3f));
            if (rot != 0) ui.imgRot(g.p[f.seat].cast().portrait(), x, y, size, rot, Color.WHITE);
            else ui.img(g.p[f.seat].cast().portrait(), x - size * sxs / 2, y - size * sys / 2, size * sxs, size * sys);
            if (!f.enter) ui.textC(ui.bold, "RUN!", x, y + size / 2 + 22, Color.WHITE);
        }
        // zap bolts
        for (Bolt b : bolts) {
            if (b.delay > 0 || b.from >= g.p.length || b.to >= g.p.length) continue;
            float[] a = where(g, b.from), c = where(g, b.to);
            float alpha = 1 - b.t / 0.42f;
            int segs = 9;
            float px = a[0], py = a[1];
            float nx = -(c[1] - a[1]), ny = c[0] - a[0];
            float len = (float) Math.sqrt(nx * nx + ny * ny) + 0.001f;
            nx /= len; ny /= len;
            for (int k = 1; k <= segs; k++) {
                float u = k / (float) segs;
                float j = k == segs ? 0 : MathUtils.random(-22f, 22f);
                float qx = MathUtils.lerp(a[0], c[0], u) + nx * j, qy = MathUtils.lerp(a[1], c[1], u) + ny * j;
                ui.line(px, py, qx, qy, 12 + b.amt * 2, new Color(1f, 0.85f, 0.1f, 0.35f * alpha));
                ui.line(px, py, qx, qy, 3.5f, new Color(1, 1, 0.9f, alpha));
                px = qx; py = qy;
            }
            ui.img("img/icon_zap.png", c[0] - 30, c[1] - 30, 60, 60, new Color(1, 1, 1, alpha));
        }
        // icons, puffs, flying cards, rising ghosts
        for (Part q : parts) {
            if (q.delay > 0) continue;
            float u = q.t / q.life;
            if (q.img.equals("card")) {
                float e = 1 - (1 - u) * (1 - u);
                float x = MathUtils.lerp(q.x, q.tx, e), y = MathUtils.lerp(q.y, q.ty, e) + MathUtils.sin(u * MathUtils.PI) * 60;
                float sc = MathUtils.lerp(1f, 0.35f, e);
                float w = 120 * sc, h = 160 * sc;
                ui.panel(x - w / 2, y - h / 2, w, h, new Color(0.98f, 0.96f, 0.9f, 1));
                ui.panel(x - w / 2, y + h / 2 - 24 * sc, w, 24 * sc, q.color);
                if (sc > 0.6f) ui.textW(ui.mid, q.text, x - w / 2 + 8, y + h / 2 - 30 * sc, w - 16, Align.left, new Color(0.1f, 0.1f, 0.15f, 1));
            } else if ("ghost".equals(q.text)) {
                float a = u < 0.8f ? 1 : 1 - (u - 0.8f) / 0.2f;
                ui.img(q.img, q.x - q.size / 2 + MathUtils.sin(q.t * 6) * 18, q.y - q.size / 2 + u * 170, q.size, q.size, new Color(1, 1, 1, a));
            } else if (q.homing) {
                float e = u * u;
                float x = MathUtils.lerp(q.x, q.tx, e), y = MathUtils.lerp(q.y, q.ty, e) + MathUtils.sin(u * MathUtils.PI) * 140;
                ui.img(q.img, x - q.size / 2, y - q.size / 2, q.size, q.size);
            } else {
                Color c = q.color.cpy();
                c.a = 0.8f * (1 - u);
                ui.img(q.img, q.x - q.size / 2, q.y - q.size / 2, q.size, q.size, c);
            }
        }
    }

    private void drawPanel(Game g, int i) {
        Game.P p = g.p[i];
        float[] r = panelRect(i, g.p.length);
        float sx = shake[i] > 0 ? MathUtils.sin(time * 70) * shake[i] * 10 : 0;
        float x = r[0] + sx, y = r[1], w = r[2], h = r[3];
        boolean turn = g.cur == i && g.phase != Game.Phase.OVER;
        boolean asked = g.phase == Game.Phase.YIELD && g.askYield[i];
        if (turn || asked) ui.panel(x - 4, y - 4, w + 8, h + 8, asked ? ORANGE : GOLD);
        Color bg = p.loc == Game.HUBER ? BRICK : p.loc == Game.ROSE ? new Color(0.45f, 0.2f, 0.4f, 0.95f) : INK;
        ui.panel(x, y, w, h, i == session.mySeat ? bg.cpy().lerp(BLUE, 0.25f) : bg);
        Color tint = p.alive ? Color.WHITE : new Color(0.45f, 0.45f, 0.5f, 0.8f);
        float ps = Math.min(72, w * 0.36f);
        ui.img(p.cast().portrait(), x + 6, y + h - ps - 8, ps, ps, tint);
        if (!p.alive) ui.img("img/icon_ghost.png", x + 6 + ps * 0.45f, y + h - ps - 6, ps * 0.6f, ps * 0.6f);
        if (p.loc != Game.SUBURBS) ui.img("img/icon_brick.png", x + 4, y + h - ps - 12, 30, 30);
        float tx = x + ps + 12, tw = w - ps - 16;
        String nm = p.name + (i == session.mySeat ? " (you)" : "");
        ui.text(ui.bold, clip(ui.bold, nm, tw), tx, y + h - 8, Color.WHITE);
        float subW = p.cards.length > 0 ? tw - 48 : tw;
        String sub = !p.alive ? "GHOST" : p.loc == Game.HUBER ? "IN HUBER" : p.loc == Game.ROSE ? "ROSE LOT" : (p.name.equals(p.cast().name) ? (p.bot ? "bot" : "") : "as " + p.cast().name);
        ui.text(ui.small, clip(ui.small, sub, subW), tx, y + h - 32, p.loc != Game.SUBURBS ? GOLD : GREY);
        // stats
        float sy = y + 40;
        ui.img("img/icon_heart.png", tx - 2, sy - 2, 20, 20);
        ui.text(ui.bold, String.valueOf(p.hp), tx + 20, sy + 17, p.hp <= 3 && p.alive ? RED : Color.WHITE);
        ui.img("img/icon_star.png", tx + tw * 0.38f, sy - 2, 20, 20);
        ui.text(ui.bold, String.valueOf(p.vp), tx + tw * 0.38f + 22, sy + 17, GOLD);
        ui.img("img/icon_battery.png", tx + tw * 0.74f, sy - 2, 20, 20);
        ui.text(ui.bold, String.valueOf(p.bat), tx + tw * 0.74f + 20, sy + 17, Color.WHITE);
        // bars: health and progress to 20
        ui.rect(x + 8, y + 18, w - 16, 5, new Color(0, 0, 0, 0.5f));
        ui.rect(x + 8, y + 18, (w - 16) * Math.max(0, p.hp) / (float) p.maxHp, 5, RED);
        ui.rect(x + 8, y + 9, w - 16, 5, new Color(0, 0, 0, 0.5f));
        ui.rect(x + 8, y + 9, (w - 16) * Math.min(1, p.vp / 20f), 5, GOLD);
        if (p.cards.length > 0) {
            String cc = p.cards.length + (p.cards.length == 1 ? " card" : " cards");
            ui.text(ui.tiny, cc, x + w - ui.width(ui.tiny, cc) - 8, y + h - 34, SOFT);
        }
        if (ui.hover(x, y, w, h)) {
            StringBuilder t = new StringBuilder(p.name + (p.name.equals(p.cast().name) ? "" : " as " + p.cast().name) + "\n" + p.cast().tagline);
            if (p.bot) t.append("\nBot personality: ").append(Bot.STYLE[p.style]);
            for (int c : p.cards) t.append("\n• ").append(Card.ALL[c].title).append(": ").append(Card.ALL[c].text);
            if (p.cards.length == 0) t.append("\nNo cards yet.");
            ui.tip = t.toString();
        }
    }

    private String clip(com.badlogic.gdx.graphics.g2d.BitmapFont f, String s, float w) {
        if (ui.width(f, s) <= w) return s;
        while (s.length() > 1 && ui.width(f, s + "…") > w) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void drawLog(float x, float y, float w, float h) {
        ui.panel(x, y, w, h, INK);
        ui.text(ui.bold, "What happened", x + 14, y + h - 10, GOLD);
        float ty = y + 14;
        Object[] lines = log.toArray();
        for (int k = lines.length - 1; k >= 0; k--) {
            String s = (String) lines[k];
            float lh = ui.measureW(ui.small, s, w - 28);
            if (ty + lh > y + h - 40) break;
            boolean fresh = k >= lines.length - 2;
            ui.textW(ui.small, s, x + 14, ty + lh, w - 28, Align.left, s.startsWith("ONLY IN HUBER") ? new Color(1f, 0.6f, 0.45f, 1) : fresh ? Color.WHITE : GREY);
            ty += lh + 7;
        }
    }

    private void drawHuber(Game g, float x, float y, float w, float h) {
        ui.panel(x, y, w, h, BRICK);
        // brick texture lines
        for (float yy = y + 12; yy < y + h - 12; yy += 22) ui.rect(x + 10, yy, w - 20, 2, new Color(0, 0, 0, 0.12f));
        ui.img("img/icon_brick.png", x + 14, y + h - 46, 36, 36);
        ui.text(ui.big, "HUBER", x + 56, y + h - 10, Color.WHITE);
        ui.text(ui.small, "+2 VP a turn. No healing. Mishaps.", x + 160, y + h - 20, new Color(1, 0.85f, 0.75f, 1));
        boolean rose = g.roseOpen();
        float hx = rose ? x + w * 0.36f : x + w / 2;
        spot(g, Game.HUBER, hx, y + 150, rose ? 128 : 160, "Huber is empty.\nThe next roller moves in.");
        if (rose) {
            ui.textC(ui.small, "ROSE MUSIC CENTER LOT", x + w * 0.8f, y + h - 60, GOLD);
            spot(g, Game.ROSE, x + w * 0.8f, y + 170, 90, "Lot's empty.");
        }
        if (g.event >= 0 && g.eventRound == g.round)
            ui.textW(ui.tiny, "This round: " + Game.EVENTS[g.event][0], x + 14, y + h - 52, w - 28, Align.left, new Color(0.6f, 0.85f, 1f, 1));
        String status;
        int occ = g.occupant(Game.HUBER);
        if (occ < 0) status = "Nobody's in Huber. Lucky them.";
        else status = g.p[occ].name + " is stuck in Huber with " + g.p[occ].hp + " health.";
        ui.textC(ui.body, status, x + w / 2, y + 34, Color.WHITE);
    }

    private void spot(Game g, int loc, float cx, float cy, float size, String empty) {
        int occ = g.occupant(loc);
        ui.img("img/circle.png", cx - size / 2 - 6, cy - size / 2 - 6, size + 12, size + 12, new Color(0, 0, 0, 0.35f));
        if (occ >= 0 && flyingIn(occ)) {
            ui.textW(ui.small, "Incoming!", cx - size / 2, cy + 10, size, Align.center, GOLD);
        } else if (occ >= 0) {
            float bob = MathUtils.sin(time * 2.2f) * 3;
            ui.img(g.p[occ].cast().portrait(), cx - size / 2, cy - size / 2 + bob, size, size);
            ui.textC(ui.bold, g.p[occ].name, cx, cy - size / 2 - 12, Color.WHITE);
        } else {
            ui.textW(ui.small, empty, cx - size / 2, cy + 18, size, Align.center, new Color(1, 0.85f, 0.75f, 0.9f));
        }
    }

    private void drawShop(Game g, float x, float y, float w, float h) {
        ui.panel(x, y, w, h, INK);
        ui.text(ui.bold, "The Shop", x + 14, y + h - 10, GOLD);
        ui.text(ui.small, "Spend batteries. Hover a card to read it.", x + 120, y + h - 14, GREY);
        boolean canBuy = session.myTurn() && g.phase == Game.Phase.BUY;
        int me = Math.max(0, session.mySeat);
        float cw = 128, ch = 212, gap = (w - 3 * cw) / 4;
        for (int s = 0; s < 3; s++) {
            int cid = g.market[s];
            float cx = x + gap + s * (cw + gap), cy = y + 76;
            if (cid < 0) { ui.panel(cx, cy, cw, ch, INK2); continue; }
            int cost = g.price(me, cid);
            boolean afford = g.p[me].bat >= cost;
            boolean over = ui.hover(cx, cy, cw, ch);
            float lift = canBuy && afford && over ? 6 : 0;
            card(cx, cy + lift, cw, ch, cid, cost, canBuy && !afford);
            if (over) ui.tip = Card.ALL[cid].title + " (" + cost + " batteries, " + (Card.ALL[cid].keep ? "keep" : "one-shot") + ")\n" + Card.ALL[cid].text +
                (canBuy ? (afford ? "\nClick to buy." : "\nYou can't afford it.") : "");
            if (over && ui.clicked && canBuy && afford) session.act(Game.A_BUY, s);
        }
        int sc = g.sweepCost(me);
        if (ui.button("Sweep (" + sc + ")", x + 14, y + 16, 150, 46, PURPLE, canBuy && g.p[me].bat >= sc)) session.act(Game.A_SWEEP, 0);
        if (ui.hover(x + 14, y + 16, 150, 46)) ui.tip = "Pay " + sc + " batteries to throw out all three cards and deal three new ones.";
        if (ui.button("End Turn", x + w - 214, y + 16, 200, 46, GREEN, canBuy)) session.act(Game.A_END, 0);
    }

    private void card(float x, float y, float w, float h, int cid, int cost, boolean dim) {
        Card c = Card.ALL[cid];
        Color top = c.isPower() ? PURPLE : c.keep ? BLUE : ORANGE;
        ui.shadowPanel(x, y, w, h, new Color(0.98f, 0.96f, 0.9f, 1));
        ui.panel(x, y + h - 34, w, 34, top);
        ui.text(ui.small, c.isPower() ? "POWER" : c.keep ? "KEEP" : "ONE-SHOT", x + (c.isPower() ? 10 : 44), y + h - 9, Color.WHITE);
        if (!c.isPower()) {
            ui.img("img/circle.png", x + 4, y + h - 32, 32, 32, new Color(0.1f, 0.1f, 0.15f, 1));
            ui.textC(ui.bold, String.valueOf(cost), x + 20, y + h - 9, GOLD);
        }
        com.badlogic.gdx.graphics.g2d.BitmapFont tf = w >= 180 ? ui.bold : ui.mid;
        float th = ui.textW(tf, c.title, x + 8, y + h - 42, w - 16, Align.left, new Color(0.1f, 0.1f, 0.15f, 1));
        ui.textW(w >= 180 ? ui.body : ui.tiny, c.text, x + 8, y + h - 50 - th, w - 16, Align.left, new Color(0.25f, 0.25f, 0.3f, 1));
        if (dim) ui.panel(x, y, w, h, new Color(0, 0, 0, 0.45f));
    }

    private void drawTray(Game g, float x, float y, float w, float h) {
        ui.panel(x, y, w, h, INK);
        boolean mine = session.myTurn() && g.phase == Game.Phase.ROLL;
        Game.P cur = g.p[g.cur];
        String status;
        int actor = g.actor();
        if (g.phase == Game.Phase.OVER) status = "Game over.";
        else if (g.phase == Game.Phase.YIELD) status = actor == session.mySeat ? "You got zapped in Huber. Stay or run?" : "Waiting for " + g.p[actor].name + " to decide: stay in Huber or run?";
        else if (g.phase == Game.Phase.POWER) status = mine || actor == session.mySeat ? "Pick a personal power." : cur.name + " is picking a personal power...";
        else if (g.phase == Game.Phase.BUY) status = actor == session.mySeat ? "Buy cards or end your turn (Enter)." : cur.name + " is shopping...";
        else if (mine) status = !g.rolled ? "YOUR TURN. Roll! (Space)" : "Click dice to keep them, then reroll. Enter to stop.";
        else status = cur.name + " is rolling...";
        ui.text(ui.bold, status, x + 16, y + h - 12, mine || actor == session.mySeat ? GOLD : Color.WHITE);
        if (!mine && g.phase == Game.Phase.ROLL) ui.text(ui.small, g.rollsLeft + (g.rollsLeft == 1 ? " roll left" : " rolls left"), x + w - 110, y + h - 16, GREY);
        float clock = session.clockLeft();
        if (clock >= 0 && g.phase != Game.Phase.OVER) {
            String ct = (int) Math.ceil(clock) + "s";
            float cx = x + w - 196;
            ui.panel(cx - 70, y + h - 40, 60, 30, clock < 10 ? RED : new Color(0.25f, 0.27f, 0.33f, 1));
            ui.textC(ui.bold, ct, cx - 40, y + h - 16, Color.WHITE);
        }

        float ds = 78, gap = 10;
        float dx = x + 16;
        float dy = y + 40;
        for (int i = 0; i < g.nDice; i++) {
            float px = dx + i * (ds + gap);
            boolean kept = g.keep[i] && g.rolled && g.phase == Game.Phase.ROLL;
            float lift = kept ? 22 : 0;
            int face = g.dice[i];
            float rot = 0;
            if (rollAnim > 0 && !kept) { face = MathUtils.random(5); rot = MathUtils.random(-25f, 25f); lift = MathUtils.random(-4f, 8f); }
            boolean over = mine && g.rolled && g.rollsLeft > 0 && ui.hover(px, dy, ds, ds + 22);
            if (kept) ui.panel(px - 4, dy + lift - 4, ds + 8, ds + 8, GOLD);
            if (!g.rolled) {
                ui.panel(px, dy, ds, ds, new Color(1, 1, 1, 0.12f));
                ui.textC(ui.big, "?", px + ds / 2, dy + ds / 2 + 16, new Color(1, 1, 1, 0.35f));
            } else {
                ui.imgRot("img/die_" + face + ".png", px + ds / 2, dy + ds / 2 + lift, over ? ds + 6 : ds, rot, Color.WHITE);
            }
            if (kept) ui.textC(ui.small, "KEEP", px + ds / 2, dy - 2, GOLD);
            if (over && ui.clicked) session.act(Game.A_TOGGLE, i);
        }
        float bx = x + w - 196;
        if (mine) {
            String lbl = g.rolled ? "Reroll (" + g.rollsLeft + ")" : "ROLL";
            if (ui.button(lbl, bx, y + 86, 180, 60, ORANGE, g.rollsLeft > 0)) session.act(Game.A_ROLL, 0);
            if (ui.button("Done", bx, y + 22, 180, 50, GREEN, g.rolled)) session.act(Game.A_DONE, 0);
        }
    }

    private void drawMine(Game g, float x, float y, float w, float h) {
        ui.panel(x, y, w, h, INK);
        if (session.mySeat < 0) return;
        Game.P me = g.p[session.mySeat];
        ui.text(ui.bold, "Your stuff", x + 14, y + h - 10, GOLD);
        if (!me.alive) {
            ui.textW(ui.body, "You're a ghost. Every turn you haunt the table automatically: rattle Huber, slip someone a battery, or leave a casserole.", x + 14, y + h - 44, w - 28, Align.left, SOFT);
            return;
        }
        if (me.cards.length == 0) {
            ui.textW(ui.small, "No cards yet. Buy KEEP cards in the shop, or roll three hearts to unlock one of " + me.cast().name + "'s powers.",
                x + 14, y + h - 44, w - 28, Align.left, GREY);
        }
        float cx = x + 14, cy = y + h - 64;
        for (int cid : me.cards) {
            Card c = Card.ALL[cid];
            float cw = Math.min(w - 28, ui.width(ui.small, c.title) + 20);
            if (cx + cw > x + w - 14) { cx = x + 14; cy -= 34; }
            if (cy < y + 8) break;
            boolean over = ui.hover(cx, cy, cw, 28);
            ui.panel(cx, cy, cw, 28, c.isPower() ? PURPLE : BLUE);
            ui.text(ui.small, c.title, cx + 10, cy + 21, Color.WHITE);
            if (over) ui.tip = c.title + "\n" + c.text;
            cx += cw + 6;
        }
        ui.text(ui.tiny, "Hover players for their cards.  T chat  M mute  N music  Esc menu", x + 14, y + 20, GREY);
    }

    private void drawFloaters(Game g) {
        int[] stack = new int[6];
        for (Floater f : floaters) {
            if (f.delay > 0 || f.seat >= g.p.length) continue;
            float[] r = panelRect(f.seat, g.p.length);
            float a = f.t < 1.2f ? 1 : 1 - (f.t - 1.2f) / 0.4f;
            float fy = r[1] + 52 + f.t * 26;
            float cx = r[0] + r[2] / 2;
            float tw = ui.width(ui.bold, f.text) + (f.icon != null ? 26 : 0);
            ui.panel(cx - tw / 2 - 10, fy - 26, tw + 20, 30, new Color(0, 0, 0, 0.7f * a));
            Color c = f.color.cpy();
            c.a = a;
            float tx = cx - tw / 2;
            if (f.icon != null) { ui.img(f.icon, tx, fy - 24, 22, 22, new Color(1, 1, 1, a)); tx += 26; }
            ui.text(ui.bold, f.text, tx, fy - 4, c);
        }
    }

    private void drawBubbles(Game g) {
        ArrayList<float[]> placed = new ArrayList<>();
        for (int i = 0; i < g.p.length && i < 6; i++) {
            if (bubbleT[i] <= 0 || bubble[i] == null) continue;
            float[] r = panelRect(i, g.p.length);
            float a = Math.min(1, bubbleT[i] * 2);
            float w = Math.min(300, ui.width(ui.body, bubble[i]) + 28);
            float th = ui.measureW(ui.body, bubble[i], w - 24);
            float bx = MathUtils.clamp(r[0] + r[2] / 2 - w / 2, 8, Ui.W - w - 8);
            float by = r[1] - 34 - th;
            boolean moved = true;
            while (moved) {
                moved = false;
                for (float[] q : placed)
                    if (bx < q[0] + q[2] + 6 && bx + w + 6 > q[0] && by < q[1] + q[3] + 6 && by + th + 22 + 6 > q[1]) { by = q[1] - th - 28; moved = true; }
            }
            placed.add(new float[]{bx, by, w, th + 22});
            ui.rect(r[0] + r[2] / 2 - 2, by + th + 18, 4, r[1] - by - th - 18, new Color(1, 1, 1, 0.8f * a));
            ui.panel(bx, by, w, th + 22, new Color(1, 1, 1, 0.97f * a));
            ui.textW(ui.body, bubble[i], bx + 12, by + th + 12, w - 24, Align.center, new Color(0.1f, 0.1f, 0.15f, a));
        }
    }

    private void drawBanner() {
        float a = Math.min(1, Math.min(bannerT * 3, (bannerMax - bannerT) * 6));
        if (bannerImg != null) {
            float w = 900, iw = 300, ih = 170;
            float tw = w - iw - 70;
            float th = ui.measureW(ui.body, bannerSub == null ? "" : bannerSub, tw);
            float bh = ui.measureW(ui.big, banner, tw);
            float h = Math.max(ih + 40, bh + th + 70);
            float x = 640 - w / 2, y = 390 - h / 2 + (1 - a) * 30;
            Color c = bannerColor.cpy();
            c.a = 0.97f * a;
            ui.shadowPanel(x, y, w, h, c);
            float age = bannerMax - bannerT;
            float pop = age < 0.35f ? 0.7f + 0.3f * (age / 0.35f) + MathUtils.sin(age / 0.35f * MathUtils.PI) * 0.12f : 1 + MathUtils.sin(time * 3) * 0.015f;
            float cw = iw * pop, chh = ih * pop;
            ui.img(bannerImg, x + 20 + iw / 2 - cw / 2, y + h / 2 - chh / 2, cw, chh, new Color(1, 1, 1, a));
            ui.textW(ui.big, banner, x + iw + 45, y + h - 20, tw, Align.left, new Color(1, 1, 1, a));
            if (bannerSub != null) ui.textW(ui.body, bannerSub, x + iw + 45, y + h - 36 - bh, tw, Align.left, new Color(1, 0.95f, 0.9f, a));
            return;
        }
        float w = 760, th = ui.measureW(ui.body, bannerSub == null ? "" : bannerSub, w - 60);
        float h = 78 + th;
        float x = 640 - w / 2, y = 390 - h / 2 + (1 - a) * 30;
        Color c = bannerColor.cpy();
        c.a = 0.97f * a;
        ui.shadowPanel(x, y, w, h, c);
        ui.textC(ui.big, banner, 640, y + h - 14, new Color(1, 1, 1, a));
        if (bannerSub != null) ui.textW(ui.body, bannerSub, x + 30, y + h - 58, w - 60, Align.center, new Color(1, 0.95f, 0.9f, a));
    }

    private void dim() { ui.rect(0, 0, Ui.W, Ui.H, new Color(0, 0, 0, 0.55f)); }

    private void drawYield(Game g) {
        Game.P me = g.p[session.mySeat];
        dim();
        ui.shadowPanel(340, 230, 600, 270, BRICK);
        ui.textC(ui.big, "ZAPPED IN HUBER!", 640, 470, Color.WHITE);
        ui.textW(ui.body, "You have " + me.hp + " health. Run back to the Suburbs and " + g.p[g.cur].name + " moves in, or stay for +2 VP when your turn starts (plus whatever Huber does to you).",
            380, 420, 520, Align.center, new Color(1, 0.92f, 0.85f, 1));
        if (ui.button("Stay", 380, 260, 240, 60, RED, true)) session.act(Game.A_YIELD, 0);
        if (ui.button("RUN!", 660, 260, 240, 60, GREEN, true)) session.act(Game.A_YIELD, 1);
    }

    private void drawPower(Game g) {
        dim();
        ui.textC(ui.big, "THREE HEARTS! Pick a personal power.", 640, 610, Color.WHITE);
        for (int k = 0; k < 2; k++) {
            int cid = g.offer[k];
            if (cid < 0) continue;
            float cx = g.offer[1] < 0 ? 540 : 400 + k * 280, cy = 190;
            boolean over = ui.hover(cx, cy, 200, 320);
            card(cx, cy + (over ? 8 : 0), 200, 320, cid, 0, false);
            if (over && ui.clicked) session.act(Game.A_POWER, k);
        }
        ui.textC(ui.body, "KEEP powers stay with you. ONE-SHOT powers happen right now.", 640, 150, SOFT);
    }

    private void drawGameOver(Game g) {
        if (bannerT > 0) return;
        recordCareer(g);
        dim();
        ui.shadowPanel(70, 70, 1140, 560, INK);
        // winner
        if (g.winner >= 0) {
            Game.P w = g.p[g.winner];
            float bob = MathUtils.sin(time * 2.4f) * 5;
            ui.img(w.cast().portrait(), 150, 380 + bob, 190, 190);
            ui.textW(ui.huge, w.name.toUpperCase() + " WINS!", 90, 360, 320, Align.center, GOLD);
            ui.textW(ui.body, w.vp >= g.winVP ? "Reached " + g.winVP + " VP. Huber Heights is theirs. Condolences." : "Last one standing. Everyone else is haunting the brick ranches now.",
                100, 250, 300, Align.center, SOFT);
        } else {
            ui.textW(ui.huge, "HUBER WINS", 90, 470, 320, Align.center, GOLD);
            ui.textW(ui.body, "Everybody got knocked out. Huber always wins.", 100, 380, 300, Align.center, SOFT);
        }
        // report card
        ui.text(ui.big, "HUBER REPORT CARD", 460, 600, Color.WHITE);
        ui.rect(460, 556, 720, 3, GOLD);
        float y = 540;
        for (String[] a : awards(g)) {
            ui.text(ui.bold, a[0], 460, y, GOLD);
            ui.text(ui.bold, a[1], 760, y, Color.WHITE);
            ui.text(ui.small, a[2], 760 + ui.width(ui.bold, a[1]) + 12, y - 3, SOFT);
            y -= 44;
        }
        if (!careerLine.isEmpty()) ui.textW(ui.small, careerLine, 460, 168, 720, Align.left, new Color(0.6f, 0.85f, 1f, 1));
        if (session.canControl() && ui.button("Play Again", 780, 90, 200, 52, GREEN, true)) { session.backToLobby(); resetTable(); }
        if (ui.button("Main Menu", 990, 90, 200, 52, BLUE, true)) leave(null);
        if (!session.canControl()) ui.text(ui.small, "The host or lobby leader can start a rematch.", 460, 120, GREY);
    }

    /** Funny awards from what actually happened. {title, name, detail}. */
    private ArrayList<String[]> awards(Game g) {
        ArrayList<String[]> out = new ArrayList<>();
        String[][] defs = {
            {"Landlord of the Year", "turns started in Huber"},
            {"Only in Huber", "mishaps survived"},
            {"Most Likely to Flee", "times fled Huber"},
            {"Biggest Single Zap", "damage in one roll"},
            {"Demolition Crew", "damage dealt"},
            {"Punching Bag", "damage taken"},
            {"Grill Master", "health healed"},
            {"Shopaholic", "cards bought"},
            {"Undertaker", "players knocked out"},
            {"Property Tax Complainer", "ghostly moans"},
        };
        for (int k = 0; k < defs.length && out.size() < 8; k++) {
            int best = -1, bv = 0;
            for (int i = 0; i < g.p.length; i++) {
                Game.P p = g.p[i];
                int v = k == 0 ? p.sHuberTurns : k == 1 ? p.sMishaps : k == 2 ? p.sFled : k == 3 ? p.sBigZap : k == 4 ? p.sDealt
                    : k == 5 ? p.sTaken : k == 6 ? p.sHealed : k == 7 ? p.sBought : k == 8 ? p.sKOs : p.sMoans;
                if (v > bv) { bv = v; best = i; }
            }
            if (best >= 0) out.add(new String[]{defs[k][0], g.p[best].name, bv + " " + defs[k][1]});
        }
        return out;
    }

    /** Once per game: save your wins, games and head-to-head record against the other humans. */
    private void recordCareer(Game g) {
        if (recordedFor == g || session.mySeat < 0 || session.mySeat >= g.p.length) return;
        recordedFor = g;
        int games = prefs.getInteger("career.games", 0) + 1, wins = prefs.getInteger("career.wins", 0);
        boolean iWon = g.winner == session.mySeat;
        if (iWon) wins++;
        prefs.putInteger("career.games", games).putInteger("career.wins", wins);
        StringBuilder vs = new StringBuilder();
        for (int i = 0; i < g.p.length && i < session.seats.size(); i++) {
            Net.Seat st = session.seats.get(i);
            if (i == session.mySeat || st.kind == Net.Seat.BOT && st.conn != -3) continue;
            String other = st.name;
            String key = "h2h." + other.toLowerCase();
            String[] wl = prefs.getString(key, "0,0").split(",");
            int w = Integer.parseInt(wl[0]), l = Integer.parseInt(wl[1]);
            if (iWon) w++;
            else if (g.winner == i) l++;
            prefs.putString(key, w + "," + l);
            vs.append(vs.length() == 0 ? "" : "   ·   ").append("vs ").append(other).append(": ").append(w).append("-").append(l);
        }
        prefs.flush();
        careerLine = "Your record: " + wins + (wins == 1 ? " win" : " wins") + " in " + games + (games == 1 ? " game" : " games") + (vs.length() > 0 ? "      " + vs : "");
    }

    private void drawPause() {
        dim();
        ui.shadowPanel(440, 200, 400, 280, INK);
        ui.textC(ui.big, "Paused", 640, 450, Color.WHITE);
        ui.textC(ui.small, session.role == Session.Role.LOCAL ? "Bots wait while you're paused." : "The game keeps going online.", 640, 405, GREY);
        if (ui.button("Resume", 470, 330, 340, 50, GREEN, true)) paused = false;
        if (ui.button(musicOn ? "Music: ON" : "Music: OFF", 470, 214, 340, 46, PURPLE, true)) { musicOn = !musicOn; prefs.putBoolean("music", musicOn).flush(); }
        if (ui.button("Leave Game", 470, 272, 340, 46, RED, true)) leave(null);
    }

    private void drawTip(String t) {
        float w = Math.min(380, ui.width(ui.small, t) + 24);
        float th = ui.measureW(ui.small, t, w - 20);
        float x = MathUtils.clamp(ui.mx + 16, 6, Ui.W - w - 6), y = MathUtils.clamp(ui.my - th - 30, 6, Ui.H - th - 26);
        ui.panel(x, y, w, th + 20, new Color(0.03f, 0.03f, 0.06f, 0.96f));
        ui.textW(ui.small, t, x + 10, y + th + 10, w - 20, Align.left, Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        ui.vp.update(width, height, true);
        ui.makeFonts();
    }

    @Override
    public void dispose() {
        if (session != null) session.close();
        for (Sound s : sfx.values()) s.dispose();
        ui.dispose();
    }
}
