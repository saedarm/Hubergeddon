package huber;

import java.util.Random;

/**
 * Computer players. Each has a personality (style) that weights what it wants from the dice.
 * Dice: tries every distinct "keep these" choice and simulates the rerolls to pick the best.
 * Huber: leaves when the damage it expects before its next turn would leave it too low.
 */
public class Bot {
    public static final String[] STYLE = {"Balanced", "Berserker", "Hoarder", "Sprinter", "Coward"};
    public static final String[] DIFF = {"Easy", "Normal", "Uncle Rob After Three Beers"};
    private final Random rng = new Random();
    /** 0 = Easy (sloppy), 1 = Normal, 2 = Uncle Rob After Three Beers (sharpest). */
    public int diff = 1;

    private int samples() { return diff == 0 ? 10 : diff == 2 ? 90 : 40; }

    /** The next {action, arg} for this seat, or null if it's not this seat's move. */
    public int[] next(Game g, int seat) {
        if (g.actor() != seat || g.phase == Game.Phase.OVER) return null;
        switch (g.phase) {
            case ROLL: return rollStep(g);
            case YIELD: return new int[]{Game.A_YIELD, shouldLeave(g, seat) ? 1 : 0};
            case POWER: return new int[]{Game.A_POWER, pickPower(g)};
            case BUY: return buyStep(g);
            default: return null;
        }
    }

    // ------------------------------------------------------------------ dice

    private String planKey;
    private boolean[] plan;

    private int[] rollStep(Game g) {
        if (!g.rolled) return new int[]{Game.A_ROLL, 0};
        // Decide once per roll, then stick to it (the simulation is random, so re-deciding flip-flops).
        StringBuilder k = new StringBuilder().append(g.cur).append(':').append(g.round).append(':').append(g.rollsLeft).append(':');
        for (int i = 0; i < g.nDice; i++) k.append(g.dice[i]);
        String key = k.toString();
        if (!key.equals(planKey)) { planKey = key; plan = bestKeep(g); }
        boolean[] want = plan;
        if (want == null) return new int[]{Game.A_DONE, 0};
        for (int i = 0; i < g.nDice; i++) if (want[i] != g.keep[i]) return new int[]{Game.A_TOGGLE, i};
        return new int[]{Game.A_ROLL, 0};
    }

    /** null = stop rolling. */
    boolean[] bestKeep(Game g) {
        int[] c = g.counts();
        double stopValue = value(g, c);
        int samples = samples();
        double best = stopValue + 0.05;   // slight bias toward stopping when it's a wash
        int[] bestKeep = null;
        int[] k = new int[6];
        // Enumerate how many of each face to keep.
        int total = 1;
        for (int f = 0; f < 6; f++) total *= (c[f] + 1);
        for (int code = 0; code < total; code++) {
            int x = code, kept = 0;
            for (int f = 0; f < 6; f++) { k[f] = x % (c[f] + 1); x /= (c[f] + 1); kept += k[f]; }
            if (kept == g.nDice) continue;
            double v = 0;
            for (int s = 0; s < samples; s++) v += rollout(g, k, g.rollsLeft);
            v /= samples;
            if (diff == 0) v += rng.nextGaussian() * 1.4;   // Easy bots misjudge
            if (v > best) { best = v; bestKeep = k.clone(); }
        }
        if (bestKeep == null) return null;
        boolean[] want = new boolean[8];
        int[] left = bestKeep.clone();
        for (int i = 0; i < g.nDice; i++) if (left[g.dice[i]] > 0) { left[g.dice[i]]--; want[i] = true; }
        return want;
    }

    private double rollout(Game g, int[] kept, int rollsLeft) {
        int[] c = kept.clone();
        int free = g.nDice;
        for (int v : kept) free -= v;
        for (int r = 0; r < rollsLeft; r++) {
            for (int i = 0; i < free; i++) c[rng.nextInt(6)]++;
            if (r == rollsLeft - 1) break;
            // simple follow-up policy: keep good faces and number pairs, reroll the rest
            int[] keepNow = new int[6];
            free = 0;
            for (int f = 0; f < 6; f++) {
                boolean good = f >= 3 ? faceWeight(g, f) > 0.55 : c[f] >= 2;
                if (good) keepNow[f] = c[f]; else free += c[f];
            }
            c = keepNow;
        }
        return value(g, c);
    }

    private double faceWeight(Game g, int f) {
        Game.P me = g.p[g.cur];
        switch (f) {
            case Game.ZAP: return zapWeight(g, me);
            case Game.HEART: return healWeight(me) * (me.loc == Game.SUBURBS || me.has(Card.LAWN_GUY) ? 1 : 0.1);
            case Game.BAT: return batWeight(me);
            default: return 0;
        }
    }

