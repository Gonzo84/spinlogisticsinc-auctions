# SPC Auctions — Načrt razvoja izdelka

## Platforma danes in kaj sledi

Jedro avkcijske platforme SPC Auctions je **v celoti zgrajeno in deluje**. Kupci lahko iščejo, oddajajo ponudbe v realnem času in zaključijo nakup. Prodajalci lahko objavljajo artikle, spremljajo prodajo in prejemajo izplačila. Administratorji lahko upravljajo celotno poslovanje. Avkcijski mehanizem, plačilni tok, iskanje, obvestila in orodja za skladnost — vse deluje od začetka do konca.

Kar ostaja pred polnim komercialnim zagonom, je povezava platforme z **zunanjimi storitvami v produkcijskem okolju** — procesor za obdelavo plačil v živo, ponudniki za preverjanje identitete in produkcijska dostava e-pošte — ter dokončanje regulativne skladnosti z zahtevami EU.

Ta načrt je urejen po poslovni prioriteti, ne po tehnični zahtevnosti.

---

## Faza 1 — Zahtevano za komercialni zagon

Te postavke morajo biti zaključene, preden lahko platforma obdeluje resnične transakcije in komercialno posluje v EU.

---

### Obdelava plačil v živo

**Danes:** Plačilni tok je v celoti zgrajen — zaključek nakupa, izračun VAT, kupčeva premija, poravnava in generiranje računov — vse deluje. Vendar se plačila trenutno obdelujejo v testnem/simulacijskem načinu. Pravi denar ne menja lastnika.

**Kaj je potrebno:** Povezati platformo z **Adyen** (vodilnim evropskim procesorjem plačil) za sprejem resničnih plačil.

**Kaj to omogoča:**
- Sprejem kreditnih/debetnih kartic, iDEAL (Nizozemska), SEPA direktna obremenitev, Bancontact (Belgija) in drugih evropskih plačilnih metod
- 3D Secure avtentikacija za vsako transakcijo — zahtevana z zakonodajo EU (PSD2) za spletna plačila
- Avtomatska obdelava vračil prek ponudnika plačil
- Časovnica poravnav usklajena s pogodbami s prodajalci (npr. izplačilo 1–2 delovna dneva po potrditvi plačila)
- Polna skladnost s PCI-DSS — podatke o imetnikih kartic obdeluje Adyen, nikoli niso shranjeni na platformi

**Zakaj je to pomembno:** Brez tega platforma ne more ustvarjati prihodkov. To je najpomembnejša postavka na načrtu razvoja.

---

### Preverjanje identitete (KYC)

**Danes:** Platforma spremlja status preverjanja uporabnikov, vendar dejansko ne preverja nikogaršnje identitete. Uporabniki se lahko registrirajo in oddajajo ponudbe brez dokazovanja, kdo so.

**Kaj je potrebno:** Integrirati **ponudnika KYC (Spoznaj svojo stranko)**, kot je Onfido, Jumio ali IDnow, za preverjanje identitete uporabnikov, preden lahko sodelujejo na avkcijah.

**Kaj to omogoča:**
- Uporabniki naložijo fotografijo osebnega dokumenta (potni list, osebna izkaznica, vozniško dovoljenje) in sistem jo samodejno preveri
- Detekcija živosti preprečuje identitetne goljufije (potrjuje, da je oseba resnična, ne fotografija fotografije)
- Preverjanje naslova prek položnic ali bančnih izpiskov
- Ocenjevanje tveganja označi visoko tvegane uporabnike za ročni pregled
- Oddajanje ponudb je blokirano, dokler identiteta ni preverjena — to ščiti prodajalce in platformo pred goljufijami

**Zakaj je to pomembno:** Regulativa EU zahteva, da platforme, ki obdelujejo finančne transakcije, preverijo identiteto svojih uporabnikov. Brez KYC je platforma pravno izpostavljena in ranljiva za goljufive račune.

---

### Preverjanje za preprečevanje pranja denarja (AML)

**Danes:** Platforma ima vmesnik za AML preverjanje, vendar trenutno vrača simulirane rezultate namesto preverjanja v resničnih bazah sankcij.

**Kaj je potrebno:** Povezati se s **ponudnikom AML preverjanja**, kot je ComplyAdvantage, Refinitiv ali LexisNexis, za izvajanje resničnih preverjanj ozadja.

