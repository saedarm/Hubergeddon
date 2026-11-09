package huber;

/**
 * Every card in the game. Shop cards (owner -1) are bought with batteries.
 * Personal powers (owner = a Cast ordinal) are drawn when you roll three or more hearts.
 * Effects live in Game.java; search for the constant name.
 */
public enum Card {
    // ---- Shop: one-shot (resolve and gone) ----
    FRIED_BOLOGNA("Fried Bologna Sandwich", 3, false, 2, "Heal 3. Works in Huber too."),
    YARD_MONTH("Yard of the Month", 4, false, 2, "+2 VP. The sign is laminated."),
    FRONT_ROW("Front Row at the Rose", 6, false, 1, "+3 VP and heal 1."),
    SIREN_TEST("Wednesday Noon Siren Test", 5, false, 1, "Everyone else takes 2 damage."),
    PILEUP("Old Troy Pike Pileup", 4, false, 1, "Whoever is in Huber or the Rose lot takes 3 damage. Yes, even you."),
    FIREWORKS("Fourth of July Fireworks", 7, false, 1, "+2 VP. Everyone else takes 3 damage."),
    MEIJER_LOT("Cut Through the Meijer Lot", 4, false, 1, "Move into Huber right now. Whoever's there has to leave."),
    ZONING("Zoning Board Complaint", 3, false, 2, "Whoever has the most VP (not you) loses 2 VP."),
    GARAGE_SALE("Garage Sale Haul", 2, false, 2, "+1 VP and heal 1."),
    FISH_FRY("Friday Fish Fry", 5, false, 1, "+2 VP and heal 2."),
    BRICK_WINDOW("Brick Through the Window", 4, false, 1, "Everyone else loses 1 VP and 1 battery. Plenty of bricks around."),
    I70("I-70 Construction Season", 3, false, 1, "Everyone else loses 2 batteries."),

    // ---- Shop: keepers (stay in front of you) ----
    THIRD_ARM("Third Arm from Hangar 18", 7, true, 1, "Roll 1 extra die."),
    BUCKEYE("Lucky Buckeye", 5, true, 1, "1 extra reroll every turn."),
    TINFOIL("Tinfoil Hat", 4, true, 1, "If you'd take exactly 1 damage, take none."),
    MOWER("Riding Mower", 5, true, 1, "If you roll any zaps, deal 1 extra damage."),
    SOLAR("Solar Panel on the Garage", 3, true, 1, "End your turn with 0 batteries? Gain 1."),
    COUPONS("Coupon Clipper", 4, true, 1, "Cards cost you 1 less."),
    LAWN_GUY("Lawn Care Guy", 4, true, 1, "Hearts you can't use to heal are worth 1 VP each (max 2 a turn)."),
    RECLINER("Double-Wide Recliner", 5, true, 1, "+2 max health. Heal 2 now."),
    SPARE_BRICKS("Spare Bricks", 3, true, 1, "Three 1s are worth 2 extra VP. One brick, two brick..."),
    GOSSIP("Small-Town Gossip", 4, true, 1, "Whenever someone else buys a card, gain 1 battery."),
    CHARGER("Alien Battery Charger", 4, true, 1, "If you roll any batteries, gain 1 more."),
    NIGHT_OWL("Night Owl", 4, true, 1, "+1 VP when you start your turn in Huber or the Rose lot."),
    CORNFIELD("Cornfield Hideout", 4, true, 1, "Start your turn out in the Suburbs? Heal 1."),
    GRUDGE("Grudge List", 3, true, 1, "When you zap whoever is in Huber, gain 1 battery."),
    PLUTONIUM("Mail-Order Plutonium", 6, true, 1, "Whenever your zaps deal damage, +1 VP."),
    MECHANIC("Cousin Who's a Mechanic", 3, true, 1, "Sweeping the shop costs you 1 instead of 2."),
    GNOMES("Lawn Gnome Army", 5, true, 1, "When someone zaps you, they take 1 damage."),

