package huber;

import java.util.ArrayList;
import java.util.Random;

/**
 * All the rules as plain Java. Draws nothing and plays nothing, so it can be simulated headless
 * and sent over the network as JSON. Every change goes through act(); the screen reads the
 * public fields and plays back the events.
 */
public class Game {
    public static final int WIN_VP = 20, START_HP = 10;
    public static final int ONE = 0, TWO = 1, THREE = 2, ZAP = 3, HEART = 4, BAT = 5;
    public static final int SUBURBS = 0, HUBER = 1, ROSE = 2;
    public static final String[] PLACE = {"the Suburbs", "Huber", "the Rose lot"};
    public static final String[] FACE = {"1", "2", "3", "zap", "heart", "battery"};

    // Actions
    public static final int A_ROLL = 1, A_TOGGLE = 2, A_DONE = 3, A_YIELD = 4, A_POWER = 5, A_BUY = 6, A_SWEEP = 7, A_END = 8;

    // Events (for the screen and sounds)
    public static final int E_LOG = 0, E_ROLL = 1, E_DMG = 2, E_HEAL = 3, E_VP = 4, E_BAT = 5, E_ENTER = 6, E_LEAVE = 7,
        E_KO = 8, E_WIN = 9, E_BUY = 10, E_SAY = 11, E_MISHAP = 12, E_POWER = 13, E_GHOST = 14, E_TURN = 15, E_SWEEP = 16,
        E_EVENT = 17, E_ZAP = 18;

    /** Lobby options. winVP, noFlee, draft and events change the rules; the rest are read by Session. */
    public static class Opts {
        public int winVP = 20;
        public boolean noFlee, draft, events = true, dupes;
        public int diff = 1;      // bots: 0 Easy, 1 Normal, 2 Uncle Rob After Three Beers
        public int timer = 60;    // online turn timer in seconds, 0 = off
        public Opts copy() {
            Opts o = new Opts();
            o.winVP = winVP; o.noFlee = noFlee; o.draft = draft; o.events = events; o.dupes = dupes; o.diff = diff; o.timer = timer;
            return o;
        }
    }

    /** Huber-wide events: one hits the whole table every 3 rounds. Some last for that round. */
    public static final String[][] EVENTS = {
        {"CITY COUNCIL MEETING", "Three hours on whether to repaint a water tower. Everyone loses 1 battery."},
        {"SNOW EMERGENCY LEVEL 3", "Nobody can leave Huber this round. Roads are closed. Nobody plowed."},
        {"CONCERT NIGHT AT THE ROSE", "The bass carries for miles. Everyone out in the Suburbs loses 1 health."},
        {"GRAND OPENING: ANOTHER CAR WASH", "Huber's fifth this year. Free batteries for everyone: +2 each."},
        {"POWER OUTAGE", "A squirrel. Again. Everyone rolls 1 fewer die this round."},
        {"FREE SAMPLE DAY", "Little cups of everything. Everyone heals 2, even in Huber."},
        {"PROPERTY TAX REASSESSMENT", "Your brick ranch is worth more on paper. Everyone with 10+ VP loses 1 VP."},
        {"AIR SHOW AT WRIGHT-PATT", "Everyone's fired up. Zaps deal 1 extra damage this round."},
        {"CITYWIDE GARAGE SALE", "Everything must go. Shop cards cost 1 less this round."},
        {"OLD TROY PIKE RESURFACING", "Whoever's in Huber or the Rose lot sits in traffic and loses 2 health."},
    };
    public static final int EV_COUNCIL = 0, EV_SNOW = 1, EV_CONCERT = 2, EV_CARWASH = 3, EV_OUTAGE = 4, EV_SAMPLES = 5,
        EV_TAX = 6, EV_AIRSHOW = 7, EV_GARAGE = 8, EV_RESURFACE = 9;

    public enum Phase { ROLL, YIELD, POWER, BUY, OVER }

