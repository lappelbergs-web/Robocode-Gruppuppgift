# MinnaTeamRobot – Tank

**Ägare:** Minna  **Fil:** `src/grupp/MinnaTeamRobot.java`  **Färg:** lila, rosa och vit

## Roll
Lagets Tank. Är en **Droid**: en robot **utan radar** som i gengäld startar med **120 i energi** i stället för 100.
Ser inte fienderna själv utan får deras positioner från lagkamraterna. Slåss på nära håll med hårda skott.

## Så fungerar den
- **Målval:** fienden med lägst energi, utifrån det lagkamraterna rapporterar (samma mål som Peter och Jon).
- **Sikte:** räknar ut var målet kommer att vara när kulan når fram och siktar dit.
- **Skottstyrka:** full styrka (3) ända ut till 300 pixlar, sedan svagare. Sparar energi när den har lite kvar.
- **Rörelse:** cirklar runt målet och försöker hålla cirka **200 pixlar** avstånd. Är den för långt bort svänger den in, är den för nära svänger den ut.
- **Väggar:** svänger in mot mitten när den är närmare än 60 pixlar från en vägg.
- **Reservläge:** om ingen i laget rapporterar fiender, till exempel för att alla med radar har dött, kör den runt och skjuter tillbaka mot den som senast träffade den.

## Samarbete med laget
- Är helt beroende av `EnemyPosition`-meddelanden från Peter, Jon och Leo.
- Skickar sin egen position (`TeammatePosition`) varje tur, så att de andra inte skjuter på henne.
- Skjuter aldrig när en lagkamrat står inom 40 pixlar från skottlinjen.

## Kända svagheter och förbättringsidéer
- *(Fyll i efter testkörningar.)*

## Ändringar och tester
| Ändring | Vad vi trodde skulle hända | Vad som hände |
|---|---|---|
| | | |
| | | |
| | | |
