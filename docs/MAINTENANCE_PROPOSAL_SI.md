# SPC Auctions SPC Auctions - Ponudba za vzdrzevanje in podporo

> Interni cenik - ni namenjen za neposredno distribucijo stranki

---

## Ocena kompleksnosti platforme

Pred dolocanjem cen je pomembno razumeti, kaj se vzdrzuje:

| Dejavnik | Podrobnosti                                      | Kompleksnost |
|----------|--------------------------------------------------|--------------|
| Zaledne storitve | 13 Kotlin/Quarkus mikrostoritev                  | Visoka |
| Uporabniske aplikacije | 4 Vue 3 aplikacije (1 SSR + 3 SPA)               | Visoka |
| Podatkovne baze | 14 PostgreSQL baz + Redis + Elasticsearch         | Visoka |
| Infrastruktura | NATS, Keycloak, MinIO, Prometheus, Grafana, OTel  | Visoka |
| Dogodkovna arhitektura | 40+ domenskih dogodkov, CQRS, event sourcing      | Visoka |
| Skladnost s predpisi | GDPR, AML, DSA, PSD2, EU VAT                      | Visoka |
| Integracije | Placilni prehod, e-posta, S3, iskanje, WebSocket  | Srednje-visoka |
| Vecjezicnost | 8 jezikov                                          | Srednja |
| **Skupno** |                                                    | **Na ravni podjetniskih resitev** |

Primerljiva SaaS platforma na trgu bi samo za zacetno izdelavo stala EUR 50.000-150.000+. Vzdrzevanje mora odrazati to vrednost.

---

## Osnovni paket - EUR 500/mesec (gostovanje vkljuceno)

### Kaj pomeni "osnovni paket"

Osnovno vzdrzevanje zagotavlja, da platforma **deluje, je varna in dostopna** brez kakrsnihkoli sprememb funkcionalnosti ali aktivnega razvoja. Gre za minimum, potreben za nemoteno delovanje - strezniki so nadzorovani, varnostne kopije se izvajajo, varnostni popravki se namescajo, in ce se kaj pokvari, se popravi. Brez novih funkcionalnosti, brez izboljsav, brez preoblikovanja - le ohranjanje delovanja obstojecega sistema.

**V preprostem jeziku za stranko:** "Poskrbimo, da vasa platforma ne preneha delovati, da ni tarci napada in da ne izgubi podatkov. Ce se kaj pokvari, popravimo. Ce zelite kaj novega ali spremenjenega - pripravimo loceno ponudbo."

### Kaj je vkljuceno

**EUR 500/mesec pokriva infrastrukturo za gostovanje + vzdrzevanje:**

| Kategorija | Kaj je vkljuceno | Podrobnosti |
|------------|-------------------|-------------|
| **Infrastruktura za gostovanje** | Namenski streznik Hetzner (16 jeder, 128 GB RAM, 2x 1,92 TB NVMe), S3 objektno shranjevanje, avtomatizirane varnostne kopije, SSL certifikati, DNS, CDN | Vseh 13 storitev, 4 uporabniske aplikacije, vse podatkovne baze, celotna infrastruktura deluje 24/7 |
| **Nadzor streznikov** | Tedenski pregledi nadzornih plos Grafana/Prometheus, avtomatska opozorila o razpolozljivosti | Za tezave izvemo, se preden nas stranka pokrice |
| **Varnostni popravki** | Posodobitve operacijskega sistema, posodobitve Docker slik, kriticni CVE popravki (cetrtletni cikel) | Platforma je zascitena pred znanimi ranljivostmi |
| **Vzdrzevanje podatkovnih baz** | Preverjanje varnostnih kopij, PostgreSQL VACUUM/ANALYZE, nadzor diskovnega prostora | Preprecevanje tihe poskodbe podatkov in zasedenosti prostora |
| **Upravljanje SSL/domen** | Nadzor samodejnega podaljsevanja certifikatov, zdravje DNS | Brez pretecenih certifikatov, brez izpadov zaradi tezav z DNS |
| **Odpravljanje napak** | Do 2 popravki napak mesecno za tezave v obstojecih funkcionalnostih | Kar je prej delovalo in se je pokvarilo, se popravi |
| **Mesecno porocilo o stanju** | Kratko porocilo: razpolozljivost, incidenti, namezceni popravki, priporocila | Stranka pozna stanje svoje platforme |