    /** Only-in-Huber mishaps: drawn every time you start a turn in Huber or the Rose lot. */
    public static final String[] MISHAPS = {
        "Every house is the same brick ranch. You got lost going home. Lose 1 battery.",
        "Stuck on Old Troy Pike behind 26,500 other cars. Roll 1 fewer die this turn.",
        "Old Troy Pike widening project. Lane closed. 1 fewer reroll this turn.",
        "Tried to find downtown Huber. There isn't one. Lose 1 VP.",
        "Your landlord is a hedge fund. Rent's due. Lose 2 batteries (or 1 health if you're broke).",
        "Parked at the Rose Music Center. Walked a mile. Lose 1 health.",
        "Stubbed your toe on a brick. There are 10,707 houses' worth. Lose 1 health.",
        "Went around the roundabout four times. Lose 1 health.",
        "A 'Come Grow With Us!' sign waves at you. Nothing happens.",
        "The city announced a Brandt Pike revitalization plan. Again. Nothing happens.",
        "Meijer had everything. You bought everything. Lose half your batteries.",
        "A Wright-Patt flyover rattled every window on the street. Lose 1 health.",
        "The HOA sent a letter about your alien lawn ornaments. Lose 1 VP.",
        "Neighbor mowed at 7 a.m. on a Sunday. Lose 1 health and 1 reroll this turn.",
    };

    public static class P {
        public String name = "";
        public int who, hp = START_HP, maxHp = START_HP, vp, bat, loc, style, flannelRound = -1;
        public boolean alive = true, bot, shield, splitUsed;
        public int[] cards = new int[0];
        public int[] seen = new int[0];   // personal powers already drawn
        public boolean drafted;
        public int lease;   // turns started in Huber in a row (No Fleeing: evicted at the end of your first turn there)
        // stats for the end-of-game report card
        public int sHuberTurns, sMishaps, sFled, sEntered, sBigZap, sDealt, sTaken, sHealed, sBought, sMoans, sKOs;

        public boolean has(Card c) { for (int i : cards) if (i == c.ordinal()) return true; return false; }
        public Cast cast() { return Cast.ALL[who]; }
        void add(Card c) { cards = push(cards, c.ordinal()); }
        void saw(Card c) { seen = push(seen, c.ordinal()); }
        boolean sawIt(Card c) { for (int i : seen) if (i == c.ordinal()) return true; return false; }
    }

    public static class Ev {
        public int type, a = -1, b, c;
        public String text;
        public Ev() {}
        Ev(int type, int a, int b, String text) { this.type = type; this.a = a; this.b = b; this.text = text; }
    }

    public P[] p;
    public int cur, round = 1, winner = -1, nDice = 6, rollsLeft, lastMishap = -1, seq;
    public int[] dice = new int[8];
    public boolean[] keep = new boolean[8];
    public boolean rolled, powerPending, twoPlayer, draftPending;
    public int winVP = WIN_VP, event = -1, eventRound = -1;
    public boolean noFlee, draft, cityEvents = true;
    public boolean[] askYield;
    public Phase phase = Phase.ROLL;
    public int[] market = {-1, -1, -1};
    public int[] offer = {-1, -1};
    public int[] deck = new int[0], discard = new int[0];
    public int deckTop;

    public transient ArrayList<Ev> events = new ArrayList<>();
    private transient Random rng = new Random();
    private int diceMod, rerollMod;
    private int fxSrc = -1;   // who a damage effect visually comes from (for zap bolts)

    // ------------------------------------------------------------------ setup

    public static Game create(String[] names, int[] whos, boolean[] bots, long seed) {
        return create(names, whos, bots, seed, new Opts());
    }

    public static Game create(String[] names, int[] whos, boolean[] bots, long seed, Opts o) {
        Game g = new Game();
        g.winVP = o.winVP; g.noFlee = o.noFlee; g.draft = o.draft; g.cityEvents = o.events;
        g.rng = new Random(seed);
        int n = names.length;
        g.p = new P[n];
        g.askYield = new boolean[n];
        for (int i = 0; i < n; i++) {
            P pl = new P();
            pl.name = names[i]; pl.who = whos[i]; pl.bot = bots[i];
            pl.style = g.rng.nextInt(5);
            g.p[i] = pl;
        }
        g.twoPlayer = n == 2;
        ArrayList<Integer> d = new ArrayList<>();
        for (Card c : Card.ALL) if (!c.isPower()) for (int k = 0; k < c.copies; k++) d.add(c.ordinal());
        g.deck = new int[d.size()];
        for (int i = 0; i < d.size(); i++) g.deck[i] = d.get(i);
        g.shuffle(g.deck);
        for (int i = 0; i < 3; i++) g.market[i] = g.draw();
        g.cur = g.rng.nextInt(n);
        // Later seats start with a few batteries to make up for going later.
        for (int k = 1; k < n; k++) g.p[(g.cur + k) % n].bat = k;
        g.log(g.p[g.cur].name + " goes first.");
        g.beginTurn();
        return g;
    }

    public void setRandom(Random r) { rng = r; }

    // ------------------------------------------------------------------ who acts

