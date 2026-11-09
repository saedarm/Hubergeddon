package huber;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

/**
 * One game session from this computer's point of view:
 *  LOCAL  - playing alone with bots
 *  HOST   - playing and hosting friends from this computer
 *  CLIENT - joined someone else's game (a friend's computer or a dedicated server)
 *  SERVER - a dedicated server with no player of its own (Server.java, e.g. on Fly.io).
 *           The first friend to connect is the lobby leader and gets the host buttons.
 * The screen only talks to this class.
 */
public class Session {
    public enum Role { LOCAL, HOST, CLIENT, SERVER }

    public final Role role;
    public final ArrayList<Net.Seat> seats = new ArrayList<>();
    public final ArrayDeque<Game.Ev> events = new ArrayDeque<>();
    public Game game;
    public int mySeat = 0;
    public String closedReason;     // set when the connection is gone
    public boolean busy;            // the screen sets this while an animation is playing
    public boolean fastBots;
    public int lobbyChanges;
    public Game.Opts opts = new Game.Opts();
    /** Chat lines for the screen: {seat, name, text}. seat -1 = system message. */
    public final ArrayDeque<String[]> chat = new ArrayDeque<>();
    public boolean lost;            // connection dropped (not a polite goodbye): worth auto-rejoining
    public float clock = -1;        // seconds left for whoever's turn it is (online only)

    private Net.Host host;
    private Net.Client client;
    private int myConn = -1, leaderConn = -1;
    private final Bot bot = new Bot();
    private final Random rng = new Random();
    private float botWait, pingT, idle;
    private int autoSeat = -1, idleSeat = -1;
    private boolean autopiloting;

    private Session(Role role) { this.role = role; }

    public static Session local(String name) {
        Session s = new Session(Role.LOCAL);
        s.seats.add(new Net.Seat(name, Net.Seat.HOST, -1));
        for (int i = 0; i < 3; i++) s.addBot();
        return s;
    }

    public static Session host(String name, int port) throws Exception {
        Session s = new Session(Role.HOST);
        s.host = new Net.Host(port);
        s.seats.add(new Net.Seat(name, Net.Seat.HOST, -1));
        s.addBot();
        return s;
    }

    /** A dedicated server: no player of its own. */
    public static Session server(int port) throws Exception {
        Session s = new Session(Role.SERVER);
        s.host = new Net.Host(port);
        s.mySeat = -1;
        return s;
    }

    /** Connects; can block for several seconds (a sleeping Fly.io machine has to wake up), so call it off the render thread. */
    public static Session join(String address, int port, boolean tls, String name, String key) throws Exception {
        Session s = new Session(Role.CLIENT);
        s.client = new Net.Client(address, port, tls);
        Net.Msg hi = new Net.Msg("hello");
        hi.text = name;
        hi.key = key;
        s.client.send(hi);
        return s;
    }

    /** Runs the real game (this computer is the authority). */
    public boolean isHost() { return role != Role.CLIENT; }

    /** May use the host buttons: add bots, start, rematch. */
    public boolean canControl() {
        if (role == Role.CLIENT) return myConn >= 0 && myConn == leaderConn;
        return role != Role.SERVER;
    }

    /** True when this computer is connected to a dedicated server rather than a friend. */
    public boolean onServer() { return role == Role.CLIENT && leaderConn != -1; }

    private void command(String t, int a) {
        if (role == Role.CLIENT && canControl()) client.send(new Net.Msg(t, a, 0));
    }

    // ------------------------------------------------------------------ lobby

    public void addBot() {
        if (role == Role.CLIENT) { command("addbot", 0); return; }
        if (game != null || seats.size() >= 6) return;
        Net.Seat b = new Net.Seat("", Net.Seat.BOT, -2);
        b.who = freeCast();
        b.name = Cast.ALL[b.who].name;
        seats.add(b);
        lobbyChanged();
    }

    /** Can this seat be removed by whoever is in control? */
    public boolean removable(int i) {
        if (i < 0 || i >= seats.size()) return false;
        if (role == Role.CLIENT || role == Role.SERVER) return seats.get(i).conn != leaderConn;
        return i > 0;
    }

    public void removeSeat(int i) {
        if (role == Role.CLIENT) { command("remove", i); return; }
        if (game != null || !removable(i)) return;
        Net.Seat s = seats.remove(i);
        if (s.kind == Net.Seat.REMOTE) {
            Net.Msg bye = new Net.Msg("bye");
            bye.text = "You were removed from the game.";
            host.send(s.conn, bye);
            host.drop(s.conn);
        }
        lobbyChanged();
    }

