# Fitness App v2.0 - Dokumentace projektu

## Přehled
Android fitness aplikace pro sledování tréninků, cviků a pokroku.

**GitHub:** https://github.com/tomastomasov0001-cmd/fitness-app

## Technologie
- **Jazyk:** Kotlin
- **Min SDK:** Android 7.0 (API 24)
- **Architektura:** MVVM (ViewModel + LiveData)
- **Databáze:** Room
- **Navigace:** Navigation Component
- **UI:** Material Design 3

## Struktura projektu

```
app/src/main/java/com/harvis/fitnessapp/
├── data/
│   ├── Entities.kt          # Room entity třídy
│   ├── FitnessDao.kt         # Database Access Object
│   └── FitnessDatabase.kt    # Room databáze
├── ui/
│   ├── calendar/             # Kalendář tréninků
│   ├── exercises/            # Správa všech cviků
│   ├── history/              # Historie tréninků
│   ├── statistics/           # Statistiky a grafy
│   ├── variants/             # Tréninky (varianty)
│   └── workout/              # Aktivní trénink
├── util/
│   ├── LanguageHelper.kt     # Přepínání jazyků
│   ├── PremiumManager.kt     # Premium funkce
│   └── PremiumDialogHelper.kt
├── FitnessApp.kt             # Application třída
└── MainActivity.kt           # Hlavní aktivita
```

## Hlavní funkce

### 1. Tréninky (Variants)
- Vytváření vlastních tréninků
- Přidávání cviků do tréninků
- Drag & drop přeřazování cviků
- Export/Import dat (JSON)

### 2. Cviky (Exercises)
- Centrální správa všech cviků
- Typy cviků: série, opakování, váha, čas
- Výběr existujícího cviku nebo vytvoření nového
- Zobrazení počtu použití v trénincích

### 3. Kalendář
- Plánování tréninků na konkrétní dny
- Max 4 tréninky denně
- Vizuální přehled

### 4. Aktivní trénink
- Záznam sérií, opakování, váhy, času
- Automatické ukládání

### 5. Historie
- Přehled dokončených tréninků
- Detail každého tréninku

### 6. Statistiky
- Celkový počet tréninků
- Série tréninků (streak)
- Osobní rekordy
- Grafy progrese

## Databázové entity

### Exercise (Cvik)
```kotlin
- id: Long
- name: String
- description: String
- hasSets: Boolean
- hasReps: Boolean
- hasWeight: Boolean
- hasTime: Boolean
- defaultSets: Int
```

### WorkoutVariant (Trénink)
```kotlin
- id: Long
- name: String
- description: String
- createdAt: Long
```

### VariantExercise (Propojení)
```kotlin
- variantId: Long
- exerciseId: Long
- orderIndex: Int
```

### WorkoutLog (Záznam tréninku)
```kotlin
- id: Long
- variantId: Long
- date: Long
- completed: Boolean
```

### ExerciseSet (Série)
```kotlin
- id: Long
- workoutLogId: Long
- exerciseId: Long
- setNumber: Int
- reps: Int?
- weight: Float?
- time: Int?
- completed: Boolean
```

## Lokalizace
Aplikace podporuje 8 jazyků:
- Čeština (cs) - výchozí
- Angličtina (en)
- Němčina (de)
- Španělština (es)
- Francouzština (fr)
- Italština (it)
- Polština (pl)
- Slovenština (sk)

## Premium funkce
- Free verze: 1 trénink, 2 cviky na trénink
- Premium: neomezeno
- Promo kódy pro testování

## Poslední změny (Srpen 2025)

### Sekce Cviky
- Nová obrazovka "Všechny cviky" přístupná z obrazovky Tréninky
- Možnost přidávat, upravovat a mazat cviky centrálně
- Při přidávání cviku do tréninku lze vybrat existující nebo vytvořit nový
- Drag & drop pro změnu pořadí cviků v tréninku
- Zobrazení počtu použití cviku v trénincích

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Instalace na zařízení
./gradlew installDebug
```

## Git workflow

```bash
# Status
git status

# Commit
git add .
git commit -m "Popis změn"

# Push
git push origin master
```

## Budoucí plány
- [ ] Šablony tréninků
- [ ] Sdílení tréninků
- [ ] Notifikace/připomínky
- [ ] Widget na plochu
- [ ] Synchronizace s cloudem
- [ ] Tmavý režim
- [ ] Fotky/videa u cviků

## Kontakt
Vytvořeno s pomocí Claude Code (Anthropic)