    /** The seat whose input the game is waiting on. */
    public int actor() {
        if (phase == Phase.YIELD) for (int i = 0; i < p.length; i++) if (askYield[i]) return i;
        return cur;
    }

    public boolean act(int seat, int a, int arg) {
        if (phase == Phase.OVER || seat != actor()) return false;
        boolean ok = false;
        switch (a) {
            case A_ROLL: ok = phase == Phase.ROLL && roll(); break;
            case A_TOGGLE: ok = phase == Phase.ROLL && toggle(arg); break;
            case A_DONE: ok = phase == Phase.ROLL && rolled && resolveDice(); break;
            case A_YIELD: ok = phase == Phase.YIELD && answerYield(seat, arg == 1); break;
            case A_POWER: ok = phase == Phase.POWER && pickPower(arg); break;
            case A_BUY: ok = phase == Phase.BUY && buy(arg); break;
            case A_SWEEP: ok = phase == Phase.BUY && sweep(); break;
            case A_END: ok = phase == Phase.BUY && endTurn(); break;
        }
        if (ok) seq++;
        return ok;
    }

    public int price(int seat, int card) {
        if (card < 0) return 99;
        return Math.max(0, Card.ALL[card].cost - (p[seat].has(Card.COUPONS) ? 1 : 0) - (eventOn(EV_GARAGE) ? 1 : 0));
    }

    /** Is this round-long city event in effect right now? */
    public boolean eventOn(int id) { return event == id && eventRound == round; }

    /** Can someone hit in Huber flee right now? */
    public boolean canFlee() { return !noFlee && !eventOn(EV_SNOW); }

    public int sweepCost(int seat) { return p[seat].has(Card.MECHANIC) ? 1 : 2; }

    public int aliveCount() { int c = 0; for (P q : p) if (q.alive) c++; return c; }

    public boolean roseOpen() { return aliveCount() >= 5; }