    /** Choose your character (any role). */
    public void pick(int who) {
        if (game != null) return;
        if (role == Role.CLIENT) { client.send(new Net.Msg("pick", who, 0)); return; }
        if (role != Role.SERVER) setWho(0, who);
    }

    /** Change a bot's character. */
    public void cycleBot(int seat) {
        if (role == Role.CLIENT) { command("cycle", seat); return; }
        if (game != null || seat < 0 || seat >= seats.size() || seats.get(seat).kind != Net.Seat.BOT) return;
        Net.Seat s = seats.get(seat);
        for (int k = 1; k <= Cast.ALL.length; k++) {
            int w = (s.who + k) % Cast.ALL.length;
            if (!taken(w, seat)) { s.who = w; s.name = Cast.ALL[w].name; break; }
        }
        lobbyChanged();
    }

    public static final String[] OPT_NAMES = {"First to", "Fleeing", "Power Draft", "City events", "Bots", "Turn timer", "Same character twice", "Fast bots"};

    public String optValue(int id) {
        switch (id) {
            case 0: return opts.winVP + " VP";
            case 1: return opts.noFlee ? "NOT ALLOWED" : "allowed";
            case 2: return opts.draft ? "on" : "off";
            case 3: return opts.events ? "on" : "off";
            case 4: return Bot.DIFF[opts.diff];
            case 5: return opts.timer == 0 ? "off" : opts.timer + "s";
            case 6: return opts.dupes ? "allowed" : "no";
            case 7: return fastBots ? "on" : "off";
            default: return "";
        }
    }

    /** Step a lobby option to its next value. */
    public void cycleOpt(int id) {
        if (role == Role.CLIENT) { command("opt", id); return; }
        if (game != null) return;
        switch (id) {
            case 0: opts.winVP = opts.winVP == 20 ? 15 : opts.winVP == 15 ? 25 : 20; break;
            case 1: opts.noFlee = !opts.noFlee; break;
            case 2: opts.draft = !opts.draft; break;
            case 3: opts.events = !opts.events; break;
            case 4: opts.diff = (opts.diff + 1) % 3; break;
            case 5: opts.timer = opts.timer == 60 ? 90 : opts.timer == 90 ? 0 : 60; break;
            case 6:
                opts.dupes = !opts.dupes;
                if (!opts.dupes)   // undo duplicates: later copies choose again (bots get a free character)
                    for (int i = 0; i < seats.size(); i++) {
                        Net.Seat st = seats.get(i);
                        if (st.who >= 0 && taken(st.who, i, i)) {
                            if (st.kind == Net.Seat.BOT) { st.who = freeCast(); st.name = Cast.ALL[st.who].name; } else st.who = -1;
                        }
                    }
                break;
            case 7: fastBots = !fastBots; break;
            default: break;
        }
        lobbyChanged();
    }

    public void toggleFastBots() {
        if (role == Role.CLIENT) { command("fast", 0); return; }
        fastBots = !fastBots;
        lobbyChanged();
    }

    private void setWho(int seat, int who) {
        if (who < 0 || who >= Cast.ALL.length || taken(who, seat)) return;
        seats.get(seat).who = who;
        lobbyChanged();
    }

    public boolean taken(int who, int exceptSeat) {
        if (opts.dupes) return false;
        return taken(who, exceptSeat, seats.size());
    }

    /** Is this character used by a seat before index `before` (other than exceptSeat)? */
    private boolean taken(int who, int exceptSeat, int before) {
        for (int i = 0; i < before && i < seats.size(); i++) if (i != exceptSeat && seats.get(i).who == who) return true;
        return false;
    }

    private int freeCast() {
        ArrayList<Integer> free = new ArrayList<>();
        for (int w = 0; w < Cast.ALL.length; w++) if (!taken(w, -1, seats.size())) free.add(w);
        return free.isEmpty() ? 0 : free.get(rng.nextInt(free.size()));
    }

    private int humans() {
        int n = 0;
        for (Net.Seat s : seats) if (s.kind != Net.Seat.BOT) n++;
        return n;
    }

    public boolean canStart() { return canControl() && game == null && seats.size() >= 2; }

