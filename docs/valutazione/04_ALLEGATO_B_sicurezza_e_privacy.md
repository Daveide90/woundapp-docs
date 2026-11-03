# Allegato 3 (B) — Trattamento dei dati e misure di sicurezza

Allegato a supporto: descrive che il trattamento è **locale** e protetto. (Rilevante per la
protezione dei dati; secondario ai fini della qualificazione, che dipende dalla destinazione
d'uso e dalle funzioni — Allegato 1.)

## Natura locale del trattamento
- L'app funziona **interamente sul dispositivo**: nessun permesso di rete (`INTERNET`
  assente), nessun server/cloud, nessun invio di dati o telemetria.
- Lo sviluppatore **non riceve né tratta** i dati. **Titolare del trattamento** è
  l'operatore/struttura sanitaria che utilizza l'app (ai sensi del GDPR).

## Dati trattati
- Anagrafici del paziente; dati sanitari (immagini delle lesioni, misure, percentuali di
  tessuto, punteggi delle scale, note) — categorie particolari ex art. 9 GDPR.

## Misure di sicurezza (estratto)
- Database cifrato (SQLCipher, AES-256); chiave casuale a 256 bit custodita in
  EncryptedSharedPreferences e protetta dall'Android Keystore; database aperto **solo dopo
  il login**.
- Fotografie cifrate (AES-256-GCM) su storage interno privato (importate dalla galleria e
  cifrate all'import; nessuna fotocamera in-app).
- Backup cifrato (AES-256-GCM, derivazione chiave PBKDF2-HMAC-SHA256 600.000 iterazioni,
  password minima 12 caratteri).
- Accesso con autenticazione biometrica o credenziale del dispositivo; timeout di sessione;
  blocco screenshot in release (FLAG_SECURE).
- `allowBackup=false`; offuscamento del codice in release; log di audit interno delle
  principali operazioni su pazienti e sessione, incluse le esportazioni (report PDF e
  backup del singolo paziente).

## Diritti dell'interessato (a supporto del titolare)
- Informativa in-app (art. 13), con indicazione della conservazione (≥10 anni, D.Lgs.
  196/2003), del diritto di reclamo al Garante (art. 77) e dell'assenza di decisioni
  automatizzate (art. 22); accesso/portabilità tramite export PDF del decorso e backup del
  singolo paziente; rettifica; cancellazione del paziente con tutti i dati e foto.
- (Miglioria pianificata, non ancora implementata: revoca del consenso in-app, art. 7(3)
  GDPR.)

> Questo allegato documenta la protezione dei dati. La questione sottoposta a parere riguarda
> la **qualificazione** del prodotto (Allegato 1), non la conformità privacy.
