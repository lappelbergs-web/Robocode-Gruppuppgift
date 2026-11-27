# MinnaTeamRobot – Tank

**Ägare:** Minna  **Fil:** `src/grupp5/MinnaTeamRobot.java`  **Färg:** lila, rosa och vit

## Roll
Lagets Tank. Slåss på nära håll med hårda skott och ramar motståndare som den krockar med.
Har radar och delar allt den ser med laget.

## Så fungerar den
- **Radar:** snurrar hela tiden för att se alla fiender.
- **Målval:** fienden med lägst energi (samma mål som Peter och Jon).
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram och siktar dit.
- **Skottstyrka:** full styrka (3) ända ut till 300 pixlar, sedan svagare. Sparar energi när den har lite kvar.
- **Rörelse:** cirklar runt målet och försöker hålla cirka **200 pixlar** avstånd. Är den för långt bort svänger den in, är den för nära svänger den ut.
- **Minne:** räknar missade skott (`missedShots`, via `onBulletMissed`). Efter 5 missar går den närmare, till 150 pixlar, för att träffa bättre.
- **Ramning:** krockar den med en motståndare (`onHitRobot`) kör den rakt mot målet i 20 turer. Krockar den med en lagkamrat backar den undan.
- **Väggar:** `isNearWall()` känner av när den är närmare än 60 pixlar från en vägg, och `turnTowardCenter()` svänger in mot mitten. `onHitWall` byter riktning och avbryter en ramning.

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