    public void start() {
        if (role == Role.CLIENT) { command("start", 0); return; }
        if (game != null || seats.size() < 2 || humans() == 0) return;
        for (Net.Seat s : seats) if (s.who < 0) s.who = freeCast();
        int n = seats.size();
        String[] names = new String[n];
        int[] whos = new int[n];
        boolean[] bots = new boolean[n];
        for (int i = 0; i < n; i++) {
            Net.Seat s = seats.get(i);
            names[i] = s.kind == Net.Seat.BOT ? Cast.ALL[s.who].name : s.name;
            whos[i] = s.who;
            bots[i] = s.kind == Net.Seat.BOT;
        }
        game = Game.create(names, whos, bots, rng.nextLong(), opts.copy());
        idle = 0; autoSeat = -1; idleSeat = -1;
        if (role != Role.SERVER) mySeat = 0;
        ArrayList<Game.Ev> ev = game.takeEvents();
        events.addAll(ev);
        if (host != null) {
            Net.Msg m = stateMsg(ev);
            m.seats = seats.toArray(new Net.Seat[0]);
            host.broadcast(m);
        }
        log("Game started with " + n + " players (" + humans() + " human).");
    }

    /** After a game ends, go back to the lobby with the same seats. */
    public void backToLobby() {
        if (role == Role.CLIENT) { command("again", 0); return; }
        game = null;
        events.clear();
        for (int i = seats.size() - 1; i >= 0; i--)
            if (seats.get(i).conn == -3) seats.remove(i);   // friends who dropped mid-game
        lobbyChanged();
    }

    private void lobbyChanged() {
        lobbyChanges++;
        if (role == Role.SERVER) {
            leaderConn = -1;
            for (Net.Seat s : seats) if (s.kind == Net.Seat.REMOTE) { leaderConn = s.conn; break; }
        }
        if (host != null && game == null) {   // mid-game changes go out with the next game state instead
            Net.Msg m = new Net.Msg("lobby");
            m.seats = seats.toArray(new Net.Seat[0]);
            m.a = fastBots ? 1 : 0;
            m.b = leaderConn;
            m.opts = opts;
            host.broadcast(m);
        }
    }

    // ------------------------------------------------------------------ game

    /** Your move. */
    public void act(int a, int b) {
        if (game == null) return;
        if (role == Role.CLIENT) client.send(new Net.Msg("act", a, b));
        else if (role != Role.SERVER) apply(mySeat, a, b);
    }

    public boolean myTurn() { return game != null && mySeat >= 0 && game.phase != Game.Phase.OVER && game.actor() == mySeat; }

    private Net.Msg stateMsg(ArrayList<Game.Ev> ev) {
        Net.Msg m = new Net.Msg("state");
        m.game = game;
        m.ev = ev.toArray(new Game.Ev[0]);
        m.a = fastBots ? 1 : 0;
        m.b = leaderConn;
        m.opts = opts;
        m.clock = clockLeft();
        return m;
    }

    private void apply(int seat, int a, int b) {
        if (!game.act(seat, a, b)) return;
        if (!autopiloting) idle = 0;
        ArrayList<Game.Ev> ev = game.takeEvents();
        if (role != Role.SERVER) events.addAll(ev);
        if (host != null) host.broadcast(stateMsg(ev));
    }

    public void update(float dt) {
        if (host != null) for (Net.In in; (in = host.inbox.poll()) != null; ) hostReceive(in);
        if (client != null) for (Net.In in; (in = client.inbox.poll()) != null; ) clientReceive(in);
        // Keepalive so proxies (like Fly.io's) don't drop a quiet connection.
        pingT += dt;
        if (pingT > 15) {
            pingT = 0;
            if (host != null) host.broadcast(new Net.Msg("ping"));
            if (client != null) client.send(new Net.Msg("ping"));
        }
        bot.diff = opts.diff;
        if (isHost() && game != null && game.phase != Game.Phase.OVER && !busy) {
            int s = game.actor();
            if (s != idleSeat) { idleSeat = s; idle = 0; }
            if (autoSeat >= 0 && autoSeat != s) autoSeat = -1;
            if (game.p[s].bot || autoSeat == s) {
                botWait += dt;
                int[] x = bot.next(game, s);
                float delay = x != null && x[0] == Game.A_TOGGLE ? 0.2f : role == Role.SERVER ? 0.95f : 0.75f;
                if (fastBots && game.p[s].bot) delay *= 0.3f;
                if (x != null && botWait >= delay) {
                    botWait = 0;
                    autopiloting = true;
                    apply(s, x[0], x[1]);
                    autopiloting = false;
                }
            } else {
                botWait = 0;
                if (timed()) {
                    idle += dt;
                    if (idle > opts.timer) {
                        autoSeat = s;
                        idle = 0;
                        system(game.p[s].name + " ran out of time. Autopilot takes this turn.");
                    }
                }
            }
        }
        if (role == Role.CLIENT && clock > 0) clock = Math.max(0, clock - dt);
    }