    // ---- Personal powers (owner = Cast ordinal) ----
    // Sam
    SPREADSHEET("Pivot Table Wizard", 0, true, 0, 1, "Sets of 1s and 2s score 1 extra VP. Small items add up."),
    RECONCILE("Reconcile the Books", 0, false, 0, 1, "Your batteries match whoever has the most."),
    BACKGROUND("Background Extra", 0, false, 0, 1, "Nobody can damage you until your next turn. You're not in this scene."),
    SHIP_IT("Ship It on a Friday", 0, false, 0, 1, "+3 batteries."),
    // Jack
    FLANNEL("Flannel Armor", 0, true, 1, 1, "The first damage you take each round is 1 less."),
    SMOOTH("Smooth Talker", 0, true, 1, 1, "+1 VP whenever you leave Huber."),
    BORROW("Borrow Without Asking", 0, false, 1, 1, "Take 1 battery from everyone."),
    SECOND_WIND("Second Wind", 0, false, 1, 1, "Heal 4."),
    // Uncle Rob
    REDS_FAN("Reds Fan Since Birth", 0, true, 2, 1, "Used to losing. +1 VP when you start a turn with 5 health or less."),
    GRILL("Fire Up the Grill", 0, false, 2, 1, "Heal 4. Everyone else heals 1. It's a cookout."),
    LONG_STORY("Tells a Long Story", 0, false, 2, 1, "Everyone else loses 1 battery waiting for it to end. You gain 1 VP."),
    POOL_NAP("Pool Nap", 0, false, 2, 1, "Heal to full. If you're in Huber, you leave it."),
    // Glorp
    LAWN_MUNCHER("Lawn Muncher", 0, true, 3, 1, "Every heart you heal with heals 1 extra."),
    SPLIT("Split in Two", 0, true, 3, 1, "Once, when you'd be knocked out, drop to 6 health instead."),
    BURP("Fertilizer Burp", 0, false, 3, 1, "Everyone else takes 1 damage. Heal 2."),
    PHOTOSYNTH("Photosynthesis", 0, true, 3, 1, "+1 battery at the start of every turn. +1 more if you're out in the Suburbs."),
    // Commander Zib
    TRACTOR("Tractor Beam", 0, false, 4, 1, "Beam the priciest card in the shop to you. Free."),
    CLIPBOARD("Clipboard of Complaints", 0, true, 4, 1, "Whenever someone else moves into Huber, they lose 1 VP."),
    PROBE("Probe the Leader", 0, false, 4, 1, "Whoever has the most VP (not you) loses 2. You gain 2."),
    SAUCER("Overclocked Saucer", 0, true, 4, 1, "Roll 1 extra die while you're in Huber or the Rose lot."),
    // Unit 10,708
    WRONG_HOUSE("Wrong House", 0, true, 5, 1, "When you leave Huber, whoever chased you out takes 2 damage. Wrong ranch, buddy."),
    LANDLORD("Hedge Fund Landlord", 0, false, 5, 1, "Rent's due. Everyone else loses 2 batteries."),
    LOAD_BEARING("Load-Bearing Walls", 0, true, 5, 1, "In Huber or the Rose lot, take 1 less damage from zaps."),
    BUILT_HERE("Built Here in 1962", 0, true, 5, 1, "Only-in-Huber mishaps don't affect you. You ARE Huber."),
    // Moth Dad
    DRAWN("Drawn to the Light", 0, true, 6, 1, "+1 VP whenever you move into Huber or the Rose lot."),
    DUST("Wing Dust", 0, false, 6, 1, "Everyone else loses 1 VP."),
    DAD_JOKE("Dad Joke", 0, false, 6, 1, "Everyone else takes 1 damage. It was that bad."),
    PORCH("Porch Light", 0, true, 6, 1, "Whenever you take damage, gain 1 battery.");

    public static final Card[] ALL = values();

    public final String title, text;
    public final int cost, copies, owner;
    public final boolean keep;

    Card(String title, int cost, boolean keep, int copies, String text) {
        this(title, cost, keep, -1, copies, text);
    }

    Card(String title, int cost, boolean keep, int owner, int copies, String text) {
        this.title = title; this.cost = cost; this.keep = keep; this.owner = owner; this.copies = copies; this.text = text;
    }

    public boolean isPower() { return owner >= 0; }
}
