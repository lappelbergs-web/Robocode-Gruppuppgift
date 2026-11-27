# JonTeamRobot – Prickskytt

**Ägare:** Jon  **Fil:** `src/grupp5/JonTeamRobot.java`  **Färg:** grön och gul

## Roll
Lagets Prickskytt. Håller sig långt från striden, är svår att träffa och skjuter bara när den är säker på att träffa.
Har radar och delar allt den ser med laget.

## Så fungerar den
- **Radar:** snurrar hela tiden för att se alla fiender.
- **Målval:** fienden med lägst energi (samma mål som Peter och Minna).
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram. Skjuter **bara när kanonen pekar exakt rätt**, inom 3 grader. Det är ett strängare krav än hos de andra robotarna.
- **Skottstyrka:** 2 på långt håll, eftersom svagare kulor går snabbare och hinner fram innan fienden flyttat sig. Styrka 3 om fienden kommer närmare än 200 pixlar.
- **Rörelse:** cirklar runt målet på cirka **400 pixlar** avstånd och byter riktning ofta (10 % chans varje tur, dubbelt så ofta som de andra).
- **Minne:** räknar hur många gånger den blivit träffad (`hitsTaken`). Efter 2 träffar byter den riktning ännu oftare (20 % chans varje tur).
- **Krockar:** backar undan om den krockar med en robot.
- **Väggar:** `isNearWall()` känner av när den är närmare än 60 pixlar från en vägg, och `turnTowardCenter()` svänger in mot mitten. `onHitWall` byter riktning.

## Samarbete med laget
- Skickar sin position (`TeammateInfo`) varje tur och fiendernas positioner (`EnemyInfo`) när den ser dem.
- Skjuter aldrig när en lagkamrat står inom 40 pixlar från skottlinjen.

## Kända svagheter och förbättringsidéer
- *(Fyll i efter testkörningar.)*

## Ändringar och tester
| Ändring | Vad vi trodde skulle hända | Vad som hände |
|---|---|---|
| | | |
| | | |
| | | |