    /** Online games with a timer: is the current player on the clock? */
    private boolean timed() {
        if (opts.timer <= 0 || game == null || role == Role.LOCAL) return false;
        if (role == Role.HOST) { boolean anyRemote = false; for (Net.Seat st : seats) if (st.kind == Net.Seat.REMOTE) anyRemote = true; if (!anyRemote) return false; }
        return !game.p[game.actor()].bot;
    }

    /** Seconds left on the current player's turn timer, or -1 if there's no timer. */
    public float clockLeft() {
        if (role == Role.CLIENT) return clock;
        if (!timed() || autoSeat == game.actor()) return -1;
        return Math.max(0, opts.timer - idle);
    }

    // ------------------------------------------------------------------ chat

    public void sendChat(String text) {
        if (text == null || text.isBlank()) return;
        String t = text.trim();
        if (t.length() > 120) t = t.substring(0, 120);
        if (role == Role.CLIENT) { Net.Msg m = new Net.Msg("chat"); m.text = t; client.send(m); return; }
        int seat = role == Role.SERVER ? -1 : 0;
        broadcastChat(seat, seats.isEmpty() ? "Host" : seats.get(0).name, t);
    }

    private void broadcastChat(int seat, String name, String text) {
        chat.add(new String[]{String.valueOf(seat), name, text});
        while (chat.size() > 50) chat.removeFirst();
        if (host != null) {
            Net.Msg m = new Net.Msg("chat");
            m.a = seat;
            m.key = name;   // the speaker's name (in chat messages, key carries the name)
            m.text = text;
            host.broadcast(m);
        }
    }

    private void system(String text) { broadcastChat(-1, "", text); }

    private int seatOf(int conn) {
        for (int i = 0; i < seats.size(); i++) if (seats.get(i).conn == conn) return i;
        return -1;
    }

    private boolean fromLeader(int conn) {
        return role == Role.SERVER && conn == leaderConn;
    }

    private void hostReceive(Net.In in) {
        Net.Msg m = in.msg;
        int seat = seatOf(in.conn);
        if (m == null) { disconnected(seat); return; }
        switch (m.t) {
            case "hello": {
                if (m.version == Net.VERSION && m.key != null && rejoin(in.conn, m.key)) break;
                String why = m.version != Net.VERSION ? "Different game version. Everyone needs the same Hubergeddon."
                    : game != null ? "A game is already in progress. Try again when it's over."
                    : seats.size() >= 6 ? "The game is full (6 players)." : null;
                if (why != null) {
                    Net.Msg bye = new Net.Msg("bye");
                    bye.text = why;
                    host.send(in.conn, bye);
                    host.drop(in.conn);
                    return;
                }
                String name = m.text == null || m.text.isBlank() ? "Friend" : m.text.trim();
                if (name.length() > 16) name = name.substring(0, 16);
                Net.Seat ns = new Net.Seat(name, Net.Seat.REMOTE, in.conn);
                ns.key = m.key;
                seats.add(ns);
                host.send(in.conn, new Net.Msg("welcome", in.conn, 0));
                if (role == Role.SERVER && humans() == 1) {   // first one in gets a bot to play with
                    addBot();
                }
                lobbyChanged();
                log(name + " joined (" + humans() + " human).");
                break;
            }
            case "pick": if (seat >= 0 && game == null) setWho(seat, m.a); break;
            case "act": if (seat >= 0 && game != null) apply(seat, m.a, m.b); break;
            case "addbot": if (fromLeader(in.conn)) addBot(); break;
            case "remove": if (fromLeader(in.conn)) removeSeat(m.a); break;
            case "cycle": if (fromLeader(in.conn)) cycleBot(m.a); break;
            case "fast": if (fromLeader(in.conn)) toggleFastBots(); break;
            case "start": if (fromLeader(in.conn)) start(); break;
            case "opt": if (fromLeader(in.conn)) cycleOpt(m.a); break;
            case "chat": if (seat >= 0 && m.text != null) {
                String t = m.text.trim();
                if (t.length() > 120) t = t.substring(0, 120);
                if (!t.isEmpty()) broadcastChat(seat, seats.get(seat).name, t);
            } break;
            case "again": if (fromLeader(in.conn) && game != null && game.phase == Game.Phase.OVER) backToLobby(); break;
            default: break;   // "ping" and anything unknown
        }
    }