**Kaj to omogoča:**
- Avtomatsko preverjanje v mednarodnih seznamih sankcij (OZN, EU, OFAC)
- PEP preverjanja — identifikacija visoko tveganih posameznikov na vladnih ali javnih položajih
- Preverjanje negativnih medijskih objav — označitev uporabnikov, omenjenih v negativnih novicah
- Spremljanje sumljivih transakcij — zaznavanje neobičajnih vzorcev oddajanja ponudb ali plačil
- Avtomatizirano poročanje o sumljivih dejavnostih pristojni FIU

**Zakaj je to pomembno:** Direktiva EU o preprečevanju pranja denarja (AMLD6) zahteva, da platforme, ki omogočajo visoko vredne transakcije, preverjajo uporabnike in poročajo o sumljivih dejavnostih. Neskladnost pomeni znatne globe in potencialno kazensko odgovornost za operaterje platforme.

---

### Produkcijska dostava e-pošte

**Danes:** Vsa e-poštna obvestila (opozorila o preseženih ponudbah, rezultati avkcij, potrditve plačil) so v celoti zgrajena in delujejo, vendar se med razvojem zajemajo v testni nabiralnik namesto dostave resničnim uporabnikom.

**Kaj je potrebno:** Povezati se s **produkcijsko e-poštno storitvijo**, kot je AWS SES, SendGrid ali Mailgun.

**Kaj to omogoča:**
- Resnična dostava e-pošte kupcem, prodajalcem in administratorjem
- Profesionalne blagovno označene e-poštne predloge, skladne z identiteto SPC Auctions
- Sledenje dostavi — vedenje, ali so bila e-poštna sporočila prejeta, odprta ali zavrnjena
- Upravljanje odjav — zahtevano s strani GDPR in zakonodaje o neželeni pošti
- Omejitev hitrosti pošiljanja za preprečevanje klasifikacije kot neželena pošta

**Zakaj je to pomembno:** E-pošta je primarni komunikacijski kanal za časovno občutljive avkcijske dogodke (obvestila o preseženih ponudbah, zaključek avkcije, rok plačila). Brez resnične dostave e-pošte uporabniki zamudijo ključne posodobitve in platforma deluje neodzivno.

---

### Dokončanje skladnosti z GDPR

**Danes:** Platforma podpira zahtevke GDPR za izvoz in izbris podatkov — uporabniki jih lahko oddajo, administratorji pa jih upravljajo. Vendar dejansko zbiranje podatkov in brisanje med storitvami še ni v celoti avtomatizirano.

**Kaj je potrebno:** Dokončati avtomatizirani cevovod za ravnanje s podatki v vseh storitvah platforme.

**Kaj to omogoča:**
- Ko uporabnik zahteva svoje podatke (člen 20 — pravica do prenosljivosti podatkov), platforma samodejno zbere vse informacije iz vsake storitve in jih dostavi kot šifriran prenos
- Ko uporabnik zahteva izbris podatkov (člen 17 — pravica do pozabe), platforma samodejno odstrani podatke v vseh storitvah s potrditvijo
- Uveljavljanje 30-dnevnega roka z avtomatskimi opozorili administratorjem, če se zahtevek približuje zakonskemu roku
- Integracija politike zasebnosti in soglasja za piškotke na vseh portalih
- Popolna revizijska sled vsega ravnanja s podatki za regulativni pregled

**Zakaj je to pomembno:** Globe GDPR lahko dosežejo **4 % letnega globalnega prometa** ali 20 milijonov EUR, kar koli je višje. Delna skladnost ni dovolj — uredba zahteva popolno, pravočasno in preverljivo ravnanje s podatki.

---

### Notranja varnostna utrditev

**Danes:** Vsaka uporabniško usmerjena končna točka je varovana z avtentikacijo in nadzorom dostopa na podlagi vlog. Vendar notranja komunikacija med storitvami platforme še ne uporablja avtenticiranih povezav.

**Kaj je potrebno:** Dodati varno avtentikacijo vsej notranji komunikaciji med storitvami.

**Kaj to omogoča:**
- Vsaka notranja zahteva je preverjena in revizijsko sledljiva
- Če je ena storitev kompromitirana, ne more lažno nastopati kot druga
- Popolna revizijska sled vseh notranjih operacij
- Izpolnjevanje varnostnih zahtev za platforme finančnih storitev

**Zakaj je to pomembno:** Za platformo, ki obdeluje finančne transakcije, notranja varnost ni neobvezna. Poslovne stranke in revizorji bodo zahtevali dokaz, da je notranja komunikacija varovana.

