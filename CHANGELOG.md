# Changelog

> **Come leggere le versioni.** `versionName` = versione "commerciale" (es. **v1.0**).
> `versionCode` = **build**, un numero **progressivo** che aumenta di **+1 a ogni consegna**:
> la build più alta è la più recente. Versione e build sono riportati nel nome del file APK
> (es. `WoundAppDocs_v1.0_build32.apk`) e in app (schermata Backup).

## [1.6.8-docs] · 2026-11-06

Versione **1.6.8-docs** (versionCode 59). Solo avvisi di licenza, nessun cambiamento clinico o di schema dati.

- **Licenze di terzi nell'app.** In Impostazioni, sotto la licenza d'uso, il pulsante «Licenze di terzi» mostra autori, licenze, file NOTICE e testi completi dei componenti open source contenuti nell'APK (AndroidX, Kotlin, kotlinx.coroutines, Gson, Tink, Guava ListenableFuture, JSpecify, annotazioni JetBrains, SQLCipher per Android con SQLite e LibTomCrypt). Lo chiedono la licenza Apache 2.0 e le licenze BSD di SQLCipher e di ThreeTen (dentro la libreria standard di Kotlin). Lo stesso elenco è citato nel README.

## [1.6.7-docs] · 2026-11-03

Versione **1.6.7-docs** (versionCode 58). Correzioni di sicurezza e di testo da una revisione esterna del codice (nessun cambiamento clinico o di schema dati).

- **Registro attività: le righe arrivate da un backup sono marcate.** Al ripristino le voci del registro contenute nel backup entrano con il prefisso «[da backup]», così non si confondono con le operazioni fatte su questo telefono. Reimportare lo stesso file non le duplica.
- **Accesso: da Android 11 serve la chiave di sicurezza.** Se la chiave del Keystore legata all'autenticazione non è disponibile, l'accesso si ferma con il messaggio «Sblocco sicuro non disponibile. Riavvia il telefono e riprova.», invece di proseguire senza il controllo crittografico.
- **Backup aperti da altre app solo come content://.** L'app non si offre più per i link del browser né per i percorsi file://: un backup si apre dal file manager, da «Condividi» o dalla schermata Backup.
- **Tetto dei 600 MB sempre attivo.** Il file di backup si legge a blocchi e la lettura si ferma oltre i 600 MB anche quando Android non dichiara la dimensione del file.
- **Avviso prima del report PDF.** «Esporta PDF» ora ricorda che il report contiene nomi, codice fiscale e foto del paziente in chiaro, e va condiviso solo con chi deve riceverlo.
- **Testi allineati al codice.** README, SECURITY e documentazione tecnica: il blocco delle catture dello schermo è sempre attivo (non solo nelle build di rilascio), l'intestazione autenticata del backup è già implementata; README e informativa chiariscono che il dipendente di una struttura sanitaria deve avere l'autorizzazione della struttura per usare l'app sul telefono personale.

## [1.6.6-docs] · 2026-11-03

Versione **1.6.6-docs** (versionCode 57).

- **Correzione: selettori del confronto foto leggibili.** In «Confronta», i campi «Foto 1» e «Foto 2» avevano testo, etichetta e bordo scuri sullo sfondo nero: ora sono chiari. Segnalato dalla prova sul telefono.

## [1.6.5-docs] · 2026-11-03

Versione **1.6.5-docs** (versionCode 56). Aggiornamento di sicurezza.

- **Libreria del database aggiornata**: SQLCipher per Android 4.17.0 (SQLite 3.53.3) al posto di android-database-sqlcipher 4.5.4, dismessa, con SQLite 3.41.2 del 2023 e dieci CVE note di SQLite (fra cui CVE-2025-6965, CVE-2025-7709, CVE-2026-11822, CVE-2026-11824). Servono SQL scritto dall'attaccante o un database costruito apposta, che l'app non accetta: è un aggiornamento preventivo. Stesso formato dei database e stessa chiave: i dati esistenti si aprono senza conversioni. Il recupero dei vecchi database a chiave nulla ora fa il rekey con gli stessi byte che usa l'apertura.
- Controllo di tutte le 108 librerie sul database pubblico OSV il 3/11/2026: nessun'altra vulnerabilità nota.

## [1.6.4-docs] · 2026-11-03

Versione **1.6.4-docs** (versionCode 55). Prima build di rilascio firmata con la chiave di rilascio (le precedenti erano build di debug).

