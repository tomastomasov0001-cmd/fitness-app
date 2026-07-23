# Fitness App - Historie vývoje

## Přehled verzí
- **v1.0** - Základní funkce (Kalendář, Tréninky, Cvičení, Historie)
- **v2.0** - Statistiky a grafy výkonu

---

## Verze 1.0 - Základní implementace

### Datum: 2026-07-21

### Implementované funkce

#### 1. Struktura aplikace
- 4 záložky v bottom navigation: Kalendář, Tréninky, Cvičit, Historie
- Room databáze verze 4
- MVVM architektura s ViewModely a LiveData
- Navigation Component pro navigaci

#### 2. Tréninky (Varianty)
- Seznam tréninků s počtem cviků
- Přidání/editace/smazání tréninku
- Detail tréninku s cviky
- Cviky s vlastnostmi: série, opakování, váha, čas

#### 3. Cvičení
- Výběr tréninku ze seznamu karet
- Probíhající trénink se všemi cviky
- Série s opakováním, váhou, časem
- Dokončení tréninku s uložením do historie

#### 4. Kalendář
- Měsíční kalendář (kizitonwose calendar)
- Označení dnů s tréninky
- Přidání/zobrazení/smazání tréninků na dni

#### 5. Historie
- Seznam dokončených tréninků
- Prohlížení detailu tréninku
- Editace dokončeného tréninku

#### 6. Export/Import
- Export tréninků do JSON souboru
- Import tréninků ze zálohy
- Řešení duplicit podle názvu

### Opravené problémy

#### Pád aplikace při startu
- **Problém:** "Type is long but found integer: 0" v nav_graph.xml
- **Řešení:** Změna `android:defaultValue="0"` na `android:defaultValue="0L"` pro Long argumenty

#### Dialog přidání cviku
- **Problém:** Poslední řádek (čas) nebyl vidět
- **Řešení:** Přidání ScrollView s `android:fadeScrollbars="false"`

#### Validace názvu cviku
- **Problém:** Šlo uložit cvik bez názvu
- **Řešení:** Toast upozornění a zablokování uložení bez názvu

#### Počet sérií se neukládal
- **Problém:** Vždy 3 série i po změně
- **Řešení:** Přidání `defaultSets` do Exercise entity + migrace 3→4

#### Navigace
- **Problém:** Šipka zpět šla na špatné místo, bottom nav nefungoval správně
- **Řešení:** `setOnItemSelectedListener` a `setOnItemReselectedListener` v MainActivity

### Databázové migrace
- 1→2: hasReps, hasWeight, hasTime, timeSeconds
- 2→3: hasSets
- 3→4: defaultSets
- Přímé migrace: 1→3, 1→4

### Auto Backup
- `android:allowBackup="true"` v AndroidManifest.xml
- Data se obnovují po reinstalaci z Google cloudu
- Pro čistou instalaci: vymazat data aplikace před odinstalací

---

## Verze 2.0 - Statistiky a grafy

### Datum: 2026-07-22

### Nové funkce

#### 1. Záložka Statistiky
- Nová 5. záložka v bottom navigation
- StatisticsFragment + StatisticsViewModel
- ExerciseStatsFragment + ExerciseStatsViewModel

#### 2. Dashboard
- Celkový počet dokončených tréninků
- Tréninkové dny tento měsíc
- Aktuální série (po sobě jdoucí dny)
- Nejdelší série (rekord)

#### 3. Časové přehledy
- **Tento týden:** počet tréninků, objem, série
- **Tento měsíc:** počet tréninků, porovnání s minulým měsícem

#### 4. Grafy (MPAndroidChart)
- **Sloupcový graf:** frekvence tréninků po týdnech (posledních 4 týdnů)
- **Koláčový graf:** rozložení tréninků podle variant
- **Čárové grafy:** progrese váhy a objemu pro jednotlivé cviky

#### 5. Heatmapa kalendáře
- Roční přehled jako GitHub contributions
- Zelené čtverečky podle intenzity (0-4+ tréninků)
- Horizontální scrollování

#### 6. Osobní rekordy
- Seznam cviků s max váhou a max opakováním
- Pouze cviky s existujícími daty

#### 7. Progrese cviku
- Výběr cviku pro detailní statistiky
- Graf progrese váhy v čase
- Graf progrese objemu (váha × opakování)
- Detail posledního tréninku

### Technické změny

#### Nové závislosti
```gradle
// settings.gradle
maven { url 'https://jitpack.io' }

// build.gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

#### Nové DAO metody (FitnessDao.kt)
- `getTotalCompletedWorkouts()` - celkový počet
- `getTrainingDaysInMonth()` - dny v měsíci
- `getAllCompletedWorkoutsSync()` - pro výpočet série
- `getWorkoutCountsByVariant()` - pro koláčový graf
- `getAllSetsForExercise()` - pro grafy progrese
- `getMaxWeightForExercise()` - osobní rekord váhy
- `getMaxRepsForExercise()` - osobní rekord opakování
- `getExercisesWithData()` - cviky s daty
- `getWorkoutCountsByDate()` - pro sloupcový graf
- `getAllWorkoutDates()` - pro heatmapu

#### Nové data classes (Entities.kt)
- `VariantWorkoutCount` - pro koláčový graf
- `ExerciseSetWithDate` - série s datem
- `DateWorkoutCount` - počet na datum

#### Nové soubory
```
ui/statistics/
├── StatisticsFragment.kt
├── StatisticsViewModel.kt
├── ExerciseStatsFragment.kt
├── ExerciseStatsViewModel.kt
├── RecordsAdapter.kt
└── ExerciseSelectAdapter.kt

res/layout/
├── fragment_statistics.xml
├── fragment_exercise_stats.xml
├── item_exercise_record.xml
└── item_exercise_select.xml

res/drawable/
└── ic_statistics.xml
```

### Git commity
- `7664dae` - v1.0 - Fitness App kompletní implementace (83 souborů)
- `abaaf0d` - v2.0 - Statistiky a grafy výkonu (19 souborů, +2091 řádků)

---

## Struktura projektu

```
fitness_app/
├── v1.0/                    # Základní verze
│   ├── .git/
│   └── ...
├── v2.0/                    # Verze se statistikami
│   ├── .git/
│   └── ...
└── HISTORIE_VYVOJE.md       # Tento soubor
```

---

## Poznámky pro pokračování

### Co funguje
- Všechny základní funkce (CRUD tréninků, cvičení, historie)
- Export/import JSON
- Statistiky a grafy
- Auto backup dat

### Co by šlo přidat v budoucnu
- Notifikace/připomínky tréninků
- Sdílení výsledků
- Obrázky/videa u cviků (připraveno, ale neimplementováno)
- Tmavý režim
- Widgety na domovskou obrazovku
- Synchronizace mezi zařízeními

### Důležité soubory k prostudování
- `Entities.kt` - datový model
- `FitnessDao.kt` - databázové dotazy
- `FitnessDatabase.kt` - migrace
- `StatisticsViewModel.kt` - logika statistik
- `nav_graph.xml` - navigace

---

## Autor
Vytvořeno s pomocí Claude Code (Anthropic)
Datum poslední aktualizace: 2026-07-23
