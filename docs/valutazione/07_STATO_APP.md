# 07 — Stato dell'app (snapshot implementato)

**Scopo:** permettere al valutatore di verificare la coerenza tra codice e documenti.
Snapshot fedele dello stato **implementato** alla data indicata.

**Data:** luglio 2026 · **Versione:** v1.2.0-docs (versionCode 37) ·
**applicationId:** `com.daveide.wounddocs` · **Android:** min 8.0 (API 26) ·
**Schema database:** versione 12

## Identità e perimetro

- Nome UI "WoundApp", progetto "WoundApp Docs".
- **100% offline**: nessun permesso INTERNET, nessuna libreria di rete, nessun
  analytics/cloud.
- 4 lingue (IT/EN/FR/ES); per FR/ES i contenuti clinici degli item delle scale restano in
  **inglese** (scelta dichiarata).

## Scale implementate (esattamente queste 3)

| Scala | Item | Range |
|---|---|---|
| Norton (Norton, McLaren & Exton-Smith 1962) | 5 item, ciascuno 1–4 | 5–20 |
| NRS-2002 — **sola componente "final screening"** (ESPEN/Kondrup 2003); pre-screening a 4 domande non incluso (dichiarato) | nutrizione 0–3 + gravità malattia 0–3 + età ≥70 (+1) | 0–7 |
| NRS dolore | scala numerica 0–10; solo le ancore pubblicate (0 = "nessun dolore", 10 = "peggior dolore possibile"), nessun descrittore intermedio 1–9 | 0–10 |

- Solo **somma aritmetica** degli item (punteggio grezzo). **Assenti**: cutoff, soglie,
  categorie/fasce di rischio, colori semantici, interpretazioni, alert, raccomandazioni.