- **Tolto "Offrimi un caffè"** dalla schermata pazienti e dalle Impostazioni, e l'articolo sulle donazioni dalla licenza: l'app esce gratis su Google Play, che non ammette donazioni fuori dal suo sistema di pagamento.

## [1.6.3-docs] — 2026-10-09

Versione **1.6.3-docs** (versionCode 54). Correzione del pulsante "Confronta" nello Storico e ordine delle tendine medicazione (nessun cambiamento clinico o di schema dati).

- **Correzione: intestazione della lesione nello Storico.** Con un nome o un tipo lungo (basta il nome suggerito "Rachide/Spinose vertebrali 1" con il tipo "Dermatite associata a umidità (MASD/IAD)") il riquadro diventava altissimo e quasi vuoto e il pulsante "Confronta" veniva schiacciato fino a sparire. Ora il pulsante ha sempre la sua larghezza e resta su una riga: sta accanto al testo quando c'è spazio, altrimenti scende in basso a destra sulla riga del conteggio, e nome e tipo vanno a capo come nelle lesioni senza pulsante. Verificato con un test di rendering su schermi da 360 e 393 dp, carattere al 100% e al 130%, in italiano e spagnolo.
- **Tendine medicazione primaria e secondaria**: "Nessuna" ora è in cima, subito sotto "Non specificata", invece che in ordine alfabetico sotto la N (in tutte e 4 le lingue). Poi i prodotti dalla A alla Z.

## [1.6.2-docs] — 2026-09-01

Versione **1.6.2-docs** (versionCode 53). Secondo giro di controllo: sicurezza, pulizia e correzioni.

- **Permessi ridotti al minimo**: l'app non chiede più accesso alle immagini (`READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`). Il selettore di sistema restituisce la foto con un'autorizzazione temporanea e non richiede permessi. Restano solo i due permessi biometrici, e nessun accesso a internet.
- **Avviso dispositivo compromesso di nuovo funzionante**: da Android 11 il controllo sulle app di root era sempre negativo perché il sistema nasconde i pacchetti non dichiarati; ora sono dichiarati nel manifest.
- **Niente punteggi doppi**: due tocchi rapidi su "Salva" nelle tre scale (Norton, NRS-2002, dolore) potevano registrare due volte la stessa rilevazione.
- **Niente chiusura dell'app allo scadere della sessione** mentre sono aperti l'elenco pazienti o il registro attività: l'errore della lettura del database ora è gestito e si viene portati alla schermata di accesso.
- **Report PDF**: con moltissime rilevazioni la riga delle scale poteva superare l'altezza della pagina e venire tagliata senza dirlo; ora mostra le più recenti e dichiara quante ne restano fuori.
- **Pulizia**: rimosso il codice non più usato dopo l'eliminazione del ritaglio della lesione (selettore e processore ROI, circa 570 righe) e altri componenti orfani.

## [1.6.1-docs] — 2026-09-01

Versione **1.6.1-docs** (versionCode 52). Pulsante donazioni in home e giro di correzioni (nessun cambiamento clinico o di schema dati).

