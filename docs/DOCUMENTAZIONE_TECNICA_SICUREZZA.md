# WoundApp — Documentazione Tecnica e di Sicurezza

**Versione:** v1.0 (build 32)  
**Data documento:** giugno 2026  
**Classificazione:** Uso interno — Documentazione per IT e DPO

---

## 1. Panoramica dell'Applicazione

### 1.1 Descrizione

WoundApp (progetto "WoundApp Docs") e' un'applicazione Android per la **documentazione clinica delle lesioni cutanee**. Consente agli operatori sanitari di registrare pazienti, documentare ferite con fotografie (selezionate dalla galleria del dispositivo e cifrate all'importazione), compilare scale di valutazione e organizzare cronologicamente le informazioni inserite.

### 1.2 Cosa NON e'

WoundApp è uno **strumento di documentazione clinica**. Non fornisce:
- Diagnosi
- Indicazioni terapeutiche personalizzate
- Supporto decisionale clinico
- Analisi automatica delle immagini (nessuna AI/ML; i residui di analisi immagine sono stati rimossi anche dal modello dati)
- Cutoff, fasce di rischio, interpretazioni o stadiazioni dei punteggi

Le informazioni mostrate hanno carattere **puramente documentale e informativo**. Il professionista sanitario e' l'unico responsabile di tutte le valutazioni e decisioni cliniche.

### 1.3 Principio fondamentale: TUTTO LOCALE

L'applicazione **non possiede permesso INTERNET** e non contiene codice di rete. Nessun dato esce dal dispositivo in alcun modo automatico. Il backup e' manuale, cifrato e condiviso solo su esplicita azione dell'operatore.

### 1.4 Destinatari

Professionisti sanitari soggetti al segreto professionale ai sensi dell'Art. 9(3) GDPR. L'utilizzo richiede accettazione esplicita dell'EULA e dell'informativa privacy.

### 1.5 Lingue

4 lingue: italiano, inglese, francese, spagnolo (cambio a runtime). Nota dichiarata: per FR/ES i **contenuti clinici degli item delle scale** restano in **inglese** (non sono tradotti in FR/ES).

---

## 2. Architettura Tecnica

### 2.1 Stack tecnologico

| Componente | Tecnologia | Versione |
|-----------|------------|----------|
| Linguaggio | Kotlin | 1.9.20 |
| UI Framework | Jetpack Compose | BOM 2024.02.02 |
| Design System | Material 3 | (da BOM) |
| Database | Room + SQLCipher | Room 2.6.1, SQLCipher 4.5.4 |
| Crittografia | AndroidX Security Crypto | 1.1.0 (stabile) |
| Autenticazione | AndroidX Biometric | 1.1.0 |
| Navigazione | Navigation Compose | 2.7.6 |
| Foto | Selezione da galleria di sistema + cifratura all'import (nessuna libreria fotocamera) | — |
| Build | AGP 8.2.0, Gradle 8.5 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Compile SDK | 34 |
| Java | 17 |
| Application ID | `com.daveide.wounddocs` |

### 2.2 Struttura del database

**Database:** `wound_management_mdr.db` — cifrato con SQLCipher  
**Versione schema:** 11  
**Migrazioni rilevanti:** oltre alle migrazioni storiche, la migrazione alla v10 ha potato le tabelle delle scale rimosse (Braden, BWAT, MNA, MUST, TIME, TIMERS) e i residui dell'analisi immagine; la migrazione alla **v11** (release 1.1.0-docs) ha aggiunto la colonna `notes` alle lesioni, eliminato le tabelle `barthel_assessments`/`conley_assessments` (scale rimosse) e ricreato `dressing_events` con dolore opzionale (nullable) e i campi strutturati tipo essudato / medicazione primaria / medicazione secondaria; e' inoltre prevista una migrazione di **rekey** dei database legacy (vedi §3.1).

#### 7 Tabelle

| Tabella | Descrizione | FK Parent |
|---------|-------------|-----------|
| `patients` | Anagrafica pazienti | — |
| `wound_assessments` | Lesioni registrate | patients (CASCADE) |
| `dressing_events` | Medicazioni effettuate | wound_assessments (CASCADE) |
| `norton_assessments` | Scala Norton | patients (CASCADE) |
| `nrs2002_assessments` | NRS-2002 (screening finale) | patients (CASCADE) |
| `pain_nrs_assessments` | Scala dolore NRS | patients (CASCADE) |
| `audit_log` | Registro attivita' | — |

Tutte le tabelle figlie hanno `ON DELETE CASCADE`: l'eliminazione di un paziente rimuove automaticamente tutti i dati associati.

### 2.3 Scale di valutazione

L'app registra e mostra **solo il punteggio numerico grezzo** (somma aritmetica degli item). **Nessun cutoff, fascia di rischio, categoria o interpretazione** e' presente nel codice o nella UI. Le descrizioni degli item sono **parafrasate** dalle fonti (nessuna riproduzione verbatim).

| Scala | Items | Range | Riferimento |
|-------|-------|-------|-------------|
| **Norton** | 5 (ciascuno 1–4) | 5–20 | Norton et al., 1962 |
| **NRS-2002 — screening finale** | stato nutrizionale 0–3 + gravita' malattia 0–3 + eta' ≥70 (+1) | 0–7 | ESPEN — Kondrup et al., 2003 (pre-screening a 4 domande **non** incluso) |
| **Pain NRS** | 1 (0–10, sole ancore pubblicate 0/10) + descrittori tipo/carattere (non interpretativi) | 0–10 | Numeric Rating Scale (uso clinico standard) |

Scale presenti in versioni precedenti e **rimosse**: Braden, BWAT, MNA, MUST (copyright/licenza), TIME/TIMERS (codice morto eliminato) e, dalla v1.1.0-docs, **Barthel** (copyright attivo MedChi/Mapi) e la **valutazione interna del rischio cadute** (non validata, non pertinente al wound care).

### 2.4 Navigazione e flusso utente

```
Consenso Privacy + EULA → Login (biometrico o credenziale dispositivo) → Lista Pazienti
                                                ├── Dettaglio Paziente
                                                │     ├── Nuova Lesione (body map)
                                                │     ├── Medicazione (foto da galleria, ROI facoltativa, tessuti manuali, essudato, medicazioni, data, note)
                                                │     └── Scale (Norton, NRS-2002, Pain NRS)
                                                └── Impostazioni
                                                      ├── Registro Attivita'
                                                      ├── Backup (creazione)
                                                      └── Ripristino
```

- Il database viene aperto **solo dopo il login**.
- Timeout sessione: **5 minuti** di inattivita' → ritorno automatico al login
- Timestamp attivita' aggiornato ad ogni cambio schermata
- Controllo timeout su `ON_RESUME` (ritorno dall'app in background)

### 2.5 Nessun riferimento/guida sulle medicazioni

L'app **non** contiene alcuna tabella o guida che associ lo stato della ferita a tipi o classi di medicazione. Una sezione bibliografica "Riferimento medicazioni" presente in versioni precedenti e' stata **rimossa**: nessun elemento, neppure generale, puo' essere percepito come supporto alla scelta terapeutica.

---

## 3. Sicurezza dei Dati

### 3.1 Crittografia del database

| Parametro | Valore |
|-----------|--------|
| Libreria | SQLCipher 4.5.4 |
| Algoritmo | AES-256-CBC (default SQLCipher) |
| Lunghezza chiave | 256 bit (32 byte), generata con `java.security.SecureRandom` |
| Storage chiave | EncryptedSharedPreferences |
| Protezione chiave | Android Keystore (MasterKey AES256_GCM) |
| Cifratura nomi preferenze | AES256_SIV |
| Cifratura valori preferenze | AES256_GCM |
| Apertura DB | **Solo dopo il login** (autenticazione riuscita) |
| Legacy DB | Migrazione di **rekey**: i database creati da versioni affette dal bug della chiave (vedi sotto) vengono ri-cifrati con la chiave reale |

**Nota di trasparenza:** in versioni precedenti un bug faceva si' che il database venisse cifrato con una **chiave nulla** (di zeri). Il bug e' stato corretto: la chiave casuale a 256 bit e' ora effettivamente applicata, e i database legacy vengono migrati (rekey) alla prima apertura.

**Flusso:**
1. Al primo avvio, `DatabaseKeyManager` genera 32 byte casuali con `SecureRandom`
2. La chiave viene salvata in `EncryptedSharedPreferences` protette dal Keystore
3. Dopo il login, la chiave viene letta, passata a `SupportFactory` e azzerata dalla memoria

### 3.2 Crittografia delle fotografie

Le fotografie **non** vengono scattate in-app: l'operatore le **seleziona dalla galleria** di sistema e l'app le **cifra all'importazione**, salvandole nello storage interno privato.

| Parametro | Valore |
|-----------|--------|
| Libreria | AndroidX Security Crypto (`EncryptedFile`) |
| Schema | AES256_GCM_HKDF_4KB |
| Protezione chiave | Android Keystore (MasterKey AES256_GCM) |
| Directory | `{filesDir}/wound_photos_enc/` (storage interno privato) |
| Naming | UUID casuale + `.jpg.enc` (nessun dato paziente nel nome) |
| Qualita' JPEG | 90 |

**Protezioni aggiuntive:**
- Path traversal bloccato: `loadBitmap()` rifiuta percorsi con `..` o `/` e valida percorsi assoluti con `canonicalPath.startsWith(encDir)`
- Eliminazione paziente: tutte le foto cifrate vengono cancellate dal filesystem prima della cancellazione dal database
- All'import da backup le foto vengono **rinominate con nuovo UUID**

### 3.3 Autenticazione

| Parametro | Valore |
|-----------|--------|
| Libreria | AndroidX Biometric 1.1.0 |
| Authenticators | `BIOMETRIC_STRONG \| DEVICE_CREDENTIAL` |
| Fallback | PIN/Pattern/Password del dispositivo |
| CryptoObject | Chiave Keystore **auth-bound**, esercitata al login |
| Rate limiting | Gestito nativamente dal framework Android |
| Session timeout | 5 minuti di inattivita' |
| Evento | Login e timeout loggati nell'audit (`LOGIN`, `SESSION_TIMEOUT`) |
| Apertura dati | Il database viene aperto **solo dopo** autenticazione riuscita |

Se il Keystore non e' disponibile (es. dopo reset biometrico), le preferenze vengono cancellate e l'utente e' forzato a ri-autenticarsi e ri-accettare il consenso.

*Miglioria pianificata:* binding crittografico **completo** della chiave del database alla biometria (con strategia di recupero al re-enrollment biometrico) — vedi `SECURITY.md`.

### 3.4 Protezione screenshot e screen recording

`FLAG_SECURE` e' impostato in `MainActivity.onCreate()` **nelle build di release** (gate sul tipo di build; nelle build di debug resta disattivo per QA/screenshot). In release impedisce:
- Screenshot (tasto di cattura schermo)
- Screen recording
- Visualizzazione nell'app switcher (schermo nero)
- Cattura remota via ADB `screencap`

### 3.5 Protezione contro backup non autorizzati

| Misura | Dettaglio |
|--------|-----------|
| `android:allowBackup="false"` | Disabilita backup Android nativi |
| `data_extraction_rules.xml` | Esclude TUTTI i domini (file, database, sharedpref, external) sia da cloud backup sia da device transfer |
| Nessun permesso INTERNET | Nessuna trasmissione di rete dall'app |
| FileProvider limitato | Solo `cache-path/backups/` esposto, `exported="false"` |

### 3.6 Backup manuale cifrato

Il backup e' l'unico modo per trasferire dati tra dispositivi (completo o **per singolo paziente**). E' progettato per mantenere la sicurezza dei dati anche fuori dal dispositivo. L'**import e' non distruttivo** (merge): i pazienti vengono aggiunti con rimappamento degli ID, mai sovrascritti.

#### Formato file `.woundbackup`

```
[4 byte] Magic: "WBAK"
[4 byte] Versione: 2 (v1 ancora leggibile in import)
[16 byte] Salt PBKDF2 (SecureRandom)
[12 byte] IV AES-GCM (SecureRandom)
[N byte] Dati cifrati (JSON compresso GZIP)
```

#### Parametri crittografici

| Parametro | Valore |
|-----------|--------|
| Key derivation | PBKDF2WithHmacSHA256 |
| Iterazioni | **600.000** (formato v2, OWASP 2024); v1 a 210.000 letta in retrocompatibilita' |
| Salt | 16 byte, `SecureRandom` |
| Algoritmo cifratura | AES/GCM/NoPadding |
| Lunghezza chiave | 256 bit |
| IV | 12 byte, `SecureRandom` |
| Tag di autenticazione | 128 bit |
| Compressione | GZIP (prima della cifratura) |
| Password minima | **12 caratteri** |

#### Protezioni

| Protezione | Dettaglio |
|-----------|-----------|
| Import non distruttivo | Merge con rimappamento ID; foto rinominate con nuovo UUID |
| Anti-OOM | Limite file cifrato **600 MB**; limite decompresso **500 MB** (anti GZIP-bomb) |
| Path traversal foto | Filenames con `..` o `/` rifiutati |
| Memory safety | Password convertita in `CharArray` e azzerata; key bytes azzerati dopo uso; `PBEKeySpec.clearPassword()` |
| Password non in stato | Il ViewModel non memorizza la password nello StateFlow |
| Responsabilita' operatore | Warning esplicito: l'operatore e' responsabile della custodia del file esportato |
| Condivisione | Via `FileProvider` con `FLAG_GRANT_READ_URI_PERMISSION` (permesso temporaneo) |

*Miglioria pianificata:* AAD (Additional Authenticated Data) sull'header del backup — vedi `SECURITY.md`.

### 3.7 Audit trail

L'app mantiene un **registro locale delle principali operazioni** su pazienti e sessione, nella tabella `audit_log`:

| Azione | Descrizione |
|--------|-------------|
| `LOGIN` | Accesso autenticato |
| `SESSION_TIMEOUT` | Sessione scaduta per inattivita' |
| `CREATE_PATIENT` | Nuovo paziente creato |
| `UPDATE_PATIENT` | Dati paziente modificati |
| `DELETE_PATIENT` | Paziente eliminato (incluse foto) |
| `CREATE_ASSESSMENT` | Nuova lesione registrata |
| `CREATE_DRESSING` | Nuova medicazione registrata |
| `CREATE_NORTON` | Scala Norton compilata |
| `CREATE_NRS2002` | NRS-2002 (screening finale) compilato |
| `CREATE_PAIN_NRS` | Pain NRS compilata |
| `BACKUP_CREATED` | Backup completo generato |
| `BACKUP_RESTORED` | Backup importato |

Ogni record contiene: ID, timestamp, azione, tipo entita', ID entita', dettagli testuali.

**Nota di trasparenza:** l'audit copre le principali operazioni, comprese le esportazioni (`EXPORT_PDF`, `EXPORT_PATIENT_BACKUP`) e, dalla v1.1, le correzioni documentali (`DELETE_WOUND`, `DELETE_DRESSING`, `UPDATE_DRESSING`, `UPDATE_WOUND`). Non si dichiara comunque un "audit di tutte le operazioni".

L'audit log e' consultabile da Impostazioni > Registro Attivita'.

### 3.8 Protezione in release build

| Misura | Dettaglio |
|--------|-----------|
| ProGuard/R8 | `isMinifyEnabled = true`, `isShrinkResources = true` |
| Classi protette | Room entity/DAO, SQLCipher, Security Crypto, Biometric, Compose |
| Log produzione | `Log.e()` protetti da `BuildConfig.DEBUG` (non eseguiti in release) |
| Firma release | `signingConfig` da `keystore.properties` (gitignorato, mai committato) |
| FLAG_SECURE | Attivo in release (vedi §3.4) |
| Root detection | Avviso all'utente su dispositivo rooted |

---

## 4. Conformita' Normativa

### 4.1 GDPR (Regolamento UE 2016/679)

#### Modello dei ruoli

- **Titolare del trattamento:** il professionista sanitario o la struttura che utilizza l'app.
- **Sviluppatore:** mero fornitore del software; **non** e' responsabile del trattamento ex art. 28 GDPR (non effettua alcun trattamento, non ha accesso ai dati, l'app e' offline).
- Il **consenso del paziente** alla raccolta di dati e fotografie spetta al titolare, secondo le procedure della propria struttura.

#### Informativa privacy in-app (profilo EU, art. 13)

L'informativa viene mostrata al primo avvio e deve essere accettata prima dell'uso. Copre:

1. **Titolare del trattamento** — Il professionista sanitario o la struttura
2. **Dati trattati** — Anagrafici, valutazioni cliniche, fotografie, note (Art. 9 dati sanitari)
3. **Finalita'** — Documentazione clinica e continuita' assistenziale
4. **Base giuridica** — Art. 9(2)(h) GDPR (assistenza sanitaria) + Art. 9(3) (segreto professionale)
5. **Archiviazione e sicurezza** — Cifratura AES-256, autenticazione, no rete, audit log
6. **Diritti dell'interessato** — Artt. 15–22 GDPR
7. **Conservazione** — Riferimento alla conservazione della documentazione sanitaria per **almeno 10 anni** (D.Lgs. 196/2003 e normativa nazionale applicabile)
8. **Reclamo** — Diritto di reclamo al Garante per la protezione dei dati personali (art. 77 GDPR)
9. **Decisioni automatizzate** — Statement esplicito: **nessuna decisione automatizzata ne' profilazione** (art. 22 GDPR)

#### Profili giurisdizione e consenso

- **3 profili di giurisdizione** (EU / US / ROW) con EULA e flusso di consenso ramificati.
- Consenso **versionato** (attualmente v2), registrato con timestamp; il **cambio di giurisdizione** richiede un nuovo consenso.
- Checkbox esplicita con dichiarazione di essere professionista sanitario.
- *Miglioria pianificata:* la **revoca** del consenso/presa-visione **dall'interno dell'app** non e' ancora implementata.

#### Diritti dell'interessato (supporto al titolare)

- **Accesso/copia** (art. 15): report di documentazione PDF del decorso per paziente.
- **Portabilita'** (art. 20): backup del singolo paziente (JSON cifrato).
- **Rettifica** (art. 16): modifica dei dati registrati.
- **Cancellazione** (art. 17): cancellazione paziente → CASCADE su tutte le tabelle figlie; foto cifrate cancellate fisicamente dal filesystem; azione registrata nell'audit log; irreversibile.

#### NIS2 e CRA

- **NIS2** (Dir. UE 2022/2555): non direttamente applicabile allo sviluppatore (micro-impresa, prodotto offline); puo' rilevare come elemento di **supply-chain** per i clienti sanitari soggetti a NIS2.
- **CRA** (Reg. UE 2024/2847 — Cyber Resilience Act): si applichera' **direttamente anche alle micro-imprese** (piena applicazione dicembre 2027). Adempimenti futuri pianificati: SBOM, gestione delle vulnerabilita', aggiornamenti di sicurezza, marcatura CE. Vedi `SECURITY.md`.

### 4.2 EULA e Disclaimer

#### EULA (clausole principali)

1. Applicazione fornita "così com'è" (as-is), senza garanzie
2. Strumento di documentazione clinica, non fornisce diagnosi o supporto decisionale
3. Nei limiti di legge, sviluppatore non responsabile per danni da uso dell'applicazione
4. Uso riservato a professionisti sanitari con segreto professionale
5. Operatore responsabile di custodia dispositivo, dati e backup

#### Misure implementate

- Banner permanente: "Strumento di documentazione clinica"
- Scale: solo punteggio numerico grezzo, nessuna etichetta di rischio, nessuna azione suggerita
- Composizione tissutale inserita manualmente dall'operatore (4 slider, clamp ≤100%)
- Nessun algoritmo decisionale clinico nel codice
- Nessun alert, avviso o guardrail clinico
- Nessuna guida/tabella sulle medicazioni (rimossa): nessun supporto alla scelta terapeutica (§2.5)

### 4.3 Revisioni di sicurezza

- **Maggio 2026:** audit a 3 livelli (sicurezza, legale, bug bounty); finding CRITICAL e HIGH risolti.
- **Giugno 2026:** corretto il bug della chiave SQLCipher nulla (cifratura DB ora effettiva, con migrazione rekey); database aperto solo post-login; FLAG_SECURE in release; firma release configurata; rimozione scale protette e residui di analisi immagine; schema DB v10; password backup minima 12 caratteri.
- **Luglio 2026 (questa revisione, v1.1.0-docs):** rimosse le scale Barthel e valutazione rischio cadute (schema DB v11); dolore procedurale opzionale e campi medicazione strutturati; note/anamnesi cifrate; bozza anti-timeout su EncryptedSharedPreferences; audit esteso a eliminazioni e correzioni documentali; report di documentazione PDF con andamento temporale; conferma password backup.

Punti aperti dichiarati (vedi `SECURITY.md`): audit dell'export, AAD sull'header del backup, binding completo chiave DB↔biometria, revoca consenso in-app, SBOM CycloneDX.

---

## 5. Localizzazioni e Tipi di Lesione

### 5.1 Localizzazioni anatomiche (32 siti)

Sacro, tallone sx/dx, trocantere sx/dx, ischio sx/dx, malleolo sx/dx, gomito sx/dx, scapola sx/dx, occipite, orecchio sx/dx, rachide/spinose vertebrali, ginocchio sx/dx, coscia sx/dx, gluteo sx/dx, gamba sx/dx, piede sx/dx, mano sx/dx, torace, addome, altro.

### 5.2 Tipi di lesione (12 tipi)

Ulcera da pressione, ulcera venosa, ulcera arteriosa, ulcera diabetica, ferita chirurgica, ferita traumatica, ustione, lesione neoplastica/maligna, skin tear, dermatite associata a umidita' (MASD/IAD), mista, altro.

Nota: l'app **non** effettua stadiazione delle lesioni (es. NPUAP/EPUAP).

### 5.3 Origini

Comunita', ospedale, RSA, domicilio, sconosciuta.

---

## 6. Composizione Tissutale ed Essudato

La composizione tissutale e' inserita manualmente dall'operatore tramite quattro slider: necrotico (nero), fibrina/slough (giallo), granulazione (rosso), epitelizzazione (rosa), con vincolo di totale ≤100%. L'essudato e' registrato su quattro livelli (assente/scarso/moderato/abbondante). Non viene eseguita alcuna analisi automatica delle immagini. L'applicazione non contiene intelligenza artificiale, machine learning o classificatori automatici; le entita' e i casi d'uso residui dell'analisi immagine (presenti in versioni precedenti) sono stati rimossi dal codice e dal modello dati.

---

## 7. Distribuzione

| Parametro | Valore |
|-----------|--------|
| Canale | Closed beta via APK (Telegram) |
| Play Store | Non pubblicata |
| Application ID | `com.daveide.wounddocs` |
| Signing | Release firmata da `keystore.properties` (gitignorato); il file di produzione va custodito fuori dal repo |
| Target | Professionisti sanitari 18+ |

---

## 8. Contatti

Per segnalazioni di sicurezza o richieste DPO: dev.davide@pm.me (vedi anche `SECURITY.md` per la policy di vulnerability disclosure).

---

*Documento aggiornato a giugno 2026. Versione app: v1.0 (build 32), branch di revisione di conformita'.*
