package huber;

/**
 * Dedicated server: runs games and bots with no window and no player of its own.
 * Used for Fly.io (see Dockerfile and fly.toml). The first player to connect is the lobby leader.
 *
 *   java -jar Hubergeddon-server.jar            (port 7777, or $PORT)
 *   java -jar Hubergeddon-server.jar 9000
 */
public class Server {
    public static void main(String[] args) throws Exception {
        int port = Net.PORT;
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) port = Integer.parseInt(env.trim());
        if (args.length > 0) port = Integer.parseInt(args[0]);
        Session s = Session.server(port);
        System.out.println("[hubergeddon] Server listening on port " + port + " (protocol v" + Net.VERSION + ")");
        long last = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            float dt = Math.min(0.25f, (now - last) / 1e9f);
            last = now;
            s.update(dt);
            Thread.sleep(30);
        }
    }
}
