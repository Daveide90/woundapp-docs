# Privacy Policy — WoundApp Docs

**Versione 3 / Version 3**  
**Ultimo aggiornamento / Last updated: novembre 2026 / November 2026**

---

## IT

### 1. Introduzione

WoundApp Docs ("l'App") e' un'applicazione mobile di documentazione clinica per la registrazione delle lesioni cutanee, destinata all'uso da parte di professionisti sanitari.

L'App e' uno strumento di documentazione clinica. Non fornisce diagnosi, raccomandazioni terapeutiche personalizzate o supporto decisionale clinico.

### 2. Titolarita del trattamento

Lo sviluppatore dell'App non riceve dati, non ha accesso ai dati e non opera come responsabile del trattamento ex art. 28 GDPR: e' mero fornitore del software. L'App e' completamente offline: non contiene il permesso INTERNET, non contatta server, non invia analytics, non usa cookie o tracker.

Il titolare del trattamento dei dati personali e' il professionista sanitario o la struttura sanitaria che utilizza l'App, ai sensi del Regolamento UE 2016/679 (GDPR).

Il professionista dipendente di una struttura sanitaria tratta dati di cui e' titolare la struttura: l'uso dell'App su un dispositivo personale richiede l'autorizzazione della struttura e il rispetto delle sue procedure.

### 3. Dati trattati

L'App tratta le seguenti categorie di dati, inseriti dal professionista sanitario:

- Dati anagrafici del paziente (nome, cognome, reparto, letto)
- Valutazioni cliniche delle lesioni cutanee (misure, percentuali di tessuto, essudato e tipo di essudato)
- Scale di valutazione: Norton, NRS-2002 (sola componente "final screening"), NRS Dolore
- Fotografie delle lesioni cutanee (selezionate dalla galleria e cifrate all'importazione)
- Note e anamnesi della lesione, note del paziente
- Storia delle medicazioni, inclusi data della medicazione, dolore procedurale (se rilevato) e medicazione primaria/secondaria applicata

Questi dati includono dati sanitari di categoria speciale ai sensi dell'Art. 9 GDPR.

**Dati NON raccolti dall'App:**
- Identificazione del dispositivo
- Geolocalizzazione
- Dati di utilizzo o analytics
- Cookie o tecnologie di tracciamento
- Pubblicita

### 4. Finalita del trattamento

I dati sono trattati esclusivamente per documentazione clinica delle lesioni cutanee e per organizzarne cronologicamente l'evoluzione documentata.

### 5. Base giuridica

Il trattamento dei dati sanitari si basa su:
- Art. 9(2)(h) GDPR: finalita di medicina preventiva, diagnosi, assistenza o terapia sanitaria
- Art. 9(3) GDPR: trattamento da parte di professionisti sanitari soggetti al segreto professionale

Il consenso del paziente alla raccolta dei dati e delle fotografie deve essere acquisito dal professionista sanitario (titolare) secondo le procedure della propria struttura.

### 6. Archiviazione e sicurezza

Tutti i dati sono archiviati esclusivamente sul dispositivo dell'utente.

Misure di sicurezza:
- Cifratura database: AES-256 tramite SQLCipher
- Cifratura fotografie: AES-256-GCM tramite Android EncryptedFile
- Protezione chiavi: Android Keystore
- Autenticazione biometrica o credenziale del dispositivo (PIN/pattern/password); database aperto solo dopo l'autenticazione
- Timeout sessione: 5 minuti di inattivita
- Nessuna comunicazione di rete
- Backup automatico su Google Drive disabilitato
- Screenshot e registrazioni dello schermo bloccati
- Audit log locale delle principali operazioni su pazienti e sessione

### 7. Trasferimento di dati

L'App non trasferisce **automaticamente** alcun dato a terze parti, server esterni o servizi cloud: non esiste alcun canale di rete.

Esiste un **export manuale cifrato**: l'operatore puo, su sua esplicita azione, esportare un report di documentazione PDF o un backup cifrato (completo o del singolo paziente, AES-256-GCM con password di almeno 12 caratteri) e condividerlo con gli strumenti del dispositivo. La custodia e la trasmissione dei file esportati sono sotto la responsabilita del titolare.

### 8. Conservazione

I dati restano sul dispositivo finche il professionista sanitario non li elimina. La documentazione sanitaria e' soggetta agli obblighi di conservazione previsti dalle norme nazionali e dalle regole della struttura. La definizione dei tempi di conservazione spetta al titolare.

### 9. Diritti dell'interessato

Ai sensi degli Articoli 15-22 del GDPR, il paziente ha diritto di accesso, rettifica, cancellazione, limitazione, portabilita e opposizione.

L'App supporta il titolare con: copia/accesso tramite report di documentazione PDF (art. 15), portabilita tramite backup del singolo paziente (art. 20), rettifica dei dati, cancellazione completa e irreversibile di tutti i dati di un paziente, fotografie cifrate incluse (art. 17).

Per l'esercizio dei propri diritti, il paziente deve rivolgersi al professionista sanitario o alla struttura che utilizza l'App.

### 10. Decisioni automatizzate

L'App **non effettua alcuna decisione automatizzata ne profilazione** ai sensi dell'art. 22 GDPR: nessun algoritmo interpreta i dati, nessun punteggio viene classificato o usato per produrre effetti sul paziente.

### 11. Reclamo all'autorita di controllo

Ai sensi dell'art. 77 GDPR, l'interessato ha diritto di proporre reclamo all'autorita di controllo: in Italia, il **Garante per la protezione dei dati personali** (www.garanteprivacy.it).

### 12. Contatti

Per segnalazioni tecniche: dev.davide@pm.me

---

## EN

### 1. Introduction

WoundApp Docs ("the App") is a mobile clinical documentation application for recording skin lesions, intended for use by healthcare professionals.

The App is a clinical documentation tool. It does not provide diagnoses, personalized therapeutic recommendations, or clinical decision support.

### 2. Data controllership

The developer does not receive data, has no access to data, and does not act as a data processor under Article 28 GDPR: the developer is a mere software supplier. The App is fully offline: it has no INTERNET permission, contacts no servers, sends no analytics, uses no cookies or trackers.

The data controller is the healthcare professional or facility using the App, under EU Regulation 2016/679 (GDPR).

A professional employed by a healthcare facility processes data whose controller is the facility: using the App on a personal device requires the facility's authorisation and compliance with its procedures.

### 3. Data processed

The App processes the following data categories, entered by the healthcare professional:

- Patient demographics (name, surname, ward, bed)
- Clinical wound assessments (measurements, tissue percentages, exudate and exudate type)
- Assessment scales: Norton, NRS-2002 (final screening component only), Pain NRS
- Wound photographs (selected from the gallery and encrypted on import)
- Wound notes/history and patient notes
- Dressing history, including dressing date, procedural pain (if recorded) and applied primary/secondary dressing

This data includes special category health data under Article 9 GDPR.

**Data NOT collected by the App:**
- Device identification
- Geolocation
- Usage data or analytics
- Cookies or tracking technologies
- Advertising

### 4. Purpose

Data is processed exclusively for clinical wound documentation and for organizing the documented course over time.

### 5. Legal basis

Processing of health data is based on:
- Article 9(2)(h) GDPR: purposes of preventive medicine, medical diagnosis, provision of health care
- Article 9(3) GDPR: processing by healthcare professionals bound by professional secrecy

Patient consent for data and photograph collection must be obtained by the healthcare professional (controller) according to their facility's procedures.

### 6. Storage and security

All data is stored exclusively on the user's device.

Security measures:
- Database encryption: AES-256 via SQLCipher
- Photo encryption: AES-256-GCM via Android EncryptedFile
- Key protection: Android Keystore
- Biometric authentication or device credential (PIN/pattern/password); database opened only after authentication
- Session timeout: 5 minutes of inactivity
- No network communication
- Automatic Google Drive backup disabled
- Screenshots and screen recordings blocked
- Local audit log of the main patient and session operations

### 7. Data transfers

The App does not **automatically** transfer any data to third parties, external servers, or cloud services: there is no network channel.

A **manual encrypted export** exists: the operator may, upon explicit action, export a PDF documentation report or an encrypted backup (full or single-patient, AES-256-GCM with a password of at least 12 characters) and share it via device tools. Custody and transmission of exported files are the controller's responsibility.

### 8. Retention

Data remains on the device until the healthcare professional deletes it. Medical documentation is subject to the retention obligations set by national law and by the organisation's rules. Retention periods are defined by the controller.

### 9. Data subject rights

Under Articles 15-22 GDPR, patients have the right to access, rectification, erasure, restriction, portability, and objection.

The App supports the controller with: access/copy via PDF documentation report (Art. 15), portability via single-patient backup (Art. 20), rectification, and complete, irreversible deletion of all patient data including encrypted photographs (Art. 17).

To exercise their rights, patients should contact the healthcare professional or facility using the App.

### 10. Automated decision-making

The App performs **no automated decision-making and no profiling** under Article 22 GDPR: no algorithm interprets the data, no score is classified or used to produce effects concerning the patient.

### 11. Complaint to the supervisory authority

Under Article 77 GDPR, data subjects have the right to lodge a complaint with a supervisory authority: in Italy, the **Garante per la protezione dei dati personali** (www.garanteprivacy.it).

### 12. Contact

For technical inquiries: dev.davide@pm.me
