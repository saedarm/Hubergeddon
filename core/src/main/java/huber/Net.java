package huber;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plain TCP sockets, one JSON message per line. The host runs the real Game; clients send
 * actions and get the whole game state back after every change. Network threads only read
 * and queue; everything else happens on the game thread via poll().
 */
public class Net {
    public static final int PORT = 7777;
    public static final int VERSION = 3;

    public static class Seat {
        public static final int HOST = 0, REMOTE = 1, BOT = 2;
        public String name = "";
        public int who = -1, kind, conn = -1;
        public transient String key;   // the client's rejoin key; never sent to other players
        public Seat() {}
        public Seat(String name, int kind, int conn) { this.name = name; this.kind = kind; this.conn = conn; }
    }

    public static class Msg {
        public String t;
        public int a, b, seat = -1, version = VERSION;
        public String text;
        public Seat[] seats;
        public Game game;
        public Game.Ev[] ev;
        public Game.Opts opts;
        public String key;
        public float clock = -1;   // seconds left on the current player's turn timer, -1 = none
        public Msg() {}
        public Msg(String t) { this.t = t; }
        public Msg(String t, int a, int b) { this.t = t; this.a = a; this.b = b; }
    }

    /** One message from the network. msg == null means the connection closed. */
    public static class In {
        public final int conn;
        public final Msg msg;
        In(int conn, Msg msg) { this.conn = conn; this.msg = msg; }
    }

    private static Json json() {
        Json j = new Json(JsonWriter.OutputType.minimal);
        j.setIgnoreUnknownFields(true);
        j.setUsePrototypes(false);
        return j;
    }

    static class Conn {
        final int id;
        final Socket socket;
        final PrintWriter out;
        final Json json = json();
        volatile boolean open = true;

        Conn(int id, Socket s, ConcurrentLinkedQueue<In> inbox) throws Exception {
            this.id = id;
            this.socket = s;
            s.setTcpNoDelay(true);
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread t = new Thread(() -> {
                Json rj = json();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        try { inbox.add(new In(id, rj.fromJson(Msg.class, line))); }
                        catch (Exception bad) { System.err.println("Bad message: " + bad); }
                    }
                } catch (Exception ignored) {
                } finally {
                    open = false;
                    inbox.add(new In(id, null));
                }
            }, "net-read-" + id);
            t.setDaemon(true);
            t.start();
        }

        synchronized void send(Msg m) {
            if (!open) return;
            out.println(json.toJson(m, Msg.class));
            if (out.checkError()) open = false;
        }

        void close() { open = false; try { socket.close(); } catch (Exception ignored) {} }
    }

    // ------------------------------------------------------------------ host

    public static class Host {
        public final ConcurrentLinkedQueue<In> inbox = new ConcurrentLinkedQueue<>();
        private final CopyOnWriteArrayList<Conn> conns = new CopyOnWriteArrayList<>();
        private ServerSocket server;
        private volatile boolean running = true;
        private int nextId = 1;

        public Host(int port) throws Exception {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
            Thread t = new Thread(() -> {
                while (running) {
                    try {
                        Socket s = server.accept();
                        conns.add(new Conn(nextId++, s, inbox));
                    } catch (Exception e) { if (running) System.err.println("Accept failed: " + e); }
                }
            }, "net-accept");
            t.setDaemon(true);
            t.start();
        }

        public void send(int conn, Msg m) { for (Conn c : conns) if (c.id == conn) c.send(m); }

        public void broadcast(Msg m) { for (Conn c : conns) c.send(m); }

        public void drop(int conn) {
            for (Conn c : conns) if (c.id == conn) { c.close(); conns.remove(c); }
        }

        public void close() {
            running = false;
            for (Conn c : conns) c.close();
            try { server.close(); } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------------ client

    public static class Client {
        public final ConcurrentLinkedQueue<In> inbox = new ConcurrentLinkedQueue<>();
        private Conn conn;

        /**
         * Blocks for up to 20 seconds (a stopped Fly.io machine takes a few seconds to wake up).
         * Call from a background thread. tls = wrap the connection in TLS, which Fly.io's proxy
         * terminates; that's what lets a free shared IPv4 address work on port 7777.
         */
        public Client(String host, int port, boolean tls) throws Exception {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 20000);
            if (tls) {
                javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket)
                    ((javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(s, host, port, true);
                ssl.setSoTimeout(20000);
                ssl.startHandshake();   // sends the hostname (SNI), which is how Fly routes to the app
                ssl.setSoTimeout(0);
                s = ssl;
            }
            conn = new Conn(0, s, inbox);
        }

        public void send(Msg m) { conn.send(m); }

        public boolean open() { return conn.open; }

        public void close() { conn.close(); }
    }
}