---

### Avtomatizirani cevovod za uvajanje

**Danes:** Celoten cevovod za uvajanje je zgrajen in preizkušen — avtomatizirano gradnje, testiranje, varnostno pregledovanje in pakiranje vsebnikov. Zadnji korak (uvajanje na produkcijske strežnike) je konfiguriran, a še ni aktiviran.

**Kaj je potrebno:** Aktivirati produkcijsko fazo uvajanja in jo povezati z gostujočo infrastrukturo.

**Kaj to omogoča:**
- Spremembe kode se samodejno testirajo, pregledajo za varnostne ranljivosti in uvajajo v testno okolje za preverjanje
- Produkcijske uvajanja se izvajajo z enim klikom (ali samodejno ob odobritvi)
- Če gre kaj narobe, se platforma lahko samodejno povrne na prejšnjo različico
- Brez ročne konfiguracije strežnikov ali kopiranja datotek — vse je avtomatizirano in ponovljivo

**Zakaj je to pomembno:** Ročna uvajanja so počasna, nagnjena k napakam in tvegana. Avtomatizirani cevovod pomeni hitrejše posodobitve, manj napak in sposobnost hitrega odziva na težave.

---

## Faza 2 — Prvih 3 mesecev po zagonu

Te postavke izboljšujejo uporabniško izkušnjo in dopolnjujejo regulativne zahteve. Platforma deluje brez njih, vendar bi jih bilo treba dodati kmalu po zagonu.

---

### Mobilna potisna obvestila

**Danes:** Uporabniki prejemajo e-poštna obvestila za avkcijske dogodke. Potisna obvestila na mobilne naprave še niso aktivna.

**Kaj to doda:** Potisna opozorila v realnem času na telefone in tablice — »Vaša ponudba je bila presežena!«, »Avkcija se zaključi čez 5 minut«, »Plačilo prejeto«. Dotik na obvestilo uporabnika popelje neposredno na ustrezno avkcijo ali plačilo.

**Zakaj je to pomembno:** Avkcije so časovno občutljive. Mobilna potisna obvestila spodbujajo angažiranost in zmanjšujejo število zamujenih ponudb, kar povečuje prihodke platforme.

---

### Dokončanje Digital Services Act (DSA)

**Danes:** Poročanje o vsebinah je vzpostavljeno — uporabniki lahko prijavijo neprimerne objave. Moderacijski potek dela in poročanje o transparentnosti potrebujeta dokončanje.

**Kaj to doda:** Polna skladnost z DSA, vključno z avtomatiziranimi moderacijskimi poteki dela, obveščanjem uporabnikov o odločitvah, pritožbenimi mehanizmi in popolnimi poročili o transparentnosti, kot zahteva Uredba EU 2022/2065.

**Zakaj je to pomembno:** DSA se nanaša na vse spletne platforme, ki poslujejo v EU. Neskladnost lahko povzroči globe do **6 % letnega globalnega prometa**.

---

### Izboljšana analitika in poročanje

**Danes:** Osnovne analitične nadzorne plošče prikazujejo trende prihodkov, obseg ponudb in rast uporabnikov. Nekatere metrike potrebujejo izpopolnitev.

**Kaj to doda:** Nadzorne plošče v realnem času (ne le dnevni posnetki), gradnja prilagojenih poročil, izvoz podatkov v CSV/Excel/PDF, primerjalna analiza uspešnosti prodajalcev in analitika prodajnega lijaka.

**Zakaj je to pomembno:** Operaterji platforme potrebujejo celovite podatke za poslovne odločitve — katere kategorije so najuspešnejše, kam usmeriti trženje, kateri prodajalci ustvarjajo največ prihodkov.

---

### Napredno upravljanje uporabnikov

**Danes:** Administratorji lahko upravljajo posamezne uporabnike. Množične operacije in podrobno sledenje dejavnosti so omejeni.

**Kaj to doda:** Iskanje in filtriranje vseh uporabnikov, množično odobravanje/suspendiranje, podrobni časovni poteki dejavnosti po uporabniku, zgodovina prijav in neobvezna dvofaktorska avtentikacija za visoko varnostne račune.

**Zakaj je to pomembno:** Z rastjo uporabniške baze administratorji potrebujejo učinkovita orodja za upravljanje tisoče računov brez posamičnega obravnavanja.

---

### Napreden nadzor dostopa

