# Hubergeddon: Come Grow With Us

A dice-and-cards brawl for 2–6 players, set in Huber Heights, Ohio, "America's Largest Community of Brick Homes." Aliens, you, Jack and Uncle Rob fight over Huber, a place nobody should actually want. Desktop game (Windows, Mac, Linux) built with libGDX. Play against bots, or online with friends plus bots.

The rules are inspired by Richard Garfield's *King of Tokyo*. Every name, card, power, joke and piece of art here is new.

## How a game works

- **Goal:** get 20 VP at the end of a turn, or be the last one standing.
- **Your turn:** roll 6 dice up to 3 times. Between rolls, click the dice you want to keep (or press 1–8). Space rolls, Enter stops.
- **The dice:**

| Face | What it does |
| --- | --- |
| 1, 2, 3 | Three of a kind scores that many VP. Each extra matching die is +1. |
| Zap | 1 damage each. From the Suburbs you hit whoever is in Huber. From Huber you hit everyone in the Suburbs. |
| Heart | Heal 1 (max 10). **Hearts don't heal you in Huber.** Three hearts unlocks one of your character's personal powers. |
| Battery | Money for the shop. |

### Huber is cursed

- If Huber is empty when you finish rolling, you have to move in (+1 VP).
- Every turn you **start** in Huber: +2 VP, then an **Only in Huber** mishap. There are 14 of them. Most cost health, batteries, VP or dice:
  - lost among identical brick ranches
  - stuck on Old Troy Pike behind 26,500 other cars
  - looking for downtown Huber (there isn't one)
  - rent's due to a hedge fund
  - parked at the Rose Music Center and walked a mile
  - went around the roundabout four times
  - and more
- Got zapped while in Huber? You can flee to the Suburbs, and whoever zapped you moves in.
- With 5–6 players still in, the **Rose Music Center lot** is a second Huber spot that works the same way.

### Huber-wide events

Every 3 rounds something hits the whole table, shown with its own picture:

| Event | What it does |
| --- | --- |
| City Council Meeting | Everyone loses 1 battery. |
| Snow Emergency Level 3 | Nobody can leave Huber this round. |
| Concert Night at the Rose | Everyone in the Suburbs loses 1 health. |
| Grand Opening: Another Car Wash | Everyone gets 2 batteries. |
| Power Outage | Everyone rolls 1 fewer die this round. |
| Free Sample Day | Everyone heals 2, even in Huber. |
| Property Tax Reassessment | Everyone with 10+ VP loses 1 VP. |
| Air Show at Wright-Patt | Zaps deal 1 extra damage this round. |
| Citywide Garage Sale | Shop cards cost 1 less this round. |
| Old Troy Pike Resurfacing | Whoever is in Huber or the Rose lot loses 2 health. |

### Knocked out? You're a ghost

You don't sit out. Each of your turns you haunt the table automatically. You might rattle the bricks (1 damage to everyone in Huber), leave a casserole for the weakest player (heal 1), slip the poorest player a battery, or moan about property taxes. Ghosts can't win.

### The shop

Three cards are for sale. **Keep** cards stay with you. **One-shot** cards happen once. **Sweep** costs 2 batteries and swaps all three cards for new ones.

### Game options (in the lobby)

The host (or lobby leader on a server) clicks an option to change it. Everyone else sees the current setting.

| Option | Choices |
| --- | --- |
| First to | 20 VP, 15 (quick), 25 (long) |
| Fleeing | Allowed, or NOT ALLOWED. With no fleeing, your lease runs out at the end of your first full turn in Huber and you're evicted. |
| Power Draft | Everyone picks a personal power before their first roll. |
| City events | On or off. |
| Bots | Easy (makes mistakes), Normal, or Uncle Rob After Three Beers (the sharpest). |
| Turn timer | 60s, 90s or off. Online only. Run out of time and autopilot finishes your turn. |
| Same character twice | Lets two players pick the same character (two Uncle Robs). |
| Fast bots | Bots take their turns faster. |

### End of game: the Huber Report Card

The game hands out joke awards based on what actually happened:

- Landlord of the Year (most turns in Huber)
- Only in Huber (most mishaps)
- Most Likely to Flee
- Biggest Single Zap
- Demolition Crew (most damage dealt)
- Punching Bag (most damage taken)
- Grill Master (most healing)
- Shopaholic (most cards bought)
- Undertaker (most knockouts)
- Property Tax Complainer (most ghostly moans)

Each computer also saves your career record: wins, games played, and your head-to-head record against each friend by name.

### What you see

- Portraits fly into Huber, bounce on landing, and spin back out when someone flees.
- Zap bolts arc from the attacker to each target.
- Hearts and batteries fly from the dice to your card. Bought cards slide over to the buyer.
- Knocked-out players float up as ghosts. Big hits shake the screen.
- Mishaps and city events each get their own illustration.
- Characters say one-liners in speech bubbles.
- Two music loops: elevator-music bossa in the menus and a funk groove at the table.

### Turn order catch-up

Going first is an advantage, so the 2nd player starts with 1 battery, the 3rd with 2, and so on.

## Characters

Each character has 4 personal powers. When you roll three or more hearts, you see 2 of the ones you haven't drawn yet and keep 1.

| Character | Who |
| --- | --- |
| Sam | Business analyst who builds ridiculous things |
| Jack | Little brother. Big flannel. |
| Uncle Rob | Reds fan. Grill master. Pool nap champion. |
| Glorp | Came for the lawn fertilizer. Stayed. |
| Commander Zib | Escaped Hangar 18. Files complaints. |
| Unit 10,708 | The brick ranch Huber Homes never meant to build. |
| Moth Dad | Drawn to the light. Tells jokes about it. |

**Sam**
- Pivot Table Wizard (keep): Sets of 1s and 2s score 1 extra VP. Small items add up.
- Reconcile the Books (one-shot): Your batteries match whoever has the most.
- Background Extra (one-shot): Nobody can damage you until your next turn. You're not in this scene.
- Ship It on a Friday (one-shot): +3 batteries.

**Jack**
- Flannel Armor (keep): The first damage you take each round is 1 less.
- Smooth Talker (keep): +1 VP whenever you leave Huber.
- Borrow Without Asking (one-shot): Take 1 battery from everyone.
- Second Wind (one-shot): Heal 4.

**Uncle Rob**
- Reds Fan Since Birth (keep): Used to losing. +1 VP when you start a turn with 5 health or less.
- Fire Up the Grill (one-shot): Heal 4. Everyone else heals 1. It's a cookout.
- Tells a Long Story (one-shot): Everyone else loses 1 battery waiting for it to end. You gain 1 VP.
- Pool Nap (one-shot): Heal to full. If you're in Huber, you leave it.

**Glorp**
- Lawn Muncher (keep): Every heart you heal with heals 1 extra.
- Split in Two (keep): Once, when you'd be knocked out, drop to 6 health instead.
- Fertilizer Burp (one-shot): Everyone else takes 1 damage. Heal 2.
- Photosynthesis (keep): +1 battery at the start of every turn. +1 more if you're out in the Suburbs.

**Commander Zib**
- Tractor Beam (one-shot): Beam the priciest card in the shop to you. Free.
- Clipboard of Complaints (keep): Whenever someone else moves into Huber, they lose 1 VP.
- Probe the Leader (one-shot): Whoever has the most VP (not you) loses 2. You gain 2.
- Overclocked Saucer (keep): Roll 1 extra die while you're in Huber or the Rose lot.

**Unit 10,708**
- Wrong House (keep): When you leave Huber, whoever chased you out takes 2 damage. Wrong ranch, buddy.
- Hedge Fund Landlord (one-shot): Rent's due. Everyone else loses 2 batteries.
- Load-Bearing Walls (keep): In Huber or the Rose lot, take 1 less damage from zaps.
- Built Here in 1962 (keep): Only-in-Huber mishaps don't affect you. You ARE Huber.

**Moth Dad**
- Drawn to the Light (keep): +1 VP whenever you move into Huber or the Rose lot.
- Wing Dust (one-shot): Everyone else loses 1 VP.
- Dad Joke (one-shot): Everyone else takes 1 damage. It was that bad.
- Porch Light (keep): Whenever you take damage, gain 1 battery.

## Shop cards

| Card | Cost | Type | What it does |
| --- | --- | --- | --- |
| Fried Bologna Sandwich | 3 | One-shot | Heal 3. Works in Huber too. (x2 in deck) |
| Yard of the Month | 4 | One-shot | +2 VP. The sign is laminated. (x2 in deck) |
| Front Row at the Rose | 6 | One-shot | +3 VP and heal 1. |
| Wednesday Noon Siren Test | 5 | One-shot | Everyone else takes 2 damage. |
| Old Troy Pike Pileup | 4 | One-shot | Whoever is in Huber or the Rose lot takes 3 damage. Yes, even you. |
| Fourth of July Fireworks | 7 | One-shot | +2 VP. Everyone else takes 3 damage. |
| Cut Through the Meijer Lot | 4 | One-shot | Move into Huber right now. Whoever's there has to leave. |
| Zoning Board Complaint | 3 | One-shot | Whoever has the most VP (not you) loses 2 VP. (x2 in deck) |
| Garage Sale Haul | 2 | One-shot | +1 VP and heal 1. (x2 in deck) |
| Friday Fish Fry | 5 | One-shot | +2 VP and heal 2. |
| Brick Through the Window | 4 | One-shot | Everyone else loses 1 VP and 1 battery. Plenty of bricks around. |
| I-70 Construction Season | 3 | One-shot | Everyone else loses 2 batteries. |
| Third Arm from Hangar 18 | 7 | Keep | Roll 1 extra die. |
| Lucky Buckeye | 5 | Keep | 1 extra reroll every turn. |
| Tinfoil Hat | 4 | Keep | If you'd take exactly 1 damage, take none. |
| Riding Mower | 5 | Keep | If you roll any zaps, deal 1 extra damage. |
| Solar Panel on the Garage | 3 | Keep | End your turn with 0 batteries? Gain 1. |
| Coupon Clipper | 4 | Keep | Cards cost you 1 less. |
| Lawn Care Guy | 4 | Keep | Hearts you can't use to heal are worth 1 VP each (max 2 a turn). |
| Double-Wide Recliner | 5 | Keep | +2 max health. Heal 2 now. |
| Spare Bricks | 3 | Keep | Three 1s are worth 2 extra VP. One brick, two brick... |
| Small-Town Gossip | 4 | Keep | Whenever someone else buys a card, gain 1 battery. |
| Alien Battery Charger | 4 | Keep | If you roll any batteries, gain 1 more. |
| Night Owl | 4 | Keep | +1 VP when you start your turn in Huber or the Rose lot. |
| Cornfield Hideout | 4 | Keep | Start your turn out in the Suburbs? Heal 1. |
| Grudge List | 3 | Keep | When you zap whoever is in Huber, gain 1 battery. |
| Mail-Order Plutonium | 6 | Keep | Whenever your zaps deal damage, +1 VP. |
| Cousin Who's a Mechanic | 3 | Keep | Sweeping the shop costs you 1 instead of 2. |
| Lawn Gnome Army | 5 | Keep | When someone zaps you, they take 1 damage. |

## Playing online

There are two ways to play online. Either way, everyone runs the same Hubergeddon jar and clicks **Join Online**.

### Option 1: a server on Fly.io (recommended)

A small always-available server that sleeps when nobody's playing and wakes when someone joins. Nobody's PC has to host, and there's no router setup. Everyone joins `your-app-name.fly.dev`.

**Online extras (server or host):**
- **Chat:** press T, type, press Enter. Messages show in the log and as a bubble over your card.
- **Rejoin:** if your connection drops mid-game, the game reconnects you to your own seat automatically. A bot covers your seat in the meantime.
- **Turn timer:** if someone goes idle, autopilot finishes their turn.

**How the lobby works on the server:**
- The first person to connect is the **lobby leader**. The leader gets the host buttons: add, change and remove bots, fast bots, START, and Play Again.
- If the leader leaves, the next person in the lobby takes over.
- The server runs one game at a time. If someone tries to join a game in progress, they're told to try again when it's over.
- When everyone leaves, the server resets for the next group.

**Deploy it (one time).** Run these from the project folder:

1. **Install the Fly command-line tool.** On Windows, in PowerShell:
   ```
   pwsh -Command "iwr https://fly.io/install.ps1 -useb | iex"
   ```
   (If `pwsh` isn't found, use `powershell` instead.) On Mac or Linux: `curl -L https://fly.io/install.sh | sh`
2. **Sign in or sign up:** `fly auth login`
3. **Pick an app name** nobody else has taken, and put it on the `app = ` line in `fly.toml`. It ships as `hubergeddon-sam`. Then create the app:
   ```
   fly apps create hubergeddon-sam
   ```
4. **Deploy with exactly one machine.** Two machines would split players across two separate servers.
   ```
   fly deploy --ha=false
   ```
5. **Check the addresses:** run `fly ips list`. You want an IPv6 address and a **shared** IPv4 address. If there's no v4 line, run `fly ips allocate-v4 --shared`.
6. **Play.** Everyone clicks **Join Online** and types `hubergeddon-sam.fly.dev`. The first connect after the server has been idle takes a few seconds while it wakes up.

**Why the server uses TLS:** Fly's free shared IPv4 address only accepts non-web ports if Fly handles the encryption. So `fly.toml` turns on Fly's TLS handler for port 7777, and the game encrypts its connection to any `.fly.dev` address automatically. Fly decrypts it and passes plain game messages to the server. To force a mode, type `tls://address` or `tcp://address`.

**If joining fails with a TLS error:** use a dedicated IPv4 address instead. Fly bills these monthly.
1. Run `fly ips allocate-v4`.
2. Release the shared one with `fly ips release <shared ip>`.
3. Delete the `handlers = ["tls"]` line in `fly.toml`.
4. Run `fly deploy --ha=false`.
5. Join with `tcp://hubergeddon-sam.fly.dev`.

**Day to day:**
- `fly logs` shows who joined, game starts and resets.
- `fly status` shows whether the machine is running or asleep.
- After changing game code, run `fly deploy --ha=false` again and send everyone the new jar. A server and a jar from different versions refuse to connect, and say so.

**Auto-deploy from GitHub (optional):** `.github/workflows/fly-deploy.yml` redeploys the server every time you push to `main`. One-time setup: run `fly tokens create deploy` and save the output as a repo secret named `FLY_API_TOKEN` (GitHub repo > Settings > Secrets and variables > Actions).

**Running the server anywhere else:** `gradlew core:serverJar` builds `core/build/libs/Hubergeddon-server.jar`. Start it with `java -jar Hubergeddon-server.jar` (port 7777, or the `PORT` environment variable, or a port argument). It needs no graphics.

### Option 2: host from your own PC

1. **Host:** click **Host Online Game**. The lobby shows your address and port 7777. Add bots, pick your character, and press **START** when your friends are in.
2. **Friends:** click **Join Online** and type the host's address:
   - **Same Wi-Fi:** the host's `192.168...` address.
   - **Over the internet:** everyone installs [Tailscale](https://tailscale.com) and uses the host's `100.x` address. Or the host forwards TCP port 7777 on their router, and friends use the host's public IP.

The first time you host on Windows, it asks whether to allow Java through the firewall. Allow it.

### How it works

Whoever hosts (the Fly server or your PC) runs the real game and the bots. Players send their clicks, and the host sends the whole game state back after every change. So everyone sees the same dice and nobody's copy can drift.
- **Connection:** plain TCP sockets, one JSON message per line (`Net.java`, `Session.java`), with a ping every 15 seconds so idle connections stay open.
- **Drop-outs:** if a player drops mid-game, a bot takes their seat.

## Running it

You need Java 17 or newer. Android Studio and IntelliJ both include one.

**Easiest:** double-click `Play Hubergeddon.bat`. The first time, it builds the game, which takes a minute or two. After that it starts right away.

**From a terminal**, in the project folder:

```
gradlew lwjgl3:run
```

**One file to share with friends:**

```
gradlew lwjgl3:dist
java -jar lwjgl3/build/libs/Hubergeddon.jar
```

The jar has everything inside it and runs anywhere Java 17+ is installed. Send it to Jack and Uncle Rob so they can join.

### Keys

| Key | What it does |
| --- | --- |
| Space | Roll |
| 1–8 | Keep or unkeep a die |
| Enter | Stop rolling / end turn |
| T | Chat (online) |
| F | Fast bots (host or leader) |
| M | Mute everything |
| N | Music on/off |
| Esc | Pause / back |

## Project layout

```
core/      all game code, plus the dedicated server (Server.java)
lwjgl3/    desktop launcher and the runnable-jar build
Dockerfile, fly.toml   the Fly.io server
assets/    img/ (generated art), sfx/ and music/ (generated audio), fonts/ (Poppins, SIL OFL)
tools/     make_art.py, make_vignettes.py, make_sfx.py, make_music.py, photos/ (source photos for the human portraits)
```

| File | What it does |
| --- | --- |
| `Game.java` | All the rules as plain Java: dice, Huber and its mishaps, cards, powers, damage, knockouts, ghosts. Draws nothing, so it runs headless and goes over the network as JSON. |
| `Card.java` | Every shop card and personal power: name, cost, keep or one-shot, owner and text. The effects live in `Game.java`; search for the constant name. |
| `Cast.java` | The 7 characters, their colours and their one-liners. |
| `Bot.java` | Computer players. They decide which dice to keep by simulating the rerolls. They flee Huber based on how much damage they expect before their next turn. Each has a personality: Balanced, Berserker, Hoarder, Sprinter or Coward. |
| `Net.java` | TCP host and client, one JSON message per line. |
| `Session.java` | Local, host, client or dedicated server. The lobby, seats, the lobby leader, sending moves, and running the bots. |
| `Server.java` | Dedicated server with no graphics (for Fly.io). Runs `Session` in a loop. |
| `HuberGame.java` | Title, join, lobby and table screens, plus the effects, speech bubbles, banners and sounds. |
| `Ui.java` | Fonts, panels and buttons on one SpriteBatch, on a 1280x720 virtual screen. |
| `Sim.java` | Headless balance check. |

## Changing things

- **Card text and costs:** `Card.java`. Effects: `instant()` and `resolveDice()` in `Game.java`.
- **Mishaps and city events:** the `MISHAPS` and `EVENTS` lists in `Game.java`, with their effects in `mishap()` and `cityEvent()`. Their pictures come from `tools/make_vignettes.py`, in the same order.
- **Music:** `tools/make_music.py` (needs numpy and ffmpeg), or drop your own `lobby.ogg` / `game.ogg` into `assets/music/`.
- **One-liners:** `Cast.java`.
- **Photos:** replace a file in `tools/photos/` and adjust its crop box in `PHOTOS` at the top of `tools/make_art.py`. Then run `python3 tools/make_art.py` (needs Pillow).
- **Sounds:** they're synthesized placeholders. Edit `tools/make_sfx.py` and run it (needs numpy), or drop your own `.wav` with the same name into `assets/sfx/`.
- **Balance:** `gradlew sim -Pgames=5000` runs that many bot games and prints win rates.

### Last balance run (3,000 bot games, city events on)

- **Game length:** about 9 rounds at every table size.
- **Knockout wins:** 5–9% of games at 4–6 players, 50% at 2 players. The rest are won on VP.
- **Characters:** win rates range from 23% to 26%.
- **Going first:** wins 31% of games, against a fair share of about 29%.
- **Bot styles:** Sprinter bots (chasing number sets) win the most and Berserkers the least, so the bots don't use zaps as well as people will.
- **No Fleeing mode:** about 1 in 10 games end in a knockout, and going first wins 26%.
- `gradlew sim -Pgames="3000 noflee"` (also `draft`, `noevents`, `vp=15`) tests the options.

The bots haven't been tuned against real players. Tune from actual play.

## Not in this version

- Quick-chat taunts with character voice lines
- Illustrations on the shop cards (they're text only)
- More than one game at a time on the server (rooms)
