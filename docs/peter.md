# PeterTeamRobot – Ledare

**Ägare:** Peter  **Fil:** `src/grupp5/PeterTeamRobot.java`  **Färg:** grå, röd och orange

## Roll
Lagets ledare. Står först i laget och startar därför med **200 i energi**, dubbelt så mycket som en vanlig robot.
Spanar med radarn och delar allt den ser med resten av laget.

## Så fungerar den
- **Radar:** snurrar hela tiden för att se alla fiender, i stället för att låsa på en enda.
- **Målval:** fienden med lägst energi (samma regel som Minna och Jon, så att laget skjuter på samma mål).
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram och siktar dit.
- **Skottstyrka:** 3 på nära håll och svagare längre bort (`400 / avstånd`). Sparar energi när den har lite kvar.
- **Rörelse:** cirklar runt målet i sidled, vilket är svårast att träffa, och byter riktning slumpmässigt (5 % chans varje tur).
- **Minne:** räknar hur många gånger den blivit träffad (`hitsTaken`). Efter 3 träffar håller den 400 pixlars avstånd i stället för 250.
- **Väggar:** `isNearWall()` känner av när den är närmare än 60 pixlar från en vägg, och `turnTowardCenter()` svänger in mot mitten. `onHitWall` byter riktning om den ändå träffar väggen.

## Samarbete med laget
- Skickar sin position (`TeammateInfo`) varje tur och fiendernas positioner (`EnemyInfo`) när den ser dem.
- Skjuter aldrig när en lagkamrat står inom 40 pixlar från skottlinjen.
- Leo (Livvakten) håller sig nära Peter och skyddar honom.

## Kända svagheter och förbättringsidéer
- *(Fyll i efter testkörningar.)*

## Ändringar och tester
| Ändring | Vad vi trodde skulle hända | Vad som hände |
|---|---|---|
| | | |
| | | |
| | | |