**Danes:** Nadzor dostopa na podlagi vlog deluje za vse standardne operacije. Natančna dovoljenja potrebujejo izpopolnitev.

**Kaj to doda:** Podrobno upravljanje dovoljenj, nadzor dostopa na ravni polj za občutljive podatke (npr. finančni podatki vidni le pooblaščenemu osebju) in administrativni vmesnik za upravljanje dovoljenj brez sprememb kode.

**Zakaj je to pomembno:** Poslovne stranke pogosto zahtevajo podrobne politike nadzora dostopa — različni člani administrativne ekipe naj vidijo različne podatke glede na svojo vlogo in odgovornosti.

---

## Faza 3 — Funkcionalnosti za rast (3–6 mesecev)

Te funkcionalnosti razširjajo zmogljivosti platforme in tržni doseg.

---

### Podpora za več valut

**Danes:** Vse transakcije so v EUR.

**Kaj to doda:** Podporo za GBP, CHF, USD, PLN, CZK, SEK in druge valute. Avtomatska pretvorba menjalnih tečajev, poravnave v več valutah in lokaliziran prikaz cen.

**Zakaj je to pomembno:** Odpira platformo za Združeno kraljestvo, Švico in trge EU zunaj evrskega območja — bistveno razširi naslovljivi trg.

---

### Mobilna aplikacija

**Danes:** Platforma je popolnoma odzivna in deluje v mobilnih brskalnikih.

**Kaj to doda:** Namensko mobilno aplikacijo (iOS in Android) s poglobljenim povezovanjem potisnih obvestil, biometrično prijavo (prstni odtis/obraz), brskanje po lotih brez povezave in integracijo kamere za posrednike pri terenskem prevzemu lotov.

**Zakaj je to pomembno:** Izvorna aplikacija zagotavlja hitrejšo zmogljivost, boljše upravljanje potisnih obvestil in prisotnost v trgovinah z aplikacijami za boljšo odkritost.

---

### Dodatni avkcijski formati

**Danes:** Platforma izvaja angleške (naraščajoča cena) avkcije — najpogostejši format pri B2B prodaji opreme.

**Kaj to doda:**
- **Nizozemske avkcije** — cena se začne visoko in pada, dokler nekdo ne odda ponudbe (pogosto za pokvarljivo blago in zaloge v razsutem stanju)
- **Avkcije z zapečatenimi ponudbami** — kupci oddajo eno zaupno ponudbo; najvišja zmaga (uporablja se za nepremičnine in visoko vredne unikatne artikle)
- **Kupi-zdaj-ali-ponudi** — možnost fiksne cene poleg avkcije (podobno kot eBayjev »Kupi zdaj«)
- **Paketne avkcije** — združevanje več lotov v eno avkcijo (pogosto za likvidacije tovarn)
- **Valovito zapiranje** — skupine avkcij se zapirajo zaporedoma za lažje upravljanje pozornosti kupcev

**Zakaj je to pomembno:** Različne vrste sredstev ustrezajo različnim avkcijskim formatom. Ponudba več formatov privabi več prodajalcev in omogoča platformi, da obvladuje širši nabor komercialnih situacij.

---

### Avtomatizirano uvajanje prodajalcev

**Danes:** Nove prodajalce ročno odobrijo administratorji.

**Kaj to doda:** Samopostrežno uvajanje z avtomatskim preverjanjem VAT številk, preverjanjem v poslovnem registru, stopenjskimi nivoji prodajalcev (začetni, preverjeni, premium) in značkami zaupanja na podlagi zgodovine uspešnosti.

**Zakaj je to pomembno:** Ročno odobravanje ustvarja ozko grlo. Avtomatizirano uvajanje omogoča platformi rast brez sorazmernega povečevanja administrativne obremenitve.

---

### Preverjeno poročanje o trajnostnosti

**Danes:** Izogibanje CO2 se izračunava z uporabo standardnih emisijskih faktorjev po kategoriji opreme.

**Kaj to doda:** S strani tretjih oseb preverjena potrdila CO2 (ISO 14040/14044), izračuni prilagojeni stanju, ter integracija z okoljskimi viri podatkov. Kupci in prodajalci prejmejo preverljiva potrdila o trajnostnosti.

**Zakaj je to pomembno:** Premik trajnostnosti od trženjske trditve k preverljivim, certificiranim potrdilom — vse pomembnejše za korporativno ESG poročanje in nabavne politike.

---

## Faza 4 — Vizija prihodnosti (6–12 mesecev)

