# LeoTeamRobot – Livvakt

**Ägare:** Leo  **Fil:** `src/grupp5/LeoTeamRobot.java`  **Färg:** blå, turkos och vit

## Roll
Lagets Livvakt. Skyddar ledaren (Peter), som har mest energi och är viktigast för laget.
Har radar och delar allt den ser med laget.

## Så fungerar den
- **Radar:** snurrar hela tiden för att se alla fiender.
- **Målval:** fienden som är **närmast ledaren**, alltså det största hotet mot honom. Det skiljer Leo från de andra, som skjuter på fienden med lägst energi.
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram och siktar dit.
- **Skottstyrka:** 3 på nära håll och svagare längre bort (`500 / avstånd`). Sparar energi när den har lite kvar.
- **Rörelse:** håller sig cirka **120 pixlar** från ledaren, på den sida där hotet finns, så att den står mellan ledaren och fienden.
- **Minne:** räknar hur många gånger den blivit träffad (`hitsTaken`). Efter 4 träffar slutar den vakta och slåss som en vanlig krigare, så att den inte offrar sig helt.
- **Om ledaren dör:** blir en vanlig krigare som cirklar runt och skjuter på lagets gemensamma mål.
- **Väggar:** `isNearWall()` känner av när den är närmare än 60 pixlar från en vägg, och `turnTowardCenter()` svänger in mot mitten. `onHitWall` byter riktning.

## Samarbete med laget
- Hittar ledaren genom att leta efter en lagkamrat vars namn innehåller `PeterTeamRobot`.
  Byter ni namn på Peters robot måste konstanten `LEADER_NAME` överst i filen ändras.
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
