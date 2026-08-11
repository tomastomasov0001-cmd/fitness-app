# Fitness App - Editor cviků (Windows)

Desktopová aplikace pro vytváření a správu cviků pro Fitness App.

## Požadavky

- Python 3.8 nebo novější
- Tkinter (součást standardní instalace Pythonu)

## Spuštění

### Možnost 1: Dvojklik na bat soubor
```
spustit.bat
```

### Možnost 2: Z příkazové řádky
```bash
python exercise_creator.py
```

## Funkce

- **Vytváření cviků** - název, popis, typ záznamu
- **Editace cviků** - dvojklik na cvik v seznamu
- **Mazání cviků** - tlačítko Smazat nebo klávesa Delete
- **Export do JSON** - pro import do Android aplikace
- **Import z JSON** - načtení existujících cviků

## Klávesové zkratky

| Zkratka | Akce |
|---------|------|
| Ctrl+N | Nový cvik (vyčistit formulář) |
| Ctrl+S | Export do JSON |
| Ctrl+O | Import z JSON |
| Delete | Smazat vybraný cvik |
| Double-click | Upravit cvik |

## Typy záznamu

- **Série** - více opakování cviku (např. 3 série)
- **Opakování** - počet opakování (např. 10 dřepů)
- **Váha** - zátěž v kg (např. 50 kg)
- **Čas** - délka cvičení (např. 60 sekund planku)

## Export/Import

Soubory jsou kompatibilní s Android aplikací Fitness App:

1. **Export z Windows**: Klikni "Export" → ulož JSON soubor
2. **Přenos na telefon**: Pošli soubor emailem, přes cloud, USB...
3. **Import v aplikaci**: Otevři Cviky → klikni ikonu Import → vyber soubor

## Formát souboru

```json
{
  "version": 1,
  "exportDate": 1234567890000,
  "exercises": [
    {
      "id": 1,
      "name": "Dřepy",
      "description": "Základní cvik na nohy",
      "hasSets": true,
      "hasReps": true,
      "hasWeight": true,
      "hasTime": false,
      "defaultSets": 3
    }
  ]
}
```

## Vytvoření EXE souboru (volitelné)

Pokud chceš vytvořit spustitelný .exe bez nutnosti Python:

```bash
pip install pyinstaller
pyinstaller --onefile --windowed --name "FitnessExerciseCreator" exercise_creator.py
```

Výsledný .exe bude v složce `dist/`.