Dolgoročne izboljšave, ki SPC Auctions preobrazijo iz samostojne platforme v ekosistem.

---

### Funkcionalnosti z umetno inteligenco

- **Pametni opisi lotov** — naložite fotografije in umetna inteligenca samodejno ustvari profesionalne opise za objavo
- **Napovedovanje cen** — analiza zgodovinskih podatkov predlaga optimalne začetne cene in rezervne zneske
- **Zaznavanje goljufij** — strojno učenje prepozna sumljive vzorce oddajanja ponudb, preden povzročijo škodo
- **Prilagojene priporočila** — kupci vidijo lote, prilagojene njihovim interesom in zgodovini ponudb
- **Podporni klepetalni robot** — avtomatizirana podpora na prvi ravni za pogosta vprašanja kupcev in prodajalcev

### Platforma z belo etiketo

- Ponudba SPC Auctions kot **rešitev z belo etiketo** — avkcijske hiše lahko upravljajo svojo lastno blagovno označeno platformo, ki jo poganja tehnologija SPC Auctions
- Prilagodljiva blagovna znamka (logotip, barve, domena), nastavljiva avkcijska pravila in struktura provizij
- Izolirani podatki po najemniku — podatki vsake stranke so popolnoma ločeni
- Samopostrežni portal za upravljanje najemnikov
- **Prihodkovni model:** mesečna SaaS naročnina + odstotek od transakcij

### Integracija logistike

- **Kalkulator stroškov pošiljanja** — ocenjeni stroški dostave na podlagi dimenzij artikla, teže in razdalje
- **Integracija prevoznikov** — neposredno naročanje pri DHL, UPS, DB Schenker za prevoz težke opreme
- **Načrtovanje prevzema** — usklajevanje datumov prevzema za velike stroje
- **Sledenje pošiljk** — sledilne številke v realnem času vidne kupcu in prodajalcu
- **Zavarovanje** — neobvezno transportno zavarovanje, izračunano po lotu

### Finančne storitve

- **Financiranje kupcev** — obročni načrti za visoko vredne nakupe
- **Storitev depozitnega računa** — varno zadrževanje sredstev za visoko vredne ali čezmejne transakcije
- **Avtomatizirano poročanje VAT** — povzetki VAT po državah za vlaganje napovedi
- **Integracija z računovodstvom** — neposreden izvoz v DATEV (Nemčija), Xero ali druge računovodske sisteme

### Razširitve tržnice

- **Sporočanje med kupci in prodajalci** — komunikacija znotraj platforme za vprašanja o lotih
- **Prodajalne prodajalcev** — namenske strani, ki prikazujejo zaloge in ugled vsakega prodajalca
- **Poročila o stanju** — integracija neodvisnih pregledov tretjih oseb za neodvisno oceno artiklov
- **Primerjava tržnih cen** — primerjava cen lotov s tržnimi podatki
- **Profesionalna orodja za kupce** — množično oddajanje ponudb, shranjena iskanja z opozorili, upravljanje portfelja
- **API dostop** — tretjim osebam omogoča gradnjo integracij prek dokumentiranih naročnin na webhookove

---

## Povzetek

| Faza | Časovnica | Fokus | Status |
|------|-----------|-------|--------|
| **Jedro platforme** | Zaključeno | Avkcije, oddajanje ponudb, plačila, iskanje, obvestila, skladnost | Končano |
| **Faza 1** | Pred zagonom | Plačila v živo, preverjanje identitete, AML, e-pošta, GDPR, varnost | Načrtovano |
| **Faza 2** | Prvih 3 mesecev | Potisna obvestila, DSA, analitika, upravljanje uporabnikov | Vizija |
| **Faza 3** | 3–6 mesecev | Več valut, mobilna aplikacija, avkcijski formati, uvajanje prodajalcev, CO2 | Vizija |
| **Faza 4** | 6–12 mesecev | Umetna inteligenca, bela etiketa, logistika, finance, tržnica | Vizija |

**Jedro avkcijske platforme je dokončano.** Pot do komercialnega zagona zahteva povezavo z resničnimi zunanjimi storitvami (procesor plačil, ponudnik preverjanja identitete, dostava e-pošte) in dokončanje regulativne skladnosti z EU. Arhitektura platforme je zasnovana za podporo vseh načrtovanih funkcionalnosti brez potrebe po ponovnem pisanju — vsaka faza gradi na obstoječih temeljih.
