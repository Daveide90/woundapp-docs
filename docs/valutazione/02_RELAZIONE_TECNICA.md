# Allegato 1 — Relazione tecnica

Prodotto: **WoundApp Docs** · Versione: v1.1.0-docs (versionCode 33) · Fabbricante/sviluppatore: ＜…＞

## 1. Identificazione del prodotto
Applicazione software per sistema operativo Android (versione minima 8.0). Funziona
**interamente in locale** sul dispositivo, senza connessione di rete, senza componente
server/cloud. Non è un accessorio di alcun dispositivo medico né è incorporata o destinata a
comandare un dispositivo.

## 2. Destinazione d'uso (intended purpose)
WoundApp Docs è destinata all'uso da parte di **personale sanitario** per la
**documentazione clinica** delle lesioni cutanee. Consente di:
- registrare e archiviare dati anagrafici del paziente;
- archiviare fotografie delle lesioni **selezionate dalla galleria del dispositivo** (cifrate
  all'importazione), con cronologia (timeline);
- registrare misurazioni, percentuali di tessuto e annotazioni **inserite manualmente**
  dall'operatore;
- registrare i **punteggi di tre strumenti di valutazione** (vedi Allegato A: Norton;
  NRS-2002, sola componente "final screening"; NRS dolore), compilati dall'operatore;
- consultare, organizzare cronologicamente ed esportare (PDF/backup) i dati registrati.

La destinazione d'uso è **documentale**: registrare, conservare, mostrare e organizzare i
dati inseriti dall'operatore. L'applicazione **non** ha la finalità di fornire informazioni
ai fini di diagnosi, prevenzione, monitoraggio (inteso come elaborazione che produce
informazione clinica nuova), prognosi, previsione o trattamento.

## 3. Funzionalità
- Anagrafica/archivio pazienti (cifrato), gestione ferite per sede corporea (mappa corporea),
  note/anamnesi per lesione e note paziente (testo libero inserito dall'operatore).
- Medicazioni con foto (da galleria, cifrate all'import), selezione poligonale **facoltativa**
  dell'area della lesione (in alternativa: foto intera), percentuali tissutali a 4 tessuti
  (necrosi/slough/granulazione/epitelizzazione) **inserite manualmente** (con vincolo totale
  ≤100%), essudato (assente/scarso/moderato/abbondante) con tipo facoltativo (sieroso,
  siero-ematico, ematico, purulento), medicazione primaria/secondaria come campi strutturati
  (default "Non specificata"), dolore procedurale NRS facoltativo, data della medicazione
  impostabile (mai nel futuro) e note. Correzione documentale di data/note di un record
  esistente ed eliminazione di lesioni/medicazioni, entrambe registrate nel log di audit.
- **Misure** inserite manualmente e riferite alla presa in carico ("Misure iniziali"):
  lunghezza (asse maggiore), larghezza (perpendicolare alla lunghezza), profondità (limitata
  a 30 cm). L'area è dichiarata come **"Lunghezza × larghezza (stima)"** — prodotto dei due
  valori inseriti, non un'area planimetrica reale.
- Compilazione delle scale di valutazione (vedi Allegato A — Norton; NRS-2002, sola
  componente "final screening"; NRS dolore): l'operatore seleziona i singoli item; l'app ne
  riporta la **somma aritmetica** (il totale grezzo definito dalla scala stessa) e lo
  registra/mostra **come semplice numero**. Le descrizioni degli item sono **parafrasate**
  dalle fonti, senza riproduzione verbatim.
- Esportazione: **report di documentazione PDF** (il documento è intitolato "REPORT DI
  DOCUMENTAZIONE — LESIONI") con la cronologia documentata per lesione — tabella cronologica
  (data, percentuali di tessuto, essudato, dolore riferito, medicazione), grafico a barre
  impilate delle percentuali di tessuto per data (solo se sono presenti almeno 2 medicazioni;
  nessuna linea di tendenza né media), sequenza di fotografie numerate e datate. Contenuto
  puramente documentale: nessuna interpretazione, indice derivato o confronto valutativo.
  Backup cifrato (completo o per singolo paziente), import non distruttivo.

## 4. Cosa il prodotto NON fa (limiti volontari)
- **Nessuna diagnosi** e nessuna interpretazione clinica.
- **Nessuna analisi automatica delle immagini / nessuna AI.**
- **Nessuna interpretazione del risultato del singolo paziente**: nessuna categoria o fascia
  di rischio, nessuna soglia, nessuna classificazione dello stato del paziente. Viene mostrato
  il punteggio numerico grezzo.
- **Nessun avviso/alert** clinico basato su soglie.
- **Nessun indicatore di tendenza**: la cronologia dei punteggi non mostra delta, frecce o
  altri indicatori di andamento; il grafico è una rappresentazione neutra dei valori
  registrati.
- **Nessuna raccomandazione** terapeutica personalizzata o supporto alla decisione clinica.
- **Nessuna stadiazione** delle lesioni (es. NPUAP/EPUAP).
Tali esclusioni sono implementate a livello di codice (rimosse anche dal modello dati e dai
file di backup, non solo dall'interfaccia): in particolare, ogni residuo di analisi
automatica delle immagini è stato eliminato anche dal modello dati (nessuna entità di
risultato analisi, nessun caso d'uso di analisi); le percentuali tissutali sono inserite
esclusivamente a mano dall'operatore.

**Legenda di lettura dei grafici.** Nella cronologia dei punteggi l'app mostra un grafico
neutro dei valori registrati e, per ciascuna scala, una legenda che riporta l'intervallo e
la direzione della scala citando la fonte pubblicata (es. "Secondo Norton et al. 1962:
punteggio più basso = condizione più compromessa"). È la descrizione della scala (analoga
all'etichetta di un asse), non una classificazione né un'interpretazione del risultato del
singolo paziente, e non comporta soglie, avvisi, raccomandazioni o indicatori di tendenza.
Si sottopone anche questo elemento alla valutazione.

**Nessuna tabella di medicazioni / nessun supporto alla scelta terapeutica.** L'app **non**
contiene alcuna tabella o guida che associ lo stato della ferita a tipi o classi di
medicazione: una sezione bibliografica di riferimento sulle classi di medicazione presente
in versioni precedenti è stata **rimossa integralmente**. Il prodotto sottoposto a
valutazione non offre quindi alcun elemento, neppure generale o bibliografico, che possa
essere percepito come supporto alla scelta della medicazione. I campi "medicazione
primaria/secondaria" del flusso di registrazione sono **campi descrittivi** con cui
l'operatore documenta ciò che ha già applicato: non sono associati ad alcun suggerimento,
filtro o logica.

## 5. Motivazione della qualificazione proposta (non-dispositivo-medico)
Ai sensi dell'**art. 2, punto 1, MDR**, un software è dispositivo medico se ha una
**destinazione d'uso medica** (diagnosi, prevenzione, monitoraggio, previsione, prognosi,
trattamento o attenuazione di malattie) realizzata mediante un'azione sui dati che fornisce
informazioni a beneficio del singolo paziente (cfr. anche considerando 19 MDR).

Applicando i passaggi decisionali della guida **MDCG 2019-11**:
- il prodotto **è** un software ed è destinato all'ambito sanitario;
- il prodotto esegue **esclusivamente** azioni di *archiviazione, comunicazione,
  visualizzazione e semplice organizzazione/ricerca* dei dati inseriti dall'operatore;
- non esegue alcuna ulteriore **elaborazione** dei dati volta a creare o fornire
  un'informazione clinica nuova per il singolo paziente (non interpreta, non classifica, non
  calcola indici diversi dalla somma elementare definita dalla scala, non segnala soglie).

Secondo MDCG 2019-11, un software che si limita a tali azioni **non** costituisce *Medical
Device Software* e pertanto **non rientra** nella definizione di dispositivo medico.

### Punto borderline sottoposto a parere
Il richiedente è consapevole che la presenza di **scale di valutazione** (alcune delle quali
— come Norton o NRS-2002 — nell'uso clinico sono associate alla stima di un rischio) può
sollevare un dubbio qualificatorio. Si rappresenta che l'applicazione:
- **non** calcola né mostra alcuna categoria di rischio o interpretazione del punteggio;
- si limita a **registrare il valore numerico** che l'operatore ottiene compilando la scala,
  alla stregua di un supporto documentale (equivalente alla trascrizione su cartella);
- la somma degli item è l'operazione aritmetica elementare prevista dalla scala, priva di
  algoritmi, pesi o logiche interpretative.

Su questa base il richiedente propone la qualificazione come **non-dispositivo-medico** e ne
chiede conferma. (Nota: ove l'Autorità ritenesse che la sola somma debba essere considerata
elaborazione rilevante, il fabbricante è disponibile a far inserire all'operatore il totale
già calcolato, eliminando anche tale operazione.)

## 6. Misure dichiarative
L'applicazione riporta in modo esplicito (banner, EULA, informativa) la natura di strumento
di sola documentazione e la non qualificazione come dispositivo medico; il report di
documentazione PDF reca analogo disclaimer.