    public int occupant(int loc) {
        for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].loc == loc) return i;
        return -1;
    }

    // ------------------------------------------------------------------ turn flow

    private void beginTurn() {
        P me = p[cur];
        events.add(new Ev(E_TURN, cur, 0, null));
        if (!me.alive) { ghostTurn(); return; }
        me.shield = false;
        diceMod = 0; rerollMod = 0; lastMishap = -1;
        if (me.loc != SUBURBS) {
            me.sHuberTurns++;
            me.lease++;
            if (twoPlayer) gainBat(cur, 1, "for waking up in " + PLACE[me.loc]);
            else gainVP(cur, 2, "for waking up in " + PLACE[me.loc]);
            if (me.has(Card.NIGHT_OWL)) gainVP(cur, 1, "Night Owl");
            mishap(cur);
            if (checkOver()) return;
            if (!me.alive) { advance(); return; }
        } else {
            if (me.has(Card.CORNFIELD)) heal(cur, 1, "Cornfield Hideout");
            if (me.has(Card.PHOTOSYNTH)) gainBat(cur, 1, "Photosynthesis");
        }
        if (me.has(Card.PHOTOSYNTH)) gainBat(cur, 1, "Photosynthesis");
        if (me.has(Card.REDS_FAN) && me.hp <= 5) gainVP(cur, 1, "Reds Fan Since Birth");
        if (eventOn(EV_OUTAGE)) diceMod--;
        nDice = Math.max(1, Math.min(8, 6 + (me.has(Card.THIRD_ARM) ? 1 : 0) + (me.has(Card.SAUCER) && me.loc != SUBURBS ? 1 : 0) + diceMod));
        rollsLeft = Math.max(1, 3 + (me.has(Card.BUCKEYE) ? 1 : 0) + rerollMod);
        for (int i = 0; i < 8; i++) keep[i] = false;
        rolled = false;
        powerPending = false;
        phase = Phase.ROLL;
        if (draft && !me.drafted) {   // Power Draft: pick a personal power before your first roll
            me.drafted = true;
            if (offerPowers(me)) { draftPending = true; phase = Phase.POWER; log(me.name + " drafts a starting power."); }
        }
    }

    private void mishap(int s) {
        P me = p[s];
        if (me.has(Card.BUILT_HERE)) { log(me.name + " is immune to Huber. They ARE Huber."); return; }
        int m = rng.nextInt(MISHAPS.length);
        lastMishap = m;
        me.sMishaps++;
        events.add(new Ev(E_MISHAP, s, m, MISHAPS[m]));
        log("ONLY IN HUBER: " + MISHAPS[m]);
        switch (m) {
            case 0: loseBat(s, 1); break;
            case 1: diceMod--; break;
            case 2: rerollMod--; break;
            case 3: case 12: loseVP(s, 1); break;
            case 4: if (me.bat >= 2) loseBat(s, 2); else { loseBat(s, me.bat); damage(s, 1, -1, false); } break;
            case 5: case 6: case 7: case 11: damage(s, 1, -1, false); break;
            case 10: loseBat(s, me.bat / 2); break;
            case 13: damage(s, 1, -1, false); rerollMod--; break;
            default: break;
        }
    }

    private void ghostTurn() {
        P me = p[cur];
        int f = rng.nextInt(6);
        String who = "The ghost of " + me.name;
        switch (f) {
            case ZAP: {
                events.add(new Ev(E_GHOST, cur, f, who + " rattles the bricks. Everyone in Huber takes 1 damage."));
                log(who + " rattles the bricks.");
                fxSrc = cur;
                for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].loc != SUBURBS) damage(i, 1, -1, false);
                fxSrc = -1;
                break;
            }
            case HEART: {
                int t = lowest(true);
                events.add(new Ev(E_GHOST, cur, f, who + " leaves a casserole for " + p[t].name + ". Heal 1."));
                heal(t, 1, "a ghost casserole");
                break;
            }
            case BAT: {
                int t = lowest(false);
                events.add(new Ev(E_GHOST, cur, f, who + " slips " + p[t].name + " a battery."));
                gainBat(t, 1, "a ghost");
                break;
            }
            default:
                me.sMoans++;
                events.add(new Ev(E_GHOST, cur, f, who + " moans about property taxes. Nothing happens."));
                log(who + " moans about property taxes.");
        }
        if (checkOver()) return;
        advance();
    }

    private int lowest(boolean byHp) {
        int best = -1;
        for (int i = 0; i < p.length; i++) {
            if (!p[i].alive) continue;
            if (best < 0 || (byHp ? p[i].hp < p[best].hp : p[i].vp < p[best].vp)) best = i;
        }
        return best;
    }

    private boolean roll() {
        if (rollsLeft <= 0) return false;
        for (int i = 0; i < nDice; i++) if (!rolled || !keep[i]) dice[i] = rng.nextInt(6);
        if (!rolled) for (int i = 0; i < 8; i++) keep[i] = false;
        rolled = true;
        rollsLeft--;
        events.add(new Ev(E_ROLL, cur, nDice, null));
        if (rollsLeft == 0) resolveDice();
        return true;
    }

    private boolean toggle(int i) {
        if (!rolled || rollsLeft <= 0 || i < 0 || i >= nDice) return false;
        keep[i] = !keep[i];
        return true;
    }

    public int[] counts() {
        int[] c = new int[6];
        for (int i = 0; i < nDice; i++) c[dice[i]]++;
        return c;
    }

    private boolean resolveDice() {
        P me = p[cur];
        int[] c = counts();
        for (int f = ONE; f <= THREE; f++) {
            if (c[f] >= 3) {
                int v = (f + 1) + (c[f] - 3);
                if (f != THREE && me.has(Card.SPREADSHEET)) v++;
                if (f == ONE && me.has(Card.SPARE_BRICKS)) v += 2;
                gainVP(cur, v, c[f] + " " + FACE[f] + "s");
            }
        }
        if (c[HEART] > 0) {
            int wasted = c[HEART];
            if (me.loc == SUBURBS) {
                int per = me.has(Card.LAWN_MUNCHER) ? 2 : 1;
                int need = me.maxHp - me.hp;
                int used = Math.min(c[HEART], (need + per - 1) / per);
                heal(cur, Math.min(need, c[HEART] * per), "hearts");
                wasted = c[HEART] - used;
            } else if (c[HEART] < 3) {
                log("Hearts don't heal you in " + PLACE[me.loc] + ". Nobody heals in Huber.");
            }
            if (me.has(Card.LAWN_GUY) && wasted > 0) gainVP(cur, Math.min(2, wasted), "Lawn Care Guy");
            if (c[HEART] >= 3) powerPending = true;
        }
        if (c[BAT] > 0) gainBat(cur, c[BAT] + (me.has(Card.CHARGER) ? 1 : 0), "rolled");
        if (c[ZAP] > 0) {
            int dmg = c[ZAP] + (me.has(Card.MOWER) ? 1 : 0) + (eventOn(EV_AIRSHOW) ? 1 : 0);
            fxSrc = cur;
            boolean fromOutside = me.loc == SUBURBS;
            boolean any = false;
            for (int i = 0; i < p.length; i++) {
                if (i == cur || !p[i].alive) continue;
                boolean target = fromOutside ? p[i].loc != SUBURBS : p[i].loc == SUBURBS;
                if (!target) continue;
                int wasLoc = p[i].loc;
                int d = damage(i, dmg, cur, true);
                if (d > 0) {
                    any = true;
                    me.sBigZap = Math.max(me.sBigZap, d);
                    if (wasLoc == HUBER && me.has(Card.GRUDGE)) gainBat(cur, 1, "Grudge List");
                    if (fromOutside && p[i].alive && p[i].loc != SUBURBS) {
                        if (canFlee()) askYield[i] = true;
                        else log(p[i].name + " can't leave Huber" + (noFlee ? " (No Fleeing rule)." : ". Snow emergency."));
                    }
                }
            }
            fxSrc = -1;
            if (any && me.has(Card.PLUTONIUM) && me.alive) gainVP(cur, 1, "Mail-Order Plutonium");
        }
        if (checkOver()) return true;
        if (!me.alive) { endTurn(); return true; }
        for (boolean b : askYield) if (b) { phase = Phase.YIELD; return true; }
        afterYield();
        return true;
    }

    private boolean answerYield(int seat, boolean leave) {
        askYield[seat] = false;
        if (leave) leaveHuber(seat, cur);
        else { log(p[seat].name + " stays in " + PLACE[p[seat].loc] + "."); }
        if (checkOver()) return true;
        for (boolean b : askYield) if (b) return true;
        if (!p[cur].alive) { endTurn(); return true; }
        afterYield();
        return true;
    }

    private void leaveHuber(int s, int chaser) {
        P q = p[s];
        int from = q.loc;
        q.loc = SUBURBS;
        q.lease = 0;
        if (chaser >= 0) q.sFled++;
        events.add(new Ev(E_LEAVE, s, from, null));
        log(q.name + " flees " + PLACE[from] + ".");
        say(s, q.cast().leave);
        if (q.has(Card.SMOOTH)) gainVP(s, 1, "Smooth Talker");
        if (chaser >= 0 && chaser != s && q.has(Card.WRONG_HOUSE)) {
            log(p[chaser].name + " walked into the wrong house.");
            damage(chaser, 2, s, false);
        }
    }

    private void afterYield() {
        P me = p[cur];
        if (powerPending && me.alive) {
            powerPending = false;
            if (offerPowers(me)) {
                phase = Phase.POWER;
                log(me.name + " rolled three hearts and gets a personal power.");
                return;
            }
        }
        afterPower();
    }

    private boolean offerPowers(P me) {
        ArrayList<Integer> pool = new ArrayList<>();
        for (Card c : Card.ALL) if (c.owner == me.who && !me.sawIt(c)) pool.add(c.ordinal());
        if (pool.isEmpty()) return false;
        java.util.Collections.shuffle(pool, rng);
        offer[0] = pool.get(0);
        offer[1] = pool.size() > 1 ? pool.get(1) : -1;
        return true;
    }

    private boolean pickPower(int i) {
        if (i < 0 || i > 1 || offer[i] < 0) return false;
        Card c = Card.ALL[offer[i]];
        P me = p[cur];
        for (int o : offer) if (o >= 0) me.saw(Card.ALL[o]);
        offer[0] = offer[1] = -1;
        events.add(new Ev(E_POWER, cur, c.ordinal(), null));
        log(me.name + " unlocks " + c.title + ".");
        if (c.keep) me.add(c); else instant(cur, c);
        if (checkOver()) return true;
        if (!me.alive) { endTurn(); return true; }
        if (draftPending) { draftPending = false; phase = Phase.ROLL; return true; }
        afterPower();
        return true;
    }

    private void afterPower() {
        P me = p[cur];
        if (me.alive && me.loc == SUBURBS) {
            if (occupant(HUBER) < 0) enter(cur, HUBER);
            else if (roseOpen() && occupant(ROSE) < 0) enter(cur, ROSE);
        }
        phase = Phase.BUY;
    }

    private void enter(int s, int loc) {
        P q = p[s];
        q.loc = loc;
        q.sEntered++;
        events.add(new Ev(E_ENTER, s, loc, null));
        log(q.name + " moves into " + PLACE[loc] + ".");
        say(s, q.cast().enter);
        if (twoPlayer) gainBat(s, 1, "moving in"); else gainVP(s, 1, "moving in");
        if (q.has(Card.DRAWN)) gainVP(s, 1, "Drawn to the Light");
        for (int i = 0; i < p.length; i++)
            if (i != s && p[i].alive && p[i].has(Card.CLIPBOARD)) { log(p[i].name + " files a complaint."); loseVP(s, 1); }
    }

    private void takeHuber(int s) {
        P me = p[s];
        if (me.loc == HUBER) return;
        int occ = occupant(HUBER);
        if (occ >= 0) leaveHuber(occ, s);
        if (!me.alive) return;
        enter(s, HUBER);
    }

    private boolean buy(int slot) {
        if (slot < 0 || slot > 2 || market[slot] < 0) return false;
        int cid = market[slot];
        int cost = price(cur, cid);
        P me = p[cur];
        if (me.bat < cost) return false;
        Card c = Card.ALL[cid];
        me.bat -= cost;
        me.sBought++;
        events.add(new Ev(E_BUY, cur, cid, null));
        log(me.name + " buys " + c.title + " for " + cost + ".");
        market[slot] = draw();
        for (int i = 0; i < p.length; i++)
            if (i != cur && p[i].alive && p[i].has(Card.GOSSIP)) gainBat(i, 1, "Small-Town Gossip");
        if (c.keep) {
            me.add(c);
            if (c == Card.RECLINER) { me.maxHp += 2; heal(cur, 2, "the recliner"); }
        } else {
            instant(cur, c);
            discard = push(discard, cid);
        }
        if (checkOver()) return true;
        if (!me.alive) endTurn();
        return true;
    }

    private boolean sweep() {
        int cost = sweepCost(cur);
        if (p[cur].bat < cost) return false;
        p[cur].bat -= cost;
        for (int i = 0; i < 3; i++) { if (market[i] >= 0) discard = push(discard, market[i]); market[i] = -1; }
        for (int i = 0; i < 3; i++) market[i] = draw();
        events.add(new Ev(E_SWEEP, cur, cost, null));
        log(p[cur].name + " sweeps the shop for " + cost + ".");
        return true;
    }

    private boolean endTurn() {
        P me = p[cur];
        if (me.alive && noFlee && me.loc != SUBURBS && me.lease >= 1) {
            log(me.name + "'s lease is up. Evicted back to the Suburbs!");
            leaveHuber(cur, -1);
        }
        if (me.alive && me.has(Card.SOLAR) && me.bat == 0) gainBat(cur, 1, "Solar Panel");
        if (checkOver()) return true;
        int best = -1;
        for (int k = 0; k < p.length; k++) {
            int i = (cur + k) % p.length;
            if (p[i].alive && p[i].vp >= winVP && (best < 0 || p[i].vp > p[best].vp)) best = i;
        }
        if (best >= 0) { win(best); return true; }
        advance();
        return true;
    }

    private void advance() {
        int next = (cur + 1) % p.length;
        cur = next;
        if (next == 0) {
            round++;
            if (cityEvents && round % 3 == 0) { cityEvent(); if (checkOver()) return; }
        }
        beginTurn();
    }

    private void cityEvent() {
        int e;
        do e = rng.nextInt(EVENTS.length); while (e == event);
        event = e;
        eventRound = round;
        events.add(new Ev(E_EVENT, -1, e, EVENTS[e][1]));
        log("HUBER-WIDE EVENT: " + EVENTS[e][0] + ". " + EVENTS[e][1]);
        switch (e) {
            case EV_COUNCIL: for (int i = 0; i < p.length; i++) if (p[i].alive) loseBat(i, 1); break;
            case EV_CONCERT: for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].loc == SUBURBS) damage(i, 1, -1, false); break;
            case EV_CARWASH: for (int i = 0; i < p.length; i++) if (p[i].alive) gainBat(i, 2, "car wash"); break;
            case EV_SAMPLES: for (int i = 0; i < p.length; i++) if (p[i].alive) heal(i, 2, "free samples"); break;
            case EV_TAX: for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].vp >= 10) loseVP(i, 1); break;
            case EV_RESURFACE: for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].loc != SUBURBS) damage(i, 2, -1, false); break;
            default: break;   // round-long effects are checked with eventOn()
        }
    }

    private boolean checkOver() {
        if (phase == Phase.OVER) return true;
        int alive = aliveCount();
        if (alive == 0) {
            winner = -2;
            phase = Phase.OVER;
            events.add(new Ev(E_WIN, -2, 0, "Everyone's knocked out. Huber wins. Huber always wins."));
            log("Everyone's knocked out. Huber wins.");
            return true;
        }
        if (alive == 1) { for (int i = 0; i < p.length; i++) if (p[i].alive) win(i); return true; }
        return false;
    }

    private void win(int s) {
        winner = s;
        phase = Phase.OVER;
        events.add(new Ev(E_WIN, s, 0, null));
        log(p[s].name + " WINS!");
        say(s, p[s].cast().win);
    }

    // ------------------------------------------------------------------ effects

    private void instant(int s, Card c) {
        fxSrc = s;
        try { instantFx(s, c); } finally { fxSrc = -1; }
    }

    private void instantFx(int s, Card c) {
        P me = p[s];
        switch (c) {
            case FRIED_BOLOGNA: heal(s, 3, c.title); break;
            case YARD_MONTH: gainVP(s, 2, c.title); break;
            case FRONT_ROW: gainVP(s, 3, c.title); heal(s, 1, c.title); break;
            case SIREN_TEST: others(s, i -> damage(i, 2, -1, false)); break;
            case PILEUP: for (int i = 0; i < p.length; i++) if (p[i].alive && p[i].loc != SUBURBS) damage(i, 3, -1, false); break;
            case FIREWORKS: gainVP(s, 2, c.title); others(s, i -> damage(i, 3, -1, false)); break;
            case MEIJER_LOT: takeHuber(s); break;
            case ZONING: { int l = leader(s); if (l >= 0) loseVP(l, 2); break; }
            case GARAGE_SALE: gainVP(s, 1, c.title); heal(s, 1, c.title); break;
            case FISH_FRY: gainVP(s, 2, c.title); heal(s, 2, c.title); break;
            case BRICK_WINDOW: others(s, i -> { loseVP(i, 1); loseBat(i, 1); }); break;
            case I70: others(s, i -> loseBat(i, 2)); break;
            case RECONCILE: {
                int m = 0;
                for (int i = 0; i < p.length; i++) if (i != s && p[i].alive) m = Math.max(m, p[i].bat);
                if (m > me.bat) gainBat(s, m - me.bat, c.title);
                break;
            }
            case BACKGROUND: me.shield = true; log(me.name + " can't be damaged until their next turn."); break;
            case SHIP_IT: gainBat(s, 3, c.title); break;
            case BORROW: others(s, i -> { if (p[i].bat > 0) { loseBat(i, 1); gainBat(s, 1, c.title); } }); break;
            case SECOND_WIND: heal(s, 4, c.title); break;
            case GRILL: heal(s, 4, c.title); others(s, i -> heal(i, 1, "the cookout")); break;
            case LONG_STORY: others(s, i -> loseBat(i, 1)); gainVP(s, 1, c.title); break;
            case POOL_NAP: heal(s, me.maxHp, c.title); if (me.loc != SUBURBS) leaveHuber(s, -1); break;
            case BURP: others(s, i -> damage(i, 1, -1, false)); heal(s, 2, c.title); break;
            case TRACTOR: {
                int slot = -1;
                for (int k = 0; k < 3; k++) if (market[k] >= 0 && (slot < 0 || Card.ALL[market[k]].cost > Card.ALL[market[slot]].cost)) slot = k;
                if (slot >= 0) {
                    Card got = Card.ALL[market[slot]];
                    market[slot] = draw();
                    log(me.name + " beams up " + got.title + ".");
                    events.add(new Ev(E_BUY, s, got.ordinal(), null));
                    if (got.keep) { me.add(got); if (got == Card.RECLINER) { me.maxHp += 2; heal(s, 2, got.title); } }
                    else { instant(s, got); discard = push(discard, got.ordinal()); }
                }
                break;
            }
            case PROBE: { int l = leader(s); if (l >= 0) { loseVP(l, 2); gainVP(s, 2, c.title); } break; }
            case LANDLORD: others(s, i -> loseBat(i, 2)); break;
            case DUST: others(s, i -> loseVP(i, 1)); break;
            case DAD_JOKE: others(s, i -> damage(i, 1, -1, false)); break;
            default: break;
        }
    }

    private interface Each { void run(int i); }

    private void others(int s, Each e) { for (int i = 0; i < p.length; i++) if (i != s && p[i].alive) e.run(i); }

    private int leader(int s) {
        int best = -1;
        for (int i = 0; i < p.length; i++)
            if (i != s && p[i].alive && p[i].vp > 0 && (best < 0 || p[i].vp > p[best].vp)) best = i;
        return best;
    }

    /** Returns the damage actually taken. attack = from zap dice (cards and mishaps aren't attacks). */
    int damage(int s, int amt, int src, boolean attack) {
        P q = p[s];
        if (!q.alive || amt <= 0) return 0;
        if (q.shield) { log(q.name + " isn't in this scene. No damage."); return 0; }
        if (amt == 1 && q.has(Card.TINFOIL)) { log(q.name + "'s Tinfoil Hat blocks it."); return 0; }
        if (q.has(Card.FLANNEL) && q.flannelRound != round) { q.flannelRound = round; amt--; }
        if (attack && q.loc != SUBURBS && q.has(Card.LOAD_BEARING)) amt--;
        if (amt <= 0) return 0;
        int from = src >= 0 ? src : fxSrc;
        if (from >= 0 && from != s) { Ev z = new Ev(E_ZAP, from, s, null); z.c = amt; events.add(z); }
        q.hp -= amt;
        q.sTaken += amt;
        if (from >= 0 && from != s) p[from].sDealt += amt;
        events.add(new Ev(E_DMG, s, amt, null));
        if (q.has(Card.PORCH) && q.hp > 0) gainBat(s, 1, "Porch Light");
        if (attack && src >= 0 && p[src].alive && q.has(Card.GNOMES)) {
            log(q.name + "'s gnomes fight back.");
            damage(src, 1, -1, false);
        }
        if (q.hp <= 0) {
            if (q.has(Card.SPLIT) && !q.splitUsed) {
                q.splitUsed = true; q.hp = 6;
                log(q.name + " splits in two and keeps going!");
            } else {
                if (from >= 0 && from != s) p[from].sKOs++;
                ko(s);
            }
        } else if (amt >= 2 && rng.nextInt(2) == 0) say(s, q.cast().hurt);
        return amt;
    }

    private void ko(int s) {
        P q = p[s];
        q.alive = false; q.hp = 0;
        int from = q.loc;
        q.loc = SUBURBS;
        askYield[s] = false;
        for (int c : q.cards) if (!Card.ALL[c].isPower()) discard = push(discard, c);
        q.cards = new int[0];
        events.add(new Ev(E_KO, s, from, null));
        log(q.name + " is knocked out! They'll haunt the place as a ghost.");
        say(s, q.cast().ko);
        if (aliveCount() < 5) {
            int r = occupant(ROSE);
            if (r >= 0) {
                if (occupant(HUBER) < 0) { p[r].loc = HUBER; log(p[r].name + " moves from the Rose lot into Huber."); }
                else { p[r].loc = SUBURBS; log(p[r].name + " leaves the Rose lot."); }
            }
        }
    }

    void heal(int s, int amt, String why) {
        P q = p[s];
        int h = Math.min(amt, q.maxHp - q.hp);
        if (h <= 0 || !q.alive) return;
        q.hp += h;
        q.sHealed += h;
        events.add(new Ev(E_HEAL, s, h, why));
    }

    void gainVP(int s, int amt, String why) {
        if (amt <= 0 || !p[s].alive) return;
        p[s].vp += amt;
        events.add(new Ev(E_VP, s, amt, why));
        log(p[s].name + " +" + amt + " VP (" + why + ")");
    }

    void loseVP(int s, int amt) {
        int l = Math.min(amt, p[s].vp);
        if (l <= 0) return;
        p[s].vp -= l;
        events.add(new Ev(E_VP, s, -l, null));
    }

    void gainBat(int s, int amt, String why) {
        if (amt <= 0 || !p[s].alive) return;
        p[s].bat += amt;
        events.add(new Ev(E_BAT, s, amt, why));
    }

    void loseBat(int s, int amt) {
        int l = Math.min(amt, p[s].bat);
        if (l <= 0) return;
        p[s].bat -= l;
        events.add(new Ev(E_BAT, s, -l, null));
    }

    private void say(int s, String[] lines) {
        events.add(new Ev(E_SAY, s, 0, lines[rng.nextInt(lines.length)]));
    }

    private void log(String t) { events.add(new Ev(E_LOG, -1, 0, t)); }

    // ------------------------------------------------------------------ deck

    private int draw() {
        if (deckTop >= deck.length) {
            if (discard.length == 0) return -1;
            deck = discard; discard = new int[0]; deckTop = 0;
            shuffle(deck);
        }
        return deck[deckTop++];
    }

    private void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) { int j = rng.nextInt(i + 1); int t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    static int[] push(int[] a, int v) {
        int[] b = java.util.Arrays.copyOf(a, a.length + 1);
        b[a.length] = v;
        return b;
    }

    /** Hand the queued events to the caller and start a fresh list. */
    public ArrayList<Ev> takeEvents() {
        ArrayList<Ev> e = events;
        events = new ArrayList<>();
        return e;
    }
}
