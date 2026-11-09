package huber;

/** The playable characters. Humans use photo portraits; aliens are drawn by tools/make_art.py. */
public enum Cast {
    SAM("Sam", "Business analyst who builds ridiculous things", 0x3B82F6, true,
        new String[]{"Per my last email, I live here now.", "Huber? I'll reconcile it later."},
        new String[]{"I'm escalating this.", "Logging out of Huber. Permanently."},
        new String[]{"That's going in the incident report.", "Ow. Variance detected."},
        new String[]{"The spreadsheet... balanced..."},
        new String[]{"Closed the books on all of you."}),
    JACK("Jack", "Little brother. Big flannel.", 0xB45309, true,
        new String[]{"Moving in. Don't tell Mom.", "Nice place. Smells like bricks."},
        new String[]{"I was never here.", "Taking the long way home. Very long."},
        new String[]{"Hey! I'm telling!", "That's a flannel foul."},
        new String[]{"Tell Sam he still owes me ten bucks."},
        new String[]{"Little brother wins again."}),
    ROB("Uncle Rob", "Reds fan. Grill master. Pool nap champion.", 0xC8102E, true,
        new String[]{"Pull up a lawn chair, boys.", "Been through worse. I'm a Reds fan."},
        new String[]{"I'm going to go check on the grill.", "Somebody else can have it."},
        new String[]{"I've seen the bullpen do worse.", "That one stung. Like a Reds September."},
        new String[]{"Wake me up for the ninth inning."},
        new String[]{"Burgers are on me. Not really."}),
    GLORP("Glorp", "Came for the lawn fertilizer. Stayed.", 0x22C55E, false,
        new String[]{"GLORP GROW WITH US.", "Many lawns. All Glorp's."},
        new String[]{"Glorp... ooze elsewhere.", "Too much brick. Glorp out."},
        new String[]{"Glorp jiggle in pain.", "Rude!"},
        new String[]{"Glorp return to compost."},
        new String[]{"ALL LAWNS BELONG TO GLORP."}),
    ZIB("Commander Zib", "Escaped Hangar 18. Files complaints.", 0x94A3B8, false,
        new String[]{"Earth. Ohio. Huber. Disappointing.", "I'll be speaking to your HOA."},
        new String[]{"Strategic retreat. Logged.", "This is a tactical withdrawal."},
        new String[]{"I am writing your name down.", "Unacceptable."},
        new String[]{"Return me... to Hangar 18..."},
        new String[]{"Your planet's suburbs are mine."}),
    UNIT("Unit 10,708", "The brick ranch Huber Homes never meant to build.", 0xB91C1C, false,
        new String[]{"Home sweet home. Literally.", "I'm basically zoned for this."},
        new String[]{"Relocating. Foundation first.", "Gotta go. Gutters need cleaning."},
        new String[]{"My shingles!", "You chipped my brick!"},
        new String[]{"Condemned..."},
        new String[]{"Come grow with ME."}),
    MOTH("Moth Dad", "Drawn to the light. Tells jokes about it.", 0xA78BFA, false,
        new String[]{"Ooh. A porch light. Two porch lights!", "Hi Huber, I'm Dad."},
        new String[]{"I'm moth-ing on out of here.", "Lights out, kids."},
        new String[]{"That really dusted me.", "Not the wings!"},
        new String[]{"Go toward... the light..."},
        new String[]{"I'm proud of you all. Mostly me."});

    public static final Cast[] ALL = values();

    public final String name, tagline;
    public final int color;
    public final boolean human;
    public final String[] enter, leave, hurt, ko, win;

    Cast(String name, String tagline, int color, boolean human,
         String[] enter, String[] leave, String[] hurt, String[] ko, String[] win) {
        this.name = name; this.tagline = tagline; this.color = color; this.human = human;
        this.enter = enter; this.leave = leave; this.hurt = hurt; this.ko = ko; this.win = win;
    }

    public String portrait() { return "img/portrait_" + name().toLowerCase() + ".png"; }
}
