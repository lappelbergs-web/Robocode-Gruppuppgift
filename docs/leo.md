# LeoTeamRobot – Livvakt

**Ägare:** Leo  **Fil:** `src/grupp/LeoTeamRobot.java`  **Färg:** blå, turkos och vit

## Roll
Lagets Livvakt. Skyddar ledaren (Peter), som har mest energi och är viktigast för laget.
Har radar och delar allt den ser med laget.

## Så fungerar den
- **Radar:** snurrar hela tiden för att se alla fiender.
- **Målval:** fienden som är **närmast ledaren**, alltså det största hotet mot honom. Det skiljer Leo från de andra, som skjuter på fienden med lägst energi.
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram och siktar dit.
- **Skottstyrka:** 3 på nära håll och svagare längre bort (`500 / avstånd`). Sparar energi när den har lite kvar.
- **Rörelse:** håller sig cirka **120 pixlar** från ledaren, på den sida där hotet finns, så att den står mellan ledaren och fienden.
- **Om ledaren dör:** blir en vanlig krigare som cirklar runt och skjuter på lagets gemensamma mål.
- **Väggar:** svänger in mot mitten när den är närmare än 60 pixlar från en vägg.

## Samarbete med laget
- Hittar ledaren genom att leta efter en lagkamrat vars namn innehåller `PeterTeamRobot`.
  Byter ni namn på Peters robot måste konstanten `LEADER_CLASS` överst i filen ändras.
- Skickar sin position (`TeammatePosition`) varje tur och fiendernas positioner (`EnemyPosition`) när den ser dem.
- Skjuter aldrig när en lagkamrat står inom 40 pixlar från skottlinjen.

## Kända svagheter och förbättringsidéer
- *(Fyll i efter testkörningar.)*

## Ändringar och tester
| Ändring | Vad vi trodde skulle hända | Vad som hände |
|---|---|---|
| | | |
| | | |
| | | |