- Cronologia punteggi: **grafico neutro senza delta/frecce/indicatori di tendenza**; per
  ogni scala una legenda che cita la fonte pubblicata (es. "Secondo Norton et al. 1962:
  punteggio più basso = condizione più compromessa").
- Descrizioni item **parafrasate** (mai verbatim dalle pubblicazioni); scale presentate a
  lista con descrizioni visibili.
- **Rimosse in v1.1**: Barthel (copyright attivo — MedChi/Mapi Research Trust, licenza
  richiesta anche per uso commerciale; rimozione prudenziale) e "Valutazione rischio
  cadute" ex-Conley (strumento interno non validato, non pertinente al wound care).
  Già rimosse in precedenza: Braden, BWAT, MNA, MUST (copyright) e TIME/TIMERS (codice
  morto). Gli identificatori legacy (`barthel`, `conley`) eventualmente presenti in backup
  di installazioni precedenti vengono **ignorati all'import**.

## Funzioni presenti

- Anagrafica paziente; mappa corporea (con sedi **Perineo** e **Viso** aggiunte in v1.1 e
  hit-test migliorato); **foto da galleria** tramite picker di sistema (nessuna fotocamera
  in-app, nessuna richiesta di permesso), cifrate all'importazione; selezione ROI poligonale
  **facoltativa** (bottone "Usa foto intera"); composizione tissutale a 4 tessuti
  (necrosi/slough/granulazione/epitelizzazione) in **percentuali manuali** (clamp ≤100%);
  essudato (assente/scarso/moderato/abbondante) con **tipo facoltativo** (sieroso,
  siero-ematico, ematico, purulento); **medicazione primaria/secondaria** come campi
  strutturati con default "Non specificata" e voci quali interfaccia siliconica/contact
  layer, superassorbente, antisettica (PHMB/iodio cadexomero), bendaggio compressivo
  multistrato; **dolore procedurale opzionale** via switch (se non rilevato è salvato come
  assente/null, non 0); default "Non specificato" per i campi clinici (nessun valore
  pre-impostato).
- **Note/anamnesi per lesione** (max 500 caratteri) e **note paziente** in evidenza nella
  scheda; nome paziente visibile nelle schermate figlie; tab predefinita "Lesioni".
- **Data della medicazione impostabile/retrodatabile** (mai nel futuro) e **correzione
  documentale** di data/note di un record esistente (azione `UPDATE_DRESSING`, tracciata
  nell'audit log).
- **Eliminazione** di lesione e di singola medicazione con conferma (foto cifrate incluse),
  tracciata nell'audit log (`DELETE_WOUND`, `DELETE_DRESSING`); eliminazione paziente
  spostata nel menu overflow.
- **Misure iniziali** riferite alla presa in carico: lunghezza = asse maggiore, larghezza
  perpendicolare, profondità limitata a 30 cm; area dichiarata come **"Lunghezza ×
  larghezza (stima)"** (non area planimetrica reale).
- Storico organizzato in sezioni (tessuti/valutazione/medicazione/note) con modifica ed
  eliminazione per record; slider protetti dallo swipe-back di sistema.
- **Bozza anti-timeout cifrata** (EncryptedSharedPreferences) con ripresa al rientro.
- **Foto multiple per medicazione** (v1.2, max 4: 1 principale + 3 angolazioni), con
  carosello a scorrimento nello storico e stampa di tutte le foto (numerate e datate) nel
  report. Puro contenuto fotografico documentale: nessuna analisi, confronto o
  elaborazione automatica delle immagini.
- **Trasferimento a un collega** (v1.2): esportazione cifrata del singolo paziente e
  import con **riconoscimento del paziente già presente** (codice fiscale o anagrafica)
  che aggiunge i soli record mancanti, senza duplicare né sovrascrivere. Il matching è
  deterministico su chiavi identificative dichiarate: nessuna inferenza.
- **Tipo lesione "Altro"** con descrizione libera dell'operatore (v1.2).
- **Nessuna guida/tabella sulle medicazioni** (rimossa integralmente): l'app non offre
  alcun elemento che associ lo stato della ferita a tipi/classi di medicazione.

## Posizione rispetto all'AI Act (Reg. UE 2024/1689)

L'app non contiene sistemi di intelligenza artificiale: nessuna analisi automatica delle
immagini, nessun modello, nessuna inferenza a runtime. I punteggi delle scale sono somme
aritmetiche dichiarate; le percentuali tissutali sono inserite manualmente; il matching
dell'import è deterministico su chiavi identificative. Il Reg. (UE) 2024/1689 non trova
pertanto applicazione al prodotto. Per trasparenza: il codice è stato sviluppato con
l'assistenza di strumenti di IA e sottoposto a revisione; ciò non incide sulla natura del
prodotto (il regolamento disciplina i prodotti che incorporano IA, non gli strumenti di
sviluppo).
- Export: **report di documentazione PDF** per paziente (documento intitolato "REPORT DI
  DOCUMENTAZIONE — LESIONI", file `report_XX_YYYYMMDD_id.pdf`) con, per ogni lesione:
  intestazione con prima/ultima rilevazione, numero di medicazioni e note; tabella
  cronologica (data, % tessuti, essudato, dolore, medicazione); grafico a **barre impilate**
  delle % tessuti per data (solo se ≥2 medicazioni; **nessuna linea di tendenza o media**);
  sequenza di foto numerate e datate con mini-barra dei tessuti. Solo punteggi grezzi +
  disclaimer, zero interpretazione. **Backup cifrato** completo o per singolo paziente, con
  conferma password e nome file proposto con data; **import non distruttivo** (merge con
  rimappamento ID; foto rinominate con nuovo UUID); formati backup v1/v2/v3 leggibili
  (`painNrs` assente nel JSON = non rilevato).

## Funzioni assenti (per scelta, verificabile nel codice)

- Nessuna analisi automatica delle immagini (rimosse anche le entità/use case residui:
  `ImageAnalysisResultEntity`, `AnalyzeImageUseCase`); slider tessuti 100% manuali. Il
  package interno `domain/analysis` è stato rinominato `domain/imaging` e il componente
  `CameraPreview` in `GalleryPhotoPicker`, per coerenza con le funzioni reali.
- Nessun cutoff/interpretazione/alert/raccomandazione; nessun indicatore di tendenza;
  nessuna stadiazione NPUAP/EPUAP.
- Nessuna AI/ML; nessuna rete.

## Database (schema v11)

- Aggiunta colonna `notes` a `wound_assessments`.
- **Drop** delle tabelle `barthel_assessments` e `conley_assessments` (scale rimosse).
- `dressing_events` ricreata con `painNrs` **nullable** + `exudateType` + `primaryDressing`
  + `secondaryDressing`.
- Export/import backup aggiornati e **retro-compatibili** (v1/v2/v3 leggibili).

## Sicurezza (art. 32 GDPR)

- DB Room + SQLCipher AES-256; chiave 256-bit casuale in EncryptedSharedPreferences
  (MasterKey Keystore AES256_GCM), **applicata correttamente** (corretto un bug storico di
  chiave nulla, con migrazione **rekey** dei DB legacy); DB aperto **solo dopo il login**.
- Foto: EncryptedFile AES-256-GCM, nomi UUID. Backup: PBKDF2-HMAC-SHA256 **600.000**
  iterazioni + AES-256-GCM; password minima **12** caratteri; limiti anti-OOM (600 MB file
  / 500 MB decompresso); formati precedenti retro-leggibili.
- Bozza anti-timeout in **EncryptedSharedPreferences** (stessa protezione delle preferenze
  cifrate).
- Login biometrico (BIOMETRIC_STRONG) o credenziale dispositivo; CryptoObject su chiave
  Keystore auth-bound esercitato al login.
- FLAG_SECURE in release; `allowBackup=false`; data_extraction_rules escludono tutto;
  timeout sessione 5 min; root detection con avviso; R8 in release; firma release da
  `keystore.properties` (gitignorato).
- **Audit log locale delle principali operazioni** su pazienti e sessione, **incluse le
  esportazioni** (`EXPORT_PDF`, `EXPORT_PATIENT_BACKUP`) e le operazioni documentali su
  lesioni/medicazioni (`DELETE_WOUND`, `DELETE_DRESSING`, `UPDATE_DRESSING`).

## Modello GDPR

- **Titolare** = professionista/struttura utente; **sviluppatore** = mero fornitore, non
  responsabile ex art. 28 (nessun trattamento, nessun accesso ai dati, offline).
- Base giuridica art. 9(2)(h) + 9(3); il consenso del paziente spetta al titolare.
- Informativa in-app EU (art. 13): finalità, base giuridica, conservazione ≥10 anni
  (D.Lgs. 196/2003), diritti artt. 15–22, reclamo al Garante (art. 77), statement
  "nessuna decisione automatizzata" (art. 22).
- Diritti in-app: accesso/copia (report PDF), portabilità (backup paziente, art. 20),
  rettifica, cancellazione (incluse foto). Dichiarato: la **revoca del consenso in-app non
  è ancora implementata** — miglioria pianificata.
- 3 profili giurisdizione EU/US/ROW con EULA/consenso ramificati; consenso versionato v2 +
  ri-consenso al cambio giurisdizione.

## Verificabilità

Il presente snapshot è verificabile su: `app/build.gradle.kts` (identità, dipendenze
pinnate), `app/src/main/java/com/example/woundmanagement/data/db/AppDatabase.kt` (entità e
versione schema 11), package `security/` (DatabaseKeyManager, BackupManager, PhotoManager,
BiometricCryptoManager, RootDetector), package `domain/imaging` (ex `domain/analysis`),
`AndroidManifest.xml` (permessi, allowBackup, data_extraction_rules).