    double value(Game g, int[] c) {
        Game.P me = g.p[g.cur];
        double vpW = (me.style == 3 ? 1.35 : 1.0) * (diff == 2 ? 1.2 : 1.0);
        double v = 0;
        int vpGain = 0;
        for (int f = 0; f < 3; f++) if (c[f] >= 3) {
            int pts = (f + 1) + (c[f] - 3);
            if (f < 2 && me.has(Card.SPREADSHEET)) pts++;
            if (f == 0 && me.has(Card.SPARE_BRICKS)) pts += 2;
            vpGain += pts;
        }
        v += vpGain * vpW;
        if (me.vp + vpGain >= g.winVP) v += 30;
        if (me.loc == Game.SUBURBS) {
            int per = me.has(Card.LAWN_MUNCHER) ? 2 : 1;
            v += Math.min(c[Game.HEART] * per, me.maxHp - me.hp) * healWeight(me);
        } else if (me.has(Card.LAWN_GUY)) v += Math.min(2, c[Game.HEART]) * 0.9;
        if (c[Game.HEART] >= 3 && powersLeft(me) > 0) v += 2.5;
        v += c[Game.BAT] * batWeight(me);
        int z = c[Game.ZAP];
        if (z > 0) {
            if (me.has(Card.MOWER)) z++;
            v += z * zapWeight(g, me);
            v += killBonus(g, me, z);
        }
        return v;
    }

    private double healWeight(Game.P me) {
        double w = me.hp <= 3 ? 1.6 : me.hp <= 6 ? 0.8 : 0.3;
        return me.style == 4 ? w * 1.5 : w;
    }

    private double batWeight(Game.P me) { return me.style == 2 ? 0.8 : 0.55; }

    private double zapWeight(Game g, Game.P me) {
        double w;
        if (me.loc != Game.SUBURBS) {
            int targets = 0;
            for (Game.P q : g.p) if (q.alive && q.loc == Game.SUBURBS && q != me) targets++;
            w = 0.3 + 0.25 * targets;
        } else {
            w = g.occupant(Game.HUBER) >= 0 ? 0.85 : 0.15;
        }
        return me.style == 1 ? w * 1.6 : w;
    }

    private double killBonus(Game g, Game.P me, int z) {
        double b = 0;
        for (Game.P q : g.p) {
            if (!q.alive || q == me) continue;
            boolean target = me.loc == Game.SUBURBS ? q.loc != Game.SUBURBS : q.loc == Game.SUBURBS;
            if (target && q.hp <= z && !q.shield) b += 3 + q.vp * 0.3;
        }
        return b;
    }

    private int powersLeft(Game.P me) {
        int n = 0;
        for (Card c : Card.ALL) if (c.owner == me.who && !me.sawIt(c)) n++;
        return n;
    }

    // ------------------------------------------------------------------ Huber

    boolean shouldLeave(Game g, int seat) {
        Game.P me = g.p[seat];
        if (diff == 0) return me.hp <= 3 ? rng.nextInt(4) > 0 : rng.nextInt(3) == 0;
        double incoming = 0.6;   // mishaps average a bit under 1 health
        for (Game.P q : g.p) if (q.alive && q != me && q.loc == Game.SUBURBS) incoming += 1.1;
        if (me.has(Card.LOAD_BEARING)) incoming *= 0.6;
        double after = me.hp - incoming;
        if (me.vp + 2 >= g.winVP && after >= 1.5) return false;   // the win is one wake-up away
        double floor = me.style == 4 ? 5 : me.style == 1 ? 1.5 : me.style == 3 ? 2.5 : 3;
        return after < floor;
    }

    // ------------------------------------------------------------------ powers and shop

    private int pickPower(Game g) {
        double a = cardValue(g, g.cur, g.offer[0]), b = g.offer[1] >= 0 ? cardValue(g, g.cur, g.offer[1]) : -99;
        return b > a ? 1 : 0;
    }

    private int[] buyStep(Game g) {
        Game.P me = g.p[g.cur];
        if (diff == 0) {   // Easy: impulse buys
            for (int s = 0; s < 3; s++)
                if (g.market[s] >= 0 && g.price(g.cur, g.market[s]) <= me.bat && rng.nextInt(3) == 0) return new int[]{Game.A_BUY, s};
            return new int[]{Game.A_END, 0};
        }
        int bestSlot = -1;
        double best = 0;
        for (int s = 0; s < 3; s++) {
            int cid = g.market[s];
            if (cid < 0) continue;
            int cost = g.price(g.cur, cid);
            if (cost > me.bat) continue;
            double v = cardValue(g, g.cur, cid);
            double ratio = v / Math.max(1, cost);
            if (v > 0 && ratio > (me.style == 2 ? 0.35 : 0.42) && v > best) { best = v; bestSlot = s; }
        }
        if (bestSlot >= 0) return new int[]{Game.A_BUY, bestSlot};
        // Nothing worth it: sweep if rich and the shop is junk.
        if (me.bat >= 6 + g.sweepCost(g.cur) && g.seq % 3 == 0) {
            double total = 0;
            for (int cid : g.market) if (cid >= 0) total += cardValue(g, g.cur, cid);
            if (total < 4) return new int[]{Game.A_SWEEP, 0};
        }
        return new int[]{Game.A_END, 0};
    }