### Kaj NI vkljuceno

Osnovni paket izrecno **ne** vkljucuje:

- Novih funkcionalnosti
- Sprememb uporabniskega vmesnika ali preoblikovanja
- Novih strani, komponent ali API koncnih tock
- Optimizacije zmogljivosti ali prestrukturiranja kode
- Novih integracij (ponudniki placil, API-ji itd.)
- Dodajanja novih jezikov/prevodov
- Implementacije skladnosti s predpisi (dejanski KYC, AML preverjanje)
- Podpore 24/7 ali med vikendi
- Usposabljanja ali prenosa znanja

### Razdelitev stroskov (interno)

| Postavka | EUR/mesec |
|----------|-----------|
| Namenski streznik Hetzner AX102-U | ~99 |
| Objektno shranjevanje Hetzner (1 TB) | ~5 |
| Varnostna kopija Hetzner Storage Box (1 TB) | ~4 |
| Plavajoce IP | ~4,50 |
| AWS SES e-posta (~10.000 e-postnih sporocil) | ~1 |
| Cloudflare CDN + DNS | 0 |
| Let's Encrypt SSL | 0 |
| Domena (.eu letno, amortizirano) | ~0,50 |
| **Vmesni sesevek infrastrukture** | **~114** |
| **Marza za vzdrzevanje** | **~386** |
| **Skupaj** | **500** |

Pri marzi ~386 to pokriva priblizno 5-6 ur dejanskega vzdrzevalnega dela na mesec - dovolj za tedenske preglede, mesecno namescanje popravkov, 2 popravki napak in porocanje.

---

## Spremembe funkcionalnosti in dodatni razvoj

Vsako delo zunaj obsega osnovnega paketa (nove funkcionalnosti, spremembe oblikovanja, integracije, izboljsave) se obravnava na podlagi **posamezne ponudbe**:

### Kako poteka

1. **Stranka zahteva spremembo** - po e-posti ali dogovorjenem komunikacijskem kanalu
2. **Ocenimo obseg** - analiziramo, kaj je potrebno, katere storitve so prizadete, ocenimo potrebne ure
3. **Poslemo pisno ponudbo** - potrebne ure, urna postavka, pricakovani casovni okvir in skupni strosek
4. **Stranka odobri pred zacetkom dela** - nobeno delo se ne zacne brez pisne odobritve
5. **Implementiramo, testiramo in namestimo** - z ustreznim preverjanjem v testnem okolju
6. **Stranka prejme razcelenjen racun** - porabljene ure, opis opravljenega dela

**Brez presenecenj. Brez skritih stroskov. Brez dela brez predhodnega dogovora.**

### Urne postavke

| Vrsta dela                                   | Postavka | Opombe |
|----------------------------------------------|----------|--------|
| Popravki napak, razvoj funkcionalnosti       | EUR 75/uro | Obracunano po dejanskih urah |
| Nujno delo/delo ob vikendih                  | EUR 130/uro | V nujnih primerih |


---

## Kaj steje kot "popravek napake" in kaj kot "sprememba funkcionalnosti"

