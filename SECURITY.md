# Security Policy — WoundApp Docs

**Versione app:** v1.1.0-docs (versionCode 33) · `com.daveide.wounddocs` · luglio 2026

> **EN summary** — Coordinated vulnerability disclosure for the WoundApp Docs repository and
> APK. Report security issues to **dev.davide@pm.me**; expect an acknowledgement and initial
> assessment within ~30 days. The app is 100% offline (no INTERNET permission). Dependencies
> are pinned in `app/build.gradle.kts`. A CycloneDX SBOM is planned. The EU Cyber Resilience
> Act (Reg. (EU) 2024/2847, fully applicable December 2027) is tracked as an upcoming
> obligation. Exports (PDF and single-patient backup) are tracked in the local audit log.
> Planned hardening: full crypto-binding of the DB key to biometrics (with a re-enrollment
> recovery strategy) and AAD on the backup header.

---

## 1. Segnalazione di vulnerabilità (vulnerability disclosure)

- **Contatto:** dev.davide@pm.me (indicare "SECURITY" nell'oggetto).
- **Finestra di risposta:** presa in carico e prima valutazione entro **~30 giorni** dalla
  segnalazione. Trattandosi di un progetto a sviluppatore singolo, tempi di correzione e
  rilascio dipendono dalla gravità; le vulnerabilità che compromettono la cifratura dei dati
  paziente hanno priorità massima.
- **Cosa includere:** versione app/branch, passi di riproduzione, impatto stimato, eventuale
  PoC. È gradita la divulgazione coordinata (nessuna pubblicazione prima del fix o della
  scadenza concordata).

## 2. Ambito (scope)

- L'**APK** dell'applicazione e il **codice di questo repository**.
- L'app è **100% offline**: nessun permesso INTERNET, nessun server, nessuna API remota.
  Non esistono quindi superfici di rete lato sviluppatore; le classi di rischio rilevanti
  sono: cifratura locale (DB SQLCipher, foto, backup), autenticazione, gestione chiavi,
  parsing dei file di backup importati, permessi/IPC Android.
- Fuori ambito: vulnerabilità del sistema operativo o del dispositivo dell'utente,
  social engineering, accesso fisico a dispositivo sbloccato.

## 3. Dipendenze

- Le dipendenze sono **pinnate a versioni esatte** in [`app/build.gradle.kts`](app/build.gradle.kts)
  (nessun range dinamico). Stack principale: Kotlin, Jetpack Compose, Room + SQLCipher,
  AndroidX Security Crypto, AndroidX Biometric.
- Una **SBOM in formato CycloneDX è pianificata** (non ancora pubblicata): sarà generata a
  partire dal grafo Gradle e allegata alle release.

## 4. Cyber Resilience Act (CRA)

Il **Reg. (UE) 2024/2847 (CRA)** si applicherà **direttamente anche alle micro-imprese**,
con piena applicazione a **dicembre 2027**. Adempimenti futuri tracciati per questo
prodotto: SBOM, processo documentato di gestione delle vulnerabilità (questa policy ne è la
base), aggiornamenti di sicurezza per il periodo di supporto, marcatura CE ai sensi del CRA.
(La NIS2 non è direttamente applicabile allo sviluppatore; può rilevare come requisito di
supply-chain dei clienti sanitari.)

## 5. Misure attuali (sintesi)

- DB Room + SQLCipher AES-256; chiave 256-bit casuale in EncryptedSharedPreferences
  (MasterKey Keystore AES256_GCM), applicata correttamente (con migrazione rekey dei DB
  legacy); DB aperto **solo dopo il login**.
- Foto cifrate (EncryptedFile AES-256-GCM, nomi UUID); backup cifrato
  PBKDF2-HMAC-SHA256 600.000 iterazioni + AES-256-GCM, password ≥12 caratteri, limiti
  anti-OOM (600 MB file / 500 MB decompresso).
- Login biometrico (BIOMETRIC_STRONG) o credenziale dispositivo, CryptoObject su chiave
  Keystore auth-bound; FLAG_SECURE in release; allowBackup=false; timeout sessione 5 min;
  root detection; R8 in release; firma release da `keystore.properties` (gitignorato).
- Audit log locale delle principali operazioni su pazienti e sessione, **incluse le
  esportazioni** (`EXPORT_PDF`, `EXPORT_PATIENT_BACKUP`) e le operazioni documentali su
  lesioni/medicazioni (`DELETE_WOUND`, `DELETE_DRESSING`, `UPDATE_DRESSING`).
- **Bozza anti-timeout** dei form in corso salvata cifrata in **EncryptedSharedPreferences**
  (stessa protezione delle preferenze cifrate), con ripresa al rientro.

Dettagli completi in [`docs/DOCUMENTAZIONE_TECNICA_SICUREZZA.md`](docs/DOCUMENTAZIONE_TECNICA_SICUREZZA.md).

## 6. Hardening pianificato

Dichiarato esplicitamente come **non ancora implementato**:

1. **Binding crittografico completo della chiave DB alla biometria** — oggi la chiave DB è
   custodita in EncryptedSharedPreferences e l'autenticazione esercita un CryptoObject
   auth-bound; il passo successivo è derivare/avvolgere la chiave DB direttamente con una
   chiave Keystore richiedente autenticazione, con una **strategia di recupero documentata
   per il re-enrollment biometrico** (evitare lock-out dei dati clinici).
2. **AAD sull'header del backup** — autenticare magic/versione/salt/IV come Additional
   Authenticated Data del GCM, contro manomissioni dell'header.

(L'audit dell'export, in precedenza elencato qui come mancante, è stato **implementato**:
l'export del report di documentazione PDF e del backup del singolo paziente sono tracciati
nel registro attività con le azioni `EXPORT_PDF` ed `EXPORT_PATIENT_BACKUP`.)

## 7. Ringraziamenti

Le segnalazioni responsabili saranno accreditate (se gradito al segnalante) nelle note di
rilascio.