    /** Rough worth of a card to this seat, in "VP-ish" units. */
    double cardValue(Game g, int seat, int cid) {
        Game.P me = g.p[seat];
        Card c = Card.ALL[cid];
        int others = 0, inHuber = 0;
        for (Game.P q : g.p) if (q.alive && q != me) { others++; if (q.loc != Game.SUBURBS) inHuber++; }
        int need = me.maxHp - me.hp;
        double hurt = me.hp <= 4 ? 1.2 : me.hp <= 7 ? 0.6 : 0.2;
        int vpTo = g.winVP - me.vp;
        double v;
        switch (c) {
            case YARD_MONTH: v = vpOr(2, vpTo); break;
            case FRONT_ROW: v = vpOr(3, vpTo) + hurt * Math.min(1, need); break;
            case FISH_FRY: v = vpOr(2, vpTo) + hurt * Math.min(2, need); break;
            case GARAGE_SALE: v = vpOr(1, vpTo) + hurt * Math.min(1, need); break;
            case FIREWORKS: v = vpOr(2, vpTo) + others * 0.7; break;
            case SIREN_TEST: v = others * 0.6; break;
            case PILEUP: v = me.loc != Game.SUBURBS ? -1 : inHuber * 1.3; break;
            case FRIED_BOLOGNA: case SECOND_WIND: v = hurt * Math.min(c == Card.SECOND_WIND ? 4 : 3, need) + 0.2; break;
            case TRACTOR: v = 3.2; break;
            case MEIJER_LOT: v = me.loc == Game.SUBURBS && me.hp >= 7 ? 2.2 : 0.3; break;
            case ZONING: case PROBE: v = 1.6; break;
            case BRICK_WINDOW: case DUST: v = others * 0.6; break;
            case I70: case LANDLORD: v = others * 0.45; break;
            case THIRD_ARM: v = 5.5; break;
            case SAUCER: v = 2.8; break;
            case BUCKEYE: v = 3.2; break;
            case TINFOIL: case LOAD_BEARING: case FLANNEL: v = 2.5; break;
            case MOWER: v = 3.0; break;
            case SOLAR: v = 1.2; break;
            case COUPONS: v = 2.0; break;
            case LAWN_GUY: case LAWN_MUNCHER: v = 2.2; break;
            case RECLINER: v = 2.2 + hurt; break;
            case SPARE_BRICKS: v = 1.5; break;
            case GOSSIP: v = 1.8; break;
            case CHARGER: v = 2.4; break;
            case NIGHT_OWL: v = 2.3; break;
            case DRAWN: v = 2.3; break;
            case CORNFIELD: case PHOTOSYNTH: v = 2.0; break;
            case GRUDGE: v = 1.4; break;
            case PLUTONIUM: v = 3.4; break;
            case MECHANIC: v = 0.8; break;
            case GNOMES: v = 2.3; break;
            case SPREADSHEET: v = 2.8; break;
            case RECONCILE: { int m = 0; for (Game.P q : g.p) if (q.alive && q != me) m = Math.max(m, q.bat); v = Math.max(0, m - me.bat) * 0.5; break; }
            case BACKGROUND: v = me.loc != Game.SUBURBS ? 2.5 : 1.2; break;
            case SHIP_IT: v = 1.6; break;
            case SMOOTH: v = 1.6; break;
            case BORROW: v = others * 0.5; break;
            case REDS_FAN: v = 2.0; break;
            case GRILL: v = hurt * Math.min(4, need) + 0.3; break;
            case LONG_STORY: v = 1.0 + others * 0.25; break;
            case POOL_NAP: v = hurt * need * 0.8; break;
            case SPLIT: v = 3.0; break;
            case BURP: case DAD_JOKE: v = others * 0.45; break;
            case CLIPBOARD: v = 1.8; break;
            case WRONG_HOUSE: v = 2.0; break;
            case BUILT_HERE: v = 2.2; break;
            case PORCH: v = 2.0; break;
            default: v = 1.0;
        }
        if (c.keep && g.round > 10) v *= 0.7;   // late keepers have less time to pay off
        return v;
    }

    private double vpOr(int vp, int vpTo) { return vp >= vpTo ? 40 : vp * 1.1; }
}