| Popravek napake (vkljuceno v paket) | Sprememba funkcionalnosti (locena ponudba) |
|--------------------------------------|--------------------------------------------|
| Nekaj, kar je prej delovalo, je prenehalo | Nekaj, kar ni nikoli obstajalo |
| API vraca napacne podatke | Nova API koncna tocka |
| Pokvarjena komponenta vmesnika | Nova stran ali komponenta |
| Tezave s prijavo/avtentikacijo | Nova uporabniska vloga ali dovoljenje |
| E-posta se ne poslje | Nova vrsta e-postne predloge |
| Iskanje vraca napacne rezultate | Nov iskalni filter |
| Napaka pri izracunu placila | Nov nacin placila |
| Prekinitev WebSocket povezave | Nova funkcionalnost v realnem casu |
| Pokvarjena mobilna odzivnost | Razvoj mobilne aplikacije |
| Manjkajoc/napacen kljuc za prevod | Dodajanje novega jezika |
| Regresija oblikovanja po posodobitvi | Celovito preoblikovanje ali sprememba blagovne znamke |

**Osnovno pravilo:** Ce je nekaj prej delovalo in zdaj vec ne, je to popravek napake. Ce nekaj ni nikoli obstajalo, je to nova funkcionalnost - in zanjo pripravimo ponudbo.

---

## Redni urnik vzdrzevanja (vsi paketi)

```
Tedensko:
  - Pregled nadzornih plos Grafana za nepravilnosti
  - Pregled dnevnikov napak vseh 13 storitev
  - Preverjanje uspesnosti varnostnega kopiranja
  - Pregled diskovnega prostora in rasti podatkovnih baz

Mesecno:
  - Namestitev kriticnih varnostnih popravkov
  - PostgreSQL VACUUM ANALYZE na velikih tabelah
  - Pregled zdravja indeksov Elasticsearch
  - Preverjanje delovanja osvezevanja zetonov Keycloak
  - Posiljanje porocila o stanju stranki

Cetrtletno:
  - Posodobitev osnovnih Docker slik (JRE, Node, nginx)
  - Pregled in zamenjava gesel/skrivnosti
  - Revizija SSL certifikatov
  - Pregled zdravja NATS JetStream uporabnikov
  - Varnostno skeniranje odvisnosti (OWASP + npm audit)

Letno:
  - Vecje nadgradnje ogrodij (locena ponudba, ce presega 4 ure)
  - Pregled arhitekture infrastrukture
  - Vaja obnovitve po katastrofi
  - Koordinacija varnostne revizije
```

---

## Utemeljitev cen

### Zakaj je to ugodna ponudba

| Primerjava | Mesecni strosek |
|------------|-----------------|
| Interni mlajsi DevOps strokovnjak (EU) | EUR 3.000-4.500 (placa + prispevki) |
| Interni visji DevOps strokovnjak (EU) | EUR 5.000-8.000 (placa + prispevki) |
| Agencijski mesecni paket (full-stack, EU) | EUR 3.000-10.000 |
| Vzdrzevanje SaaS platforme (podjetniski nivo) | EUR 5.000-15.000 |
| Splosno upravljano gostovanje | EUR 500-2.000 (brez podpore na ravni aplikacije) |
| **Nas osnovni paket** | **EUR 500 (gostovanje + vzdrzevanje)** |

### Tveganja brez vzdrzevanja

- **Nezakrpana ranljivost** → krsitev podatkov → GDPR kazen do 4 % letnega prometa
- **Podatkovna baza ostane brez prostora** → platforma odpove → izgubljene drazbe in prihodki
- **SSL certifikat potece** → brskalniki blokirajo dostop → kupci ne morejo draziti
- **Tezava z Keycloak zetoni** → nihce se ne more prijaviti → popolna nedostopnost platforme
- **CVE v Docker sliki** → izkoristljiv vektor napada → skoda ugledu

EUR 500/mesec je zavarovanje pred vsem nastetim.

---

## Priporocena zacetna tocka

| Paket | Mesecno | Kaj dobite |
|-------|---------|------------|
| **Osnovni paket** | **EUR 500** | Gostovanje + nadzor + popravki + 2 odpravki napak + porocanje |
| Spremembe funkcionalnosti | Po posamezni ponudbi | Ure se ocenijo, postavka se sporocci, pred zacetkom dela je potrebna odobritev |

**Letni strosek: EUR 6.000** - manj kot en mesec bruto stroska internega razvijalca, platforma pa celotno leto deluje, je varna in vzdrzevana.
