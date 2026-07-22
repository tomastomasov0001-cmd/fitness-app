# Fitness App - Přehled funkcí

## Verze: 2.0
## Datum: 2026-07-22

---

## 1. Základní struktura aplikace

### Záložky (Bottom Navigation)
- **Kalendář** - zobrazení tréninků v kalendáři
- **Tréninky** - správa tréninkových variant a cviků
- **Cvičit** - spuštění tréninku
- **Historie** - přehled dokončených tréninků
- **Statistiky** - grafy a přehledy výkonu (NOVÉ v 2.0)

### Databáze (Room)
- Verze: 4
- Soubor: `fitness_database`
- Entity:
  - `WorkoutVariant` - tréninkové varianty
  - `Exercise` - cviky
  - `VariantExercise` - propojení cviků s variantami
  - `WorkoutLog` - záznamy tréninků
  - `ExerciseSet` - série cviků

---

## 2. Tréninky (Varianty)

### Seznam tréninků (`VariantsFragment`)
- Zobrazení všech tréninků s počtem cviků
- Přidání nového tréninku (FAB tlačítko)
- Editace tréninku
- Smazání tréninku
- **Export** tréninků do JSON souboru
- **Import** tréninků z JSON souboru

### Detail tréninku (`VariantDetailFragment`)
- Zobrazení cviků v tréninku
- Přidání nového cviku
- Editace cviku
- Odebrání cviku z tréninku
- Editace názvu a popisu tréninku

### Cviky - vlastnosti
- **Název** (povinný - validace s upozorněním)
- **Popis** (volitelný)
- **Série** - ano/ne (výchozí počet sérií: 1-99)
- **Opakování** - ano/ne
- **Váha** - ano/ne
- **Čas** - ano/ne (hodiny:minuty:sekundy)

---

## 3. Cvičení (`WorkoutFragment`)

### Výběr tréninku
- Seznam tréninků ve formě karet
- Kliknutím spustíte trénink
- Pokud nejsou tréninky, zobrazí se informace

### Probíhající trénink
- Všechny cviky zobrazeny vertikálně pod sebou
- Každý cvik má:
  - Název a popis
  - Seznam sérií (pokud má série)
  - Tlačítko pro přidání série
- Každá série má:
  - Číslo série
  - Pole pro opakování (pokud má)
  - Pole pro váhu (pokud má)
  - Pole pro čas - hodiny:minuty:sekundy (pokud má)
  - Tlačítko dokončení (zelená barva po dokončení)

### Cviky bez sérií
- Zobrazí se pouze vstupní pole podle typu (opakování/váha/čas)
- Bez čísla série

### Dokončení tréninku
- Dialog s možnostmi: Dokončit / Zrušit / Pokračovat
- Po dokončení se trénink uloží do historie

---

## 4. Kalendář (`CalendarFragment`)

### Zobrazení
- Měsíční kalendář
- Dny s tréninky jsou označeny
- Kliknutím na den zobrazíte tréninky

### Tréninky na dni
- Seznam naplánovaných/dokončených tréninků
- Tlačítko pro přidání tréninku na den
- Tlačítko pro zobrazení/editaci tréninku
- Tlačítko pro smazání tréninku (s potvrzením)

---

## 5. Historie (`HistoryFragment`)

### Seznam
- Všechny dokončené tréninky seřazené podle data (nejnovější nahoře)
- Zobrazení názvu tréninku a data
- Badge "Dokončeno" pro dokončené tréninky

### Detail z historie
- Kliknutím otevřete trénink v režimu prohlížení
- Tlačítko "Upravit" pro přepnutí do editace
- Tlačítko "Uložit" pro uložení změn
- Tlačítko "Zpět" pro návrat do historie

---

## 6. Statistiky (`StatisticsFragment`) - NOVÉ v 2.0

### Dashboard - základní přehled
- **Celkem tréninků** - počet všech dokončených tréninků
- **Tréninkové dny tento měsíc** - počet unikátních dnů s tréninkem
- **Aktuální série** - po sobě jdoucí dny s tréninkem
- **Nejdelší série** - rekord po sobě jdoucích dnů

### Tento týden
- Počet tréninků
- Celkový objem (váha × opakování)
- Počet sérií

### Tento měsíc
- Počet tréninků
- Porovnání s minulým měsícem (+/- změna)

### Graf frekvence tréninků
- Sloupcový graf (BarChart)
- Zobrazuje počet tréninků po týdnech
- Období: posledních 4 týdnů

### Rozložení tréninků
- Koláčový graf (PieChart)
- Zobrazuje, které varianty tréninku používáte nejvíce
- Procentuální zastoupení

### Osobní rekordy
- Seznam cviků s osobními rekordy
- Max váha pro každý cvik
- Max opakování pro každý cvik

### Heatmapa kalendáře
- Roční přehled jako GitHub contributions
- Zelené čtverečky podle intenzity tréninků
- Horizontální scrollování

### Progrese cviku (`ExerciseStatsFragment`)
- Výběr cviku pro detailní statistiky
- **Graf progrese váhy** - čárový graf vývoje max váhy
- **Graf progrese objemu** - čárový graf vývoje celkového objemu
- **Osobní rekordy** pro vybraný cvik
- **Poslední trénink** - detail všech sérií

---

## 7. Export / Import

### Export
- Ikona šipky nahoru v záložce Tréninky
- Exportuje všechny tréninky, cviky a jejich propojení
- Formát: JSON
- Název souboru: `fitness_backup_DATUM.json`

