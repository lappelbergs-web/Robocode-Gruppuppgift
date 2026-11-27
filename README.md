# Robocode – Gruppuppgift

Ett lag med fyra robotar som strider tillsammans i [Robocode](https://robocode.sourceforge.io/).
Varje gruppmedlem har en egen robot med en egen roll. Robotarna samarbetar genom att skicka
meddelanden till varandra under striden.

## Laget

| Robot | Ägare | Roll | Radar | Startenergi | Färg |
|---|---|---|---|---|---|
| `PeterTeamRobot` | Peter | **Ledare** | Ja | 200 | Grå/röd |
| `MinnaTeamRobot` | Minna | **Tank** (Droid) | Nej | 120 | Lila/rosa |
| `JonTeamRobot` | Jon | **Prickskytt** | Ja | 100 | Grön |
| `LeoTeamRobot` | Leo | **Livvakt** | Ja | 100 | Blå |

- **Ledare (Peter):** står först i laget och får därför 200 i energi. Spanar och cirklar runt målet på medelavstånd.
- **Tank (Minna):** en *Droid*, alltså en robot utan radar som i stället får 120 i energi. Ser inte fienderna själv utan får deras positioner från lagkamraterna. Slåss på nära håll (cirka 200 pixlar) med hårda skott.
- **Prickskytt (Jon):** håller sig långt bort (cirka 400 pixlar), skjuter snabba kulor bara när kanonen pekar exakt rätt och byter riktning ofta så att den är svår att träffa.
- **Livvakt (Leo):** håller sig nära ledaren, på den sida där hotet finns, och skjuter på den fiende som är närmast ledaren. Blir en vanlig krigare om ledaren dör.

## Så samarbetar robotarna

Alla robotar ärver från `TeamRobot` och skickar två sorters meddelanden till laget:

| Meddelande | Innehåll | Skickas |
|---|---|---|
| `TeammatePosition` | Robotens egen position | Varje tur |
| `EnemyPosition` | Fiendens position, riktning, fart och energi | När en robot med radar ser en fiende |

Det används till tre saker:

1. **Gemensamt mål ("focus fire"):** Ledaren, Tanken och Prickskytten skjuter alla på fienden med lägst energi, så att laget slår ut en motståndare i taget.
2. **Inga skott på lagkamrater ("friendly fire"):** en robot siktar aldrig på en lagkamrat (`isTeammate`) och håller elden om en lagkamrat står inom 40 pixlar från skottlinjen.
3. **Förutsägelse:** robotarna siktar dit fienden kommer att vara när kulan når fram, inte dit den är nu.

> **Viktigt:** De två meddelandeklasserna är gemensamma för alla robotar. Ändras något i dem måste
> det göras på `main` och sedan tas in i allas grenar, annars slutar robotarna förstå varandra.

## Projektstruktur

```
Robocode-Gruppuppgift/
├── .idea/                    IntelliJ-inställningar (delas så att projektet går att öppna direkt)
├── MinRobot.iml              IntelliJ-modulen
├── README.md
└── src/
    └── grupp/                Alla robotar och meddelandeklasser ligger här
        ├── TeammatePosition.java   (gemensam)
        ├── EnemyPosition.java      (gemensam)
        ├── PeterTeamRobot.java     (peter-branch)
        ├── MinnaTeamRobot.java     (minna-branch)
        ├── JonTeamRobot.java       (jon-branch)
        └── LeoTeamRobot.java       (leo-branch)
```

Mappen `out/` skapas när projektet byggs och ska **inte** läggas upp på GitHub (den står i `.gitignore`).

## Kom igång

### 1. Installera
- **Robocode 1.12.0:** kör `robocode-1.12.0-setup.jar` och installera till `C:\robocode`.
- **JDK 25** (Java Development Kit). IntelliJ kan ladda ner den åt dig om den saknas.

### 2. Öppna projektet i IntelliJ
1. Klona repot: `git clone https://github.com/lappelbergs-web/Robocode-Gruppuppgift.git`
2. **File → Open** och välj mappen `Robocode-Gruppuppgift`.
3. Har du installerat Robocode någon annanstans än `C:\robocode`: gå till
   **File → Project Structure → Libraries → libs** och peka om till din mapp `robocode\libs`.
4. **Build → Build Project** (Ctrl+F9). Nu skapas `out/production/MinRobot/grupp/` med alla `.class`-filer.

### 3. Koppla Robocode till projektet
I Robocode: **Options → Preferences → Development Options → Add** och välj
`...\Robocode-Gruppuppgift\out\production\MinRobot`.

### 4. Skapa laget
I Robocode: **Robot → Team → Create Team** och lägg till robotarna i den här ordningen
(den första blir ledare):

1. `grupp.PeterTeamRobot`
2. `grupp.MinnaTeamRobot`
3. `grupp.JonTeamRobot`
4. `grupp.LeoTeamRobot`

### 5. Strid
**Battle → New**, välj laget och några motståndare, till exempel `sampleteam.MyFirstTeam`.
Klicka på en robots namn i listan till höger under striden för att öppna dess konsol.

## Arbetssätt i Git

| Gren | Innehåll |
|---|---|
| `main` | Gemensamma filer: projektinställningar, meddelandeklasser och README |
| `peter-branch` | Peters robotar |
| `minna-branch` | Minnas robot |
| `jon-branch` | Jons robot |
| `leo-branch` | Leos robot |

1. Varje person arbetar på sin egen gren.
2. Robotfiler läggs i `src/grupp/` och börjar med raden `package grupp;`.
3. Ta in ändringar från `main` i din gren med `git merge main`.
4. När en robot är klar: skapa en **pull request** från din gren till `main`.
5. När alla fyra robotar finns på `main` kan hela laget byggas och köras.
