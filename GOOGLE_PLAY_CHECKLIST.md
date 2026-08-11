# Google Play Store - Checklist pro publikaci

## Stav připravenosti

### Hotovo
- [x] Release AAB soubor: `app/build/outputs/bundle/release/app-release.aab` (3.8 MB)
- [x] Podpisový klíč: `release-key.jks`
- [x] Ikona 512x512: `store_assets/icon_option1_dumbbell.png`
- [x] Feature Graphic 1024x500: `store_assets/feature_graphic.png`
- [x] Privacy Policy: `store_assets/privacy_policy.html`
- [x] SVG zdrojové soubory ikon

### Potřebuješ udělat
- [ ] Aktivovat GitHub Pages pro privacy policy
- [ ] Vytvořit screenshoty aplikace (min 2)
- [ ] Zaregistrovat se na Google Play Console ($25)
- [ ] Nahrát aplikaci

---

## Krok 1: Aktivovat GitHub Pages

1. Jdi na: https://github.com/tomastomasov0001-cmd/fitness-app/settings/pages
2. Source: **Deploy from a branch**
3. Branch: **main** / **docs**
4. Klikni **Save**
5. Počkej 2-3 minuty
6. Privacy policy bude na: `https://tomastomasov0001-cmd.github.io/fitness-app/privacy_policy.html`

---

## Krok 2: Vytvořit screenshoty

Spusť aplikaci v emulátoru nebo na telefonu a vyfotografuj tyto obrazovky:

1. **Kalendář** - hlavní obrazovka s naplánovanými tréninky
2. **Seznam tréninků** - přehled všech workout variant
3. **Detail tréninku** - seznam cviků v tréninku
4. **Aktivní trénink** - obrazovka cvičení se sériemi
5. **Statistiky** - grafy a přehledy

**Jak udělat screenshot:**
- Emulátor: klikni na ikonu fotoaparátu v panelu
- Telefon: Power + Volume Down

**Kam uložit:**
- `store_assets/screenshot_1.png`
- `store_assets/screenshot_2.png`
- atd.

---

## Krok 3: Registrace na Google Play Console

1. Jdi na: https://play.google.com/console
2. Přihlaš se Google účtem
3. Zaplať jednorázový poplatek **$25**
4. Vyplň údaje o vývojáři

---

## Krok 4: Vytvoření aplikace v Console

1. Klikni **Create app**
2. Vyplň:
   - **App name:** Fitness App
   - **Default language:** Čeština
   - **App or game:** App
   - **Free or paid:** Free

---

## Krok 5: Store listing (Záznam v obchodě)

### Základní informace
- **Název aplikace:** Fitness App
- **Krátký popis (80 znaků):**
  ```
  Sledujte své tréninky, cviky a pokrok. Jednoduché a efektivní.
  ```
- **Úplný popis (4000 znaků):**
  ```
  Fitness App je váš osobní fitness deník pro sledování tréninků.

  FUNKCE:
  • Vytvářejte vlastní tréninky s libovolnými cviky
  • Plánujte tréninky v kalendáři
  • Zaznamenávejte série, opakování a váhy
  • Sledujte svůj pokrok ve statistikách
  • Exportujte a importujte cviky
  • Přetahujte cviky pro změnu pořadí

  JEDNODUCHÉ POUŽITÍ:
  Žádné složité nastavování. Vytvořte trénink, přidejte cviky a začněte cvičit. Aplikace si pamatuje vaše předchozí výkony.

  OFFLINE:
  Všechna data jsou uložena lokálně na vašem zařízení. Nepotřebujete internet.

  SOUKROMÍ:
  Vaše data zůstávají pouze na vašem telefonu. Nic neodesíláme na servery.
  ```

### Grafika
- **Ikona:** `store_assets/icon_option1_dumbbell.png` (512x512)
- **Feature graphic:** `store_assets/feature_graphic.png` (1024x500)
- **Screenshoty:** nahraj min 2 screenshoty

---

## Krok 6: Content rating

1. Jdi do **Policy** → **App content** → **Content rating**
2. Vyplň dotazník (aplikace neobsahuje násilí, gambling, atd.)
3. Dostaneš rating **PEGI 3** nebo **Everyone**

---

## Krok 7: Privacy policy

1. Jdi do **Policy** → **App content** → **Privacy policy**
2. Vlož URL: `https://tomastomasov0001-cmd.github.io/fitness-app/privacy_policy.html`

---

## Krok 8: Nahrání AAB

1. Jdi do **Release** → **Production**
2. Klikni **Create new release**
3. Nahraj soubor: `app/build/outputs/bundle/release/app-release.aab`
4. Vyplň **Release notes:**
   ```
   Verze 1.0.0
   • První vydání aplikace
   • Vytváření a správa tréninků
   • Kalendář pro plánování
   • Statistiky a pokrok
   • Export/import cviků
   ```
5. Klikni **Save** → **Review release** → **Start rollout**

---

## Krok 9: Čekání na schválení

- Google zkontroluje aplikaci (1-7 dní)
- Dostaneš email o schválení/zamítnutí
- Po schválení bude aplikace v obchodě

---

## Důležité soubory

| Soubor | Cesta |
|--------|-------|
| AAB | `app/build/outputs/bundle/release/app-release.aab` |
| Keystore | `release-key.jks` |
| Ikona | `store_assets/icon_option1_dumbbell.png` |
| Feature Graphic | `store_assets/feature_graphic.png` |
| Privacy Policy | `store_assets/privacy_policy.html` |

---

## Kontakt pro dotazy

Pokud máš problémy, zkontroluj:
- https://support.google.com/googleplay/android-developer
- https://developer.android.com/distribute

---

*Připraveno: 11. srpna 2026*