### Import
- Ikona šipky dolů v záložce Tréninky
- Načte tréninky ze záložního souboru
- Duplicity podle názvu - použije existující
- Zobrazí počet importovaných položek

### Struktura JSON souboru
```json
{
  "version": 1,
  "exportDate": 1234567890,
  "variants": [...],
  "exercises": [...],
  "variantExercises": [...]
}
```

---

## 8. Navigace

### Dolní navigace
- Kliknutí na záložku = přechod na hlavní obrazovku
- Opakované kliknutí = návrat na začátek záložky
- Správně fungující historie navigace

### Šipka zpět
- V detailu tréninku → seznam tréninků
- V prohlížení historie → seznam historie
- V detailu statistik cviku → přehled statistik
- Správné chování systémové šipky zpět

---

## 9. Databázové migrace

### Verze 1 → 2
- Přidáno: `hasReps`, `hasWeight`, `hasTime` do `exercises`
- Přidáno: `timeSeconds` do `exercise_sets`

### Verze 2 → 3
- Přidáno: `hasSets` do `exercises`

### Verze 3 → 4
- Přidáno: `defaultSets` do `exercises`

### Přímé migrace
- 1 → 3 (pro uživatele, kteří přeskočili verzi 2)
- 1 → 4 (pro nové instalace ze staré verze)

---

## 10. Soubory projektu

### Data vrstva
- `Entities.kt` - datové třídy + statistické data classes
- `FitnessDao.kt` - databázové operace + statistické dotazy
- `FitnessDatabase.kt` - konfigurace databáze
- `BackupData.kt` - třídy pro export/import

### UI - Kalendář
- `CalendarFragment.kt`
- `CalendarViewModel.kt`
- `fragment_calendar.xml`
- `item_calendar_workout.xml`

### UI - Tréninky
- `VariantsFragment.kt`
- `VariantsViewModel.kt`
- `VariantsAdapter.kt`
- `VariantDetailFragment.kt`
- `ExercisesAdapter.kt`
- `fragment_variants.xml`
- `fragment_variant_detail.xml`
- `item_variant.xml`
- `item_exercise.xml`

### UI - Cvičení
- `WorkoutFragment.kt`
- `WorkoutViewModel.kt`
- `WorkoutVariantAdapter.kt`
- `fragment_workout.xml`
- `item_workout_exercise.xml`
- `item_workout_set.xml`
- `item_workout_variant.xml`

### UI - Historie
- `HistoryFragment.kt`
- `HistoryViewModel.kt`
- `HistoryAdapter.kt`
- `fragment_history.xml`
- `item_history.xml`

### UI - Statistiky (NOVÉ v 2.0)
- `StatisticsFragment.kt`
- `StatisticsViewModel.kt`
- `ExerciseStatsFragment.kt`
- `ExerciseStatsViewModel.kt`
- `RecordsAdapter.kt`
- `ExerciseSelectAdapter.kt`
- `fragment_statistics.xml`
- `fragment_exercise_stats.xml`
- `item_exercise_record.xml`
- `item_exercise_select.xml`

### Dialogy
- `dialog_add_variant.xml`
- `dialog_add_exercise.xml`

### Navigace
- `nav_graph.xml`
- `bottom_nav_menu.xml`

### Ikony (drawable)
- `ic_calendar.xml`
- `ic_fitness.xml`
- `ic_play.xml`
- `ic_history.xml`
- `ic_statistics.xml` (NOVÉ v 2.0)
- `ic_add.xml`
- `ic_edit.xml`
- `ic_delete.xml`
- `ic_back.xml`
- `ic_check.xml`
- `ic_export.xml`
- `ic_import.xml`
- `ic_next.xml`
- `circle_background.xml`
- `tag_background.xml`

---

## 11. Závislosti (build.gradle)

```gradle
// AndroidX Core
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// Navigation
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'

// Room Database
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// Lifecycle
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'

// RecyclerView
implementation 'androidx.recyclerview:recyclerview:1.3.2'

// Glide pro obrazky
implementation 'com.github.bumptech.glide:glide:4.16.0'

// ExoPlayer pro videa
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'androidx.media3:media3-ui:1.2.0'

// Calendar
implementation 'com.kizitonwose.calendar:view:2.4.1'

// Gson pro JSON export/import
implementation 'com.google.code.gson:gson:2.10.1'

// MPAndroidChart pro grafy (NOVÉ v 2.0)
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

---

## 12. Známé vlastnosti

- Dialog pro přidání cviku má stále viditelný scrollbar
- Při prázdném názvu cviku se zobrazí Toast upozornění
- Počet sérií se ukládá per cvik (defaultSets)
- Export/import zachovává všechna data včetně propojení
- Heatmapa zobrazuje poslední rok tréninků
- Grafy se animují při zobrazení

---

## 13. Co je nového ve verzi 2.0

### Nová záložka Statistiky
- Dashboard s přehledem výkonu
- Série tréninků (streak tracking)
- Týdenní a měsíční přehledy

### Grafy
- Sloupcový graf frekvence tréninků
- Koláčový graf rozložení variant
- Čárové grafy progrese váhy a objemu

### Heatmapa
- Roční přehled tréninků jako GitHub contributions
- Vizuální motivace k pravidelnému cvičení

### Osobní rekordy
- Přehled max váhy a opakování pro každý cvik
- Detail progrese jednotlivých cviků

---

## Autor
Vytvořeno s pomocí Claude Code (Anthropic)
