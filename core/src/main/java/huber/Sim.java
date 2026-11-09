package huber;

import java.util.Random;

/** Headless balance check: bots play each other. Run with ./gradlew sim (or -Pgames=5000). */
public class Sim {
    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
        Game.Opts opts = new Game.Opts();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("noflee")) opts.noFlee = true;
            if (args[i].equals("draft")) opts.draft = true;
            if (args[i].equals("noevents")) opts.events = false;
            if (args[i].startsWith("vp=")) opts.winVP = Integer.parseInt(args[i].substring(3));
        }
        Random r = new Random(7);
        Bot bot = new Bot();
        int[] winsByCast = new int[Cast.ALL.length], playsByCast = new int[Cast.ALL.length];
        int[] winsByStyle = new int[5], playsByStyle = new int[5];
        int[] byCount = new int[7], koWins = new int[7], hubWins = new int[7];
        long[] rounds = new long[7];
        int stuck = 0, nobody = 0, firstWins = 0;
        long mishaps = 0, yields = 0, stays = 0, powers = 0, buys = 0;
        for (int gi = 0; gi < games; gi++) {
            int n = 2 + r.nextInt(5);
            int[] whos = new int[n];
            boolean[] used = new boolean[Cast.ALL.length];
            for (int i = 0; i < n; i++) { int w; do w = r.nextInt(Cast.ALL.length); while (used[w]); used[w] = true; whos[i] = w; }
            String[] names = new String[n];
            boolean[] bots = new boolean[n];
            for (int i = 0; i < n; i++) { names[i] = Cast.ALL[whos[i]].name; bots[i] = true; }
            Game g = Game.create(names, whos, bots, r.nextLong(), opts);
            int first = g.cur;
            int steps = 0;
            while (g.phase != Game.Phase.OVER && steps < 20000) {
                int s = g.actor();
                int[] a = bot.next(g, s);
                if (a[0] == Game.A_YIELD) { if (a[1] == 1) yields++; else stays++; }
                if (!g.act(s, a[0], a[1])) { stuck++; System.out.println("Bad action " + a[0] + " in " + g.phase); break; }
                for (Game.Ev e : g.takeEvents()) {
                    if (e.type == Game.E_MISHAP) mishaps++;
                    if (e.type == Game.E_POWER) powers++;
                    if (e.type == Game.E_BUY) buys++;
                }
                steps++;
            }
            if (g.phase != Game.Phase.OVER) { stuck++; continue; }
            byCount[n]++;
            rounds[n] += g.round;
            for (int i = 0; i < n; i++) { playsByCast[whos[i]]++; playsByStyle[g.p[i].style]++; }
            if (g.winner < 0) { nobody++; continue; }
            if (g.winner == first) firstWins++;
            winsByCast[whos[g.winner]]++;
            winsByStyle[g.p[g.winner].style]++;
            if (g.p[g.winner].vp >= g.winVP) hubWins[n]++; else koWins[n]++;
        }
        System.out.println("Games: " + games + ", stuck: " + stuck + ", everyone KO'd: " + nobody);
        for (int n = 2; n <= 6; n++) if (byCount[n] > 0)
            System.out.printf("%d players: %4d games, avg %.1f rounds, won on VP %2.0f%%, last standing %2.0f%%%n",
                n, byCount[n], rounds[n] / (double) byCount[n], 100.0 * hubWins[n] / byCount[n], 100.0 * koWins[n] / byCount[n]);
        System.out.println("Win rate by character (fair share depends on table size; compare to each other):");
        for (int c = 0; c < Cast.ALL.length; c++)
            System.out.printf("  %-14s %5.1f%%%n", Cast.ALL[c].name, 100.0 * winsByCast[c] / Math.max(1, playsByCast[c]));
        System.out.println("Win rate by bot style:");
        for (int s = 0; s < 5; s++)
            System.out.printf("  %-10s %5.1f%%%n", Bot.STYLE[s], 100.0 * winsByStyle[s] / Math.max(1, playsByStyle[s]));
        System.out.printf("First player wins %.1f%% of games%n", 100.0 * firstWins / games);
        System.out.printf("Per game: %.1f mishaps, %.1f leave-Huber vs %.1f stay, %.1f powers, %.1f cards bought%n",
            mishaps / (double) games, yields / (double) games, stays / (double) games, powers / (double) games, buys / (double) games);
    }
}