    /** A player who dropped is back (same rejoin key): give them their seat again. */
    private boolean rejoin(int conn, String key) {
        for (int i = 0; i < seats.size(); i++) {
            Net.Seat st = seats.get(i);
            if (st.key == null || !st.key.equals(key)) continue;
            if (st.kind == Net.Seat.REMOTE && st.conn != conn) host.drop(st.conn);   // stale socket
            st.kind = Net.Seat.REMOTE;
            st.conn = conn;
            host.send(conn, new Net.Msg("welcome", conn, 0));
            if (game != null) {
                game.p[i].bot = false;
                if (game.p[i].name.endsWith(" (bot)")) game.p[i].name = game.p[i].name.substring(0, game.p[i].name.length() - 6);
                if (autoSeat == i) autoSeat = -1;
                lobbyChanged();
                Net.Msg st2 = stateMsg(new ArrayList<>());
                st2.seats = seats.toArray(new Net.Seat[0]);
                host.broadcast(st2);
            } else lobbyChanged();
            system(st.name + " is back.");
            log(st.name + " rejoined.");
            return true;
        }
        return false;
    }

    private void disconnected(int seat) {
        if (seat < 0) return;
        log(seats.get(seat).name + " left.");
        if (game == null) {
            seats.remove(seat);
        } else {
            game.p[seat].bot = true;
            game.p[seat].name = game.p[seat].name + " (bot)";
            seats.get(seat).kind = Net.Seat.BOT;
            seats.get(seat).conn = -3;
        }
        if (role == Role.SERVER && humans() == 0) {
            // Everyone's gone: reset so the next group starts fresh.
            game = null;
            seats.clear();
            fastBots = false;
            opts = new Game.Opts();
            log("Everyone left. Lobby reset.");
        }
        lobbyChanged();
        if (game != null && host != null) host.broadcast(stateMsg(new ArrayList<>()));
    }

    private void clientReceive(Net.In in) {
        Net.Msg m = in.msg;
        if (m == null) {
            if (closedReason == null) { closedReason = "Lost the connection to the game."; lost = true; }
            return;
        }
        switch (m.t) {
            case "welcome": myConn = m.a; break;
            case "lobby":
                game = null;
                events.clear();
                seats.clear();
                for (Net.Seat s : m.seats) seats.add(s);
                fastBots = m.a == 1;
                leaderConn = m.b;
                if (m.opts != null) opts = m.opts;
                lobbyChanges++;
                break;
            case "state":
                if (m.seats != null) { seats.clear(); for (Net.Seat s : m.seats) seats.add(s); }
                game = m.game;
                fastBots = m.a == 1;
                leaderConn = m.b;
                if (m.opts != null) opts = m.opts;
                clock = m.clock;
                mySeat = -1;
                for (int i = 0; i < seats.size(); i++) if (seats.get(i).conn == myConn) mySeat = i;
                if (m.ev != null) for (Game.Ev e : m.ev) events.add(e);
                break;
            case "bye": closedReason = m.text; client.close(); break;
            case "chat": {
                chat.add(new String[]{String.valueOf(m.a), m.key == null ? "" : m.key, m.text == null ? "" : m.text});
                while (chat.size() > 50) chat.removeFirst();
                break;
            }
            default: break;
        }
    }

    public int myLobbySeat() {
        if (role == Role.LOCAL || role == Role.HOST) return 0;
        return seatOf(myConn);
    }

    public boolean isLeaderSeat(int i) { return i >= 0 && i < seats.size() && seats.get(i).conn == leaderConn && leaderConn >= 0; }

    private void log(String s) { if (role == Role.SERVER) System.out.println("[hubergeddon] " + s); }

    public void close() {
        if (host != null) {
            Net.Msg bye = new Net.Msg("bye");
            bye.text = "The host left.";
            host.broadcast(bye);
            host.close();
        }
        if (client != null) client.close();
    }
}