- **"Offrimi un caffè" nella schermata pazienti**: icona nell'header che apre una finestra con la spiegazione (donazione volontaria, non sblocca funzioni, pagamento su PayPal fuori dall'app) e il pulsante per la pagina di donazione. La stessa nota compare anche in Impostazioni.
- **Correzione: medicazione non visibile nello storico** quando sia la primaria sia la secondaria erano "Altro" con testo libero (la sezione Medicazione restava vuota).
- **Correzione: conferma "Salvato"** dopo una medicazione — il messaggio veniva cancellato dalla navigazione e spesso non si vedeva.
- **Correzione: sessione che scadeva durante backup, anteprima o ripristino** lunghi (nessun tocco sullo schermo per minuti): il timer di inattività ora resta sospeso finché l'operazione è in corso, anche durante gli export della scheda paziente.
- **Correzione: chiave del database** scritta su disco in modo sincrono alla prima creazione (con la scrittura asincrona, un'interruzione del processo poteva rendere il database illeggibile).
- **Correzione: salvataggio del file di backup** spostato fuori dal thread dell'interfaccia (blocco su archivi grandi) e avviso esplicito se il salvataggio non riesce.
- **Correzione: modifica dei dati del paziente** non più annullabile da un "indietro" immediato.
- **Confronto foto**: disponibile anche quando le due foto appartengono alla stessa medicazione; pulsante "Chiudi" quando le foto non bastano.
- **Nome suggerito della lesione** ora nella lingua dell'app (prima era sempre in italiano).
- Digitazione delle note più fluida (le preferenze cifrate non vengono più ricreate a ogni tasto); backup completo che non satura più la cache delle miniature; misure della lesione con formato uniforme nell'elenco per zona.

## [1.6.0-docs] — 2026-07-23

Versione **1.6.0-docs** (versionCode 51). Restyling grafico dell'interfaccia (nessun cambiamento clinico o di dati).

- **Schermata di benvenuto** ridisegnata: logo a gradiente indaco, pulsante d'accesso a gradiente, nota "non è un dispositivo medico".
- **Lista pazienti**: nuovo header a gradiente con titolo, conteggio assistiti e ricerca integrata; card con avatar a iniziali e angoli morbidi; pulsante "+" arrotondato in tinta.
- **Scheda paziente**: barra superiore a gradiente e tab (Scale · Lesioni · Storico) trasformate in "pillole" segmentate.
- Nuovi componenti grafici condivisi (header, avatar, tab, chip) per coerenza tra le schermate.

## [1.5.3-docs] — 2026-07-23

Versione **1.5.3-docs** (versionCode 50).

- **Confronto sovrapposto: allinea le foto col dito** — in modalità "Sovrapposte" la seconda foto ora si può trascinare e pizzicare (zoom) per allinearla alla prima e valutare meglio l'evoluzione; pulsante "Reimposta" per tornare al centro.
- **Grafica nell'app**: illustrazioni nello stato vuoto della lista pazienti e dello storico foto, in stile indaco coordinato (generate con GPT Image 2).

## [1.5.1-docs] — 2026-07-23

Versione **1.5.1-docs** (versionCode 48).

- **Icona rivista**: sostituita con la variante a **busto** (testa e spalle) con il segnaposto della lesione, più chiara e senza ambiguità rispetto alla figura a corpo intero.

## [1.5.0-docs] — 2026-07-23

Versione **1.5.0-docs** (versionCode 47). Restyling grafico.

- **Nuova icona dell'app**: silhouette del corpo con un segnaposto sulla lesione (mappa corporea), generata con un modello immagini top di gamma (GPT Image 2) e integrata come adaptive icon su tutte le densità.
- **Palette dell'app allineata all'icona (indaco)**: il colore principale passa dal teal a un **indaco** curato (Material 3, tonalità coerenti), per un look più uniforme e moderno su tutte le schermate. Nessun cambiamento funzionale; l'app resta sempre chiara.
- New app icon (body map with lesion pin, generated with GPT Image 2) + cohesive indigo Material 3 palette across the app.

## [1.4.1-docs] — 2026-07-23

Versione **1.4.1-docs** (versionCode 46).

- **Confronto tra due foto** della stessa lesione: nello storico, sul titolo di una lesione con almeno due foto, il pulsante "Confronta" apre una vista dove scegli due foto (per data) e le vedi **affiancate** oppure **sovrapposte** con opacità regolabile, per valutare l'evoluzione a colpo d'occhio. È solo visualizzazione: nessuna evidenziazione o analisi automatica (l'app resta di sola documentazione).
- Compare two photos of the same lesion, side by side or overlaid with adjustable opacity (visual only, no automatic analysis).

## [1.4.0-docs] — 2026-07-23

Versione **1.4.0-docs** (versionCode 45, schema database v13). Primo blocco del feedback betatest.

- **Misure per ogni medicazione**: nel flusso puoi inserire lunghezza, larghezza e profondità (con unità mm/cm) a ogni medicazione, non solo alla presa in carico, così da seguire l'evoluzione della lesione nel tempo. Le misure compaiono nello storico e in una nuova colonna della tabella cronologica del report.
- **Tendina medicazioni in ordine alfabetico**: i prodotti (Schiuma, Alginato, Idrocolloide…) nelle tendine medicazione primaria/secondaria sono ordinati A→Z (con "Non specificata" in cima), per trovarli più in fretta.
- Per-dressing measurements (length/width/depth, mm/cm) to track evolution; dressing-product dropdowns sorted alphabetically.

## [1.3.5-docs] — 2026-07-18

Versione **1.3.5-docs** (versionCode 44).

- **Lesione abbandonata rimossa**: se crei una nuova lesione ("Salva lesione") e poi esci dalla medicazione senza salvarla, la lesione appena creata viene eliminata automaticamente (niente lesioni vuote nello storico). Vale sia per la freccia indietro sia per il gesto di sistema; una lesione esistente non viene mai toccata.
- **Lesione senza foto consentita, con avviso**: si può procedere anche senza foto; compare un avviso che raccomanda la documentazione fotografica (buona pratica di wound care con strumenti iconografici). Nello storico la medicazione senza foto mostra un placeholder "Senza foto".
- Abandoned wound is auto-removed if you leave without saving a dressing; dressings without a photo are allowed with a recommendation notice.

## [1.3.4-docs] — 2026-07-18

Versione **1.3.4-docs** (versionCode 43).

- **Inserimento foto rifatto**: miniature grandi (non più i quadratini piccoli) e **riordino con drag & drop** — tieni premuta una foto e trascinala; **portandola in prima posizione diventa la copertina** (via la stella confusa di prima). La prima foto ha il badge "Principale", ogni foto una X netta per rimuoverla; quando non ci sono foto, un grande riquadro "Aggiungi foto".
- Photo input redesigned: large thumbnails and **drag-and-drop reordering** (long-press and drag; drop first = cover). Clear "cover" badge and remove button.

## [1.3.3-docs] — 2026-07-18

Versione **1.3.3-docs** (versionCode 42).

- **Foto a schermo intero più definite**: aprendo una foto (singola o nel carosello) ora viene caricata a piena risoluzione (fino a 2048 px) invece che a 800 px, così lo zoom resta nitido. La miniatura fa da anteprima immediata mentre la versione piena si carica.
- **Foto principale cambiabile**: caricando le foto, con la stella su una qualunque foto la si promuove a copertina (la vecchia principale scala tra le angolazioni). Prima la copertina era fissa alla prima caricata.
- Fullscreen photos now load at full resolution (crisper zoom); the cover photo can be changed with the star button while adding photos.

## [1.3.2-docs] — 2026-07-18

Versione **1.3.2-docs** (versionCode 41).

- **Profondità in millimetri o centimetri**: nella nuova lesione la profondità ha un selettore di unità (mm/cm), perché spesso si misura in mm. Cambiando unità il valore si converte da solo (8 mm resta 8 mm, non diventa 8 cm). Internamente la misura resta in cm; nel report la profondità è mostrata in mm quando è sotto il centimetro.
- Le etichette Lunghezza/Larghezza indicano esplicitamente i cm.
- **Depth in mm or cm** (unit toggle) for wound depth; often measured in mm. Value auto-converts on unit change; report shows sub-centimetre depth in mm.

## [1.3.1-docs] — 2026-07-18

Versione **1.3.1-docs** (versionCode 40).

- **Ritaglio della lesione rimosso del tutto**: su richiesta, non è più presente nemmeno come opzione. Le foto si documentano intere. Il flusso resta a 3 passaggi (Foto → Tessuti → Note); nella tappa Tessuti la foto della lesione è mostrata intera come riferimento visivo.
- Lesion cropping (ROI) removed entirely (not even optional). Photos are documented whole.

## [1.3.0-docs] — 2026-07-17

Versione **1.3.0-docs** (versionCode 39). Flusso di aggiunta medicazione ridisegnato dal feedback del betatest.

### Flusso foto ridisegnato / Redesigned photo flow
- **Caricamento di più foto in una volta**: il selettore galleria ora è multi-selezione (fino a 4). La prima foto è la copertina, le altre le angolazioni. Prima si poteva scegliere una foto per volta.
- **Ritaglio della lesione facoltativo**: non è più una tappa obbligata. Dopo aver caricato le foto si va dritti ai tessuti; il ritaglio è un bottone opzionale ("Ritaglia la lesione") con editor a schermo intero, apribile e rifacibile quando si vuole.
- Il flusso passa da **4 a 3 passaggi** (Foto → Tessuti → Note). La sezione foto è ora la prima cosa che si vede, non più sepolta nell'ultimo passaggio.
- Robustezza (da verifica adversarial): le foto scelte oltre il tetto di 4 non vengono più scritte su disco (niente file orfani); l'abbandono della schermata durante la copia multipla pulisce i file non referenziati; ripresa bozza coerente col nuovo flusso.

### Multiple photos and optional cropping (EN)
- Gallery picker is now multi-select (up to 4 photos at once); first photo is the cover.
- Lesion cropping (ROI) is now **optional** (a button with a full-screen editor), no longer a mandatory step. Flow reduced from 4 to 3 steps (Photos → Tissue → Notes).

## [1.2.0-docs] — 2026-07-17

Versione **1.2.0-docs** (versionCode 37, schema database v12). Giro guidato dal feedback dei betatester.

### Foto multiple e carosello / Multiple photos and carousel
- **Fino a 4 foto per medicazione**: 1 principale (la copertina, su cui si delimita l'area) + 3 angolazioni extra, aggiungibili nel flusso e rimovibili prima del salvataggio. Nuova tabella `dressing_photos` (migration 11→12, non distruttiva: le foto esistenti diventano la "principale").
- **Carosello nello storico**: badge col numero di foto sulla miniatura; a schermo intero si scorre tra le foto (swipe) con zoom a due dita per pagina e indicatore di posizione.
- **Report PDF**: stampa tutte le foto della medicazione, numerate e datate.

### Invio a un collega e import senza doppioni / Send to colleague and duplicate-free import
- La voce di menu diventa **"Invia paziente a un collega"**, con istruzioni nel dialog (file cifrato → WhatsApp/email → il collega lo apre con l'app e inserisce la password).
- **Import = merge intelligente**: il paziente già presente viene riconosciuto (codice fiscale; in mancanza, cognome+nome+data di nascita, con blocco in caso di omonimi ambigui) e vengono aggiunte SOLO lesioni, medicazioni, foto e scale nuove. Reimportare lo stesso file non crea doppioni; l'esito mostra quanti pazienti sono nuovi e quanti aggiornati. Le foto vengono scritte prima della conferma dei dati (nessun record può restare senza foto), con riparazione automatica dei file mancanti al re-import.
- Limite noto: se si corregge la data di una medicazione e poi si reimporta un backup precedente, quella medicazione può comparire due volte (caso raro, accettato).

### Correzioni dal betatest / Betatest fixes
- **Data di nascita**: inserimento a sole cifre con gli slash mostrati automaticamente (creazione E modifica; prima nella modifica era impossibile digitare la data); i valori storici in formato libero non vengono più visualizzati in modo fuorviante.
- **Misure**: accettata la **virgola decimale** (es. profondità `0,5` cm) oltre al punto.
- **Tipo lesione "Altro"**: campo di descrizione libera, riportato in scheda, storico e PDF.
- Rifiniture da revisione interna: gesto zoom del carosello che non blocca lo swipe, pulizia dei file foto abbandonati (uscita senza salvare/scarto bozza), tetto 4 foto non aggirabile, miniature a basso consumo di memoria, operazioni di ripristino/eliminazione non interrompibili dalla navigazione.

### Trasparenza / Transparency
- **Nota AI Act** (Reg. UE 2024/1689) nei documenti: l'app non contiene sistemi di IA a runtime (nessuna analisi automatica, nessun modello); il regolamento non trova applicazione. Il codice è stato sviluppato con l'assistenza di strumenti di IA e revisionato.

## [1.1.0-docs] — 2026-07-06

Versione **1.1.0-docs** (versionCode 33, schema database v11).

### Scale / Scales
- **Set ridotto a 3 scale**: Norton (Norton, McLaren & Exton-Smith 1962; somma 5–20, solo punteggio grezzo), NRS-2002 nella sola componente "final screening" (ESPEN/Kondrup 2003; 0–7; il pre-screening a 4 domande non è incluso, e ciò è dichiarato), NRS dolore 0–10 (solo le ancore pubblicate 0 = "nessun dolore" e 10 = "peggior dolore possibile", nessun descrittore intermedio).
- **Rimossa la scala Barthel**: copyright attivo (titolare MedChi — Maryland State Medical Society, licensing tramite Mapi Research Trust/ePROVIDE, licenza richiesta anche per uso commerciale); rimozione prudenziale.
- **Rimossa la "Valutazione rischio cadute"** (ex-Conley): strumento interno non validato e non pertinente al wound care.
- Gli identificatori legacy (`barthel`, `conley`) eventualmente presenti in backup di installazioni precedenti vengono **ignorati all'import**.
- Scale presentate **a lista con descrizioni visibili**.

### Conformità non-DM / Non-device compliance
- **Terminologia**: "referto" sostituito ovunque da **"report di documentazione"** (il PDF si intitola "REPORT DI DOCUMENTAZIONE — LESIONI", file `report_XX_YYYYMMDD_id.pdf`); "monitoraggio" sostituito da **"registrazione e consultazione cronologica"**.
- **Cronologia punteggi senza indicatori di tendenza**: nessun delta, freccia o indicatore; grafico neutro con legenda per scala che cita la fonte pubblicata (es. "Secondo Norton et al. 1962: punteggio più basso = condizione più compromessa").
- **Report PDF con andamento temporale per lesione**, puramente documentale: intestazione con prima/ultima rilevazione, numero di medicazioni e note; tabella cronologica (data, % tessuti, essudato, dolore, medicazione); grafico a **barre impilate** delle % tessuti per data (solo se ≥2 medicazioni; nessuna linea di tendenza o media); foto numerate e datate con mini-barra dei tessuti.
- **Misure dichiarate**: area come **"Lunghezza × larghezza (stima)"** (non area planimetrica reale); lunghezza = asse maggiore, larghezza perpendicolare; profondità limitata a 30 cm; misure riferite alla presa in carico ("Misure iniziali").

### Funzioni / Features
- **Note/anamnesi per lesione** (max 500 caratteri) e **note paziente** in evidenza nella scheda.
- **Eliminazione** di lesione e di singola medicazione con conferma (foto cifrate incluse), tracciata nell'audit log (`DELETE_WOUND`, `DELETE_DRESSING`).
- **Data della medicazione impostabile/retrodatabile** (mai nel futuro) e **correzione documentale** di data/note di un record esistente (`UPDATE_DRESSING`, audit-tracciata).
- **ROI facoltativa**: bottone "Usa foto intera" in alternativa alla selezione poligonale.
- **Dolore procedurale opzionale** via switch (se non rilevato è salvato come assente, non come 0).
- **Tipo di essudato facoltativo**: sieroso, siero-ematico, ematico, purulento.
- **Medicazione primaria/secondaria** come campi strutturati con default "Non specificata" e nuove voci (interfaccia siliconica/contact layer, superassorbente, antisettica PHMB/iodio cadexomero, bendaggio compressivo multistrato).
- Default **"Non specificato"** per i campi clinici (nessun valore pre-impostato).
- Mappa corporea: nuove sedi **Perineo** e **Viso**; hit-test migliorato.
- **Bozza anti-timeout cifrata** (EncryptedSharedPreferences) con ripresa al rientro.
- Selezione foto dalla galleria **senza richiesta di permesso** (picker di sistema).

### Usabilità / UX
- Nome paziente visibile nelle schermate figlie; tab predefinita "Lesioni".
- **Conferma password** nel backup + nome file proposto con data.
- Eliminazione paziente spostata nel **menu overflow**.
- **Storico a sezioni** (tessuti/valutazione/medicazione/note) con modifica ed eliminazione per record.
- Slider protetti dallo **swipe-back** di sistema.

### Database / Backup
- Schema **v11**: colonna `notes` su `wound_assessments`; drop di `barthel_assessments` e `conley_assessments`; `dressing_events` ricreata con `painNrs` nullable + `exudateType` + `primaryDressing` + `secondaryDressing`.
- Export/import backup aggiornati e **retro-compatibili** (v1/v2/v3 leggibili; `painNrs` assente nel JSON = non rilevato).

### Interno / Internal
- Rinominati il package `domain/analysis` → `domain/imaging` e il componente `CameraPreview` → `GalleryPhotoPicker`, per coerenza con le funzioni reali.

## v1.0 (build 32) — 11 giugno 2026

Prima release **v1** completa. Comprende la revisione di conformità (build 31, sotto) e, in più, questo ciclo di rifiniture:

### Usabilità / UX
- Tema **sempre chiaro** (rimosso lo scuro automatico). **Screenshot/registrazione schermo bloccati sempre** (FLAG_SECURE anche in debug).
- Corretto il **caricamento foto dalla galleria** (regressione); **pinch-to-zoom** sulla selezione dell'area lesione; **thumbnail più nitide**.
- **Backup e Ripristino in due pagine separate**.
- **Rimossa la guida/tabella sulle medicazioni** (rafforza la qualificazione non-DM).

### Mappa corporea
- Marker delle zone **ricalibrati** sui landmark anatomici (centrati, simmetrici); braccia/spalle/sotto-ginocchio sistemati; punti vicini (testa, sacro) **distanziati** per evitare tocchi sul punto sbagliato.

### Lingue
- Audit e correzione delle **traduzioni** IT/EN/FR/ES (~60 stringhe).

### Sicurezza (hardening dopo audit dedicato)
- Nomi file export **senza dati del paziente** (iniziali+data); **audit log degli export**; **chiusura del DB al timeout** (i dati non restano in RAM a sessione scaduta); **backup formato v3** con header autenticato (anti-manomissione); validazione CF/data in modifica paziente; log ripuliti in release; `taskAffinity`.

---

## v1.0 (build 31) — revisione di conformità (giugno 2026)

Revisione completa per allineare l'app allo stato dichiarato nel pacchetto di valutazione.

### Scale / Scales
- **Rimosse le scale protette da copyright**: Braden, BWAT, MNA, MUST. Rimossi anche i framework TIME/TIMERS (codice morto). Set attuale: Norton, Barthel, Valutazione rischio cadute (interna), NRS-2002 (screening finale), NRS dolore.
- **Rinominata la scala cadute**: non più "Conley modificata" ma "Valutazione rischio cadute" — strumento interno, dichiarato **non validato**.
- **NRS-2002** etichettata esplicitamente come **screening finale** (il pre-screening a 4 domande non è incluso, e ciò è dichiarato).
- Descrizioni degli item **parafrasate** dalle fonti (nessuna riproduzione verbatim); rimossi tutti i cutoff/categorie residui dalla documentazione.

### Sicurezza / Security
- **Fix chiave database SQLCipher**: corretto un bug per cui il DB veniva cifrato con chiave nulla; ora la chiave reale a 256 bit è applicata, con **migrazione rekey** automatica dei database legacy.
- Database aperto **solo dopo il login** (biometrico BIOMETRIC_STRONG o credenziale dispositivo, CryptoObject su chiave Keystore auth-bound).
- **FLAG_SECURE sempre attivo** (anche nei build di debug): screenshot e registrazione schermo bloccati su tutte le schermate.
- **signingConfig release** da `keystore.properties` (gitignorato).
- Password minima backup portata a **12 caratteri**.
- Schema database a **versione 10** (potatura tabelle delle scale rimosse e dei residui di analisi immagine).

### Funzioni / Features
- **Rimossi i residui di analisi automatica delle immagini** (entità `ImageAnalysisResultEntity`, `AnalyzeImageUseCase`): le percentuali tissutali sono esclusivamente manuali.
- **Guida/riferimento sulle medicazioni RIMOSSA**: prima spostata dal flusso a Impostazioni, poi eliminata del tutto. L'app non contiene alcuna tabella che associ lo stato della ferita a tipi/classi di medicazione (rafforza la qualificazione non-DM).
- **Backup e Ripristino separati** in due pagine distinte nelle Impostazioni.
- `applicationId` cambiato in `com.daveide.wounddocs`.

### Usabilità / UX
- Risolta la **perdita silenziosa dei punteggi** delle scale (cancellazione del salvataggio alla navigazione) e i **doppi salvataggi** (medicazione, lesione).
- La **rotazione dello schermo non azzera più** il form in corso; il timer di inattività non scade mentre si digita una nota.
- **Pinch-to-zoom** sulla foto nella selezione dell'area lesione (ROI); **thumbnail** più nitide.
- Errori di backup **localizzati** (password errata distinta da file non valido); **tastiera numerica** sulle misure.
- Foto importate da backup **rinominate** (no condivisione tra pazienti); etichette enum **localizzate** nello storico.

### Mappa corporea / Body map
- Marker delle zone **ricalibrati** sui landmark anatomici (centrati nel distretto, bilateralmente simmetrici) e **distanziati** dove erano troppo vicini (testa, sacro/glutei) per evitare tocchi sul punto sbagliato.

### Lingue / Languages
- App in **4 lingue (IT/EN/FR/ES)**; audit e correzione delle traduzioni dell'interfaccia. Le descrizioni cliniche degli item delle scale restano in IT/EN (per FR/ES ripiegano sull'inglese, per scelta dichiarata).

### Documentazione / Documentation
- Riallineata tutta la documentazione allo stato reale; nuovo **SECURITY.md** (vulnerability disclosure, nota CRA), **PRIVACY_POLICY v2** (art. 77/Garante, conservazione ≥10 anni, art. 22), scheda **07_STATO_APP**.

> Nota di lettura: le voci sottostanti (2026-05-28/30) descrivono lo stato **PRE-fork/pre-revisione** e citano scale (Braden, BWAT, MNA, MUST, Conley, TIME/TIMERS) e dettagli (cutoff Shah, descrizioni "dalle pubblicazioni originali", grafico BWAT) **non più presenti** nell'app corrente. Sono conservate come storico.

## 2026-05-30 — aggiornamenti / updates (v1.0.0-docs) — STATO PRE-REVISIONE

### Conformita non-DM / Non-device compliance
- Interpretazione clinica rimossa anche da **dominio, database e backup** (prima solo dalla UI): le scale non calcolano piu categorie/fasce di rischio, restano solo i punteggi grezzi. Migrazione database v6 -> v7. Risolve anche un bug del MNA in modalita solo-screening.

### Export e trasferimento / Export and transfer
- **Referto PDF** del decorso per paziente, con foto progressive raggruppate per sede di ferita.
- **Backup cifrato del singolo paziente**.
- **Import non distruttivo (merge)**: aggiunge i pazienti senza cancellare quelli locali, con rimappatura degli identificativi e ricostruzione delle relazioni.
- Icone informative ("i") sulla schermata backup e su tutte le scale (descrizioni fattuali dello strumento).

### Sicurezza / Security
- Backup: **PBKDF2 210K -> 600K** iterazioni (OWASP 2024); formato versione 2 (legge ancora i backup precedenti).
- Robustezza import su backup legacy/parziali; controllo dimensione file anti-OOM; fix lettura header.

### Documentazione / Documentation
- **Dossier di valutazione** in `docs/valutazione/` (non-DM, GDPR, sicurezza, scale e fonti, checklist).

## v1.0.0-docs (2026-05-28) — STATO PRE-REVISIONE

Versione di documentazione. Nessuna funzione di diagnosi, trattamento o supporto decisionale.

> ⚠️ Voce storica: le scale Braden/BWAT/MNA/MUST, la "Conley modificata", il cutoff Shah e i riferimenti TIME/TIMERS qui sotto descrivono lo stato pre-revisione e **non** sono presenti nell'app corrente (vedi entry 2026-06 in cima).

Documentation version. No diagnosis, treatment, or decision support features.

### Documentazione ferite / Wound documentation
- Foto cifrate con timeline per lesione
- Selezione ROI poligonale con drag, punti grandi, zona chiusura, auto-close
- Composizione tissutale manuale (4 slider: necrotico, fibrina, granulazione, epitelizzazione)
- Foto ferita croppata visibile nello step tessuti
- Mappa corporea 36 zone (anteriore + posteriore)
- Guida rapida medicazioni (riferimento EWMA/EPUAP/WUWHS/AIUC)

### Scale cliniche / Clinical scales
- Braden (Bergstrom & Braden 1987)
- Norton (Norton et al. 1962)
- BWAT a 13 parametri (Bates-Jensen 2001)
- MNA con mezzi punti (Vellas et al. 1999)
- MUST (BAPEN, Elia 2003)
- Barthel con cutoff Shah 1989
- Conley modificata (adattamento italiano)
- NRS Dolore con sede, tipo, carattere, fattori
- Descrizioni bilingui IT/EN dalle pubblicazioni originali
- Solo punteggio numerico, nessun colore rischio, nessun alert

### Sicurezza / Security
- SQLCipher AES-256 per database
- EncryptedFile AES-256-GCM per foto
- Android Keystore + CryptoObject biometrico (API 30+)
- Biometria o PIN dispositivo all'avvio
- Session timeout 5 min (foreground + background)
- Root detection con avviso
- Backup cifrato PBKDF2 210K + AES-256-GCM, restore atomico
- FLAG_SECURE, allowBackup=false, R8 obfuscation
- Audit trail completo
- Nessun permesso INTERNET
- security-crypto 1.1.0 stabile

### Performance
- Cache thumbnail LruCache con inSampleSize
- Caricamento foto su Dispatchers.IO
- Restore backup con downscale foto, largeHeap

### Altro / Other
- Bilingue IT/EN con cambio runtime
- Limiti lunghezza su tutti i campi testo
- Messaggi errore sanitizzati (nessun dettaglio tecnico in UI)
- Grafico BWAT con date asse X, limite 8 entry
- Privacy policy bilingue
- Disclaimer MDR/FDA
