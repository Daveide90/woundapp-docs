# Scheda licenze delle scale cliniche (uso commerciale)

**Documento INTERNO** (non fa parte dell'istanza al Ministero). Chiarisce lo stato di
**copyright/licenza** delle scale di valutazione, prima della vendita dell'app.

> Nota: questo documento NON riproduce il testo delle scale. Indica solo titolare, stato e
> dove ottenere la licenza/versione ufficiale. Verifica sempre con la fonte ufficiale.

## Stato attuale (v1.1.0-docs) — RISOLTO con set libero

Le scale protette da copyright — **Braden, BWAT, MNA®, MUST** — erano già state **RIMOSSE**
nelle revisioni precedenti, insieme ai framework **TIME/TIMERS** (codice morto eliminato).
In **v1.1** sono state rimosse anche **Barthel** (copyright attivo) e la **"Valutazione
rischio cadute"** (strumento interno non validato). Il set attuale è composto da **tre**
strumenti liberi:

| Scala (attuale) | Titolare / fonte | Stato licenza |
|---|---|---|
| **Norton** | Norton, McLaren & Exton-Smith, 1962 | **Adottata.** Pubblicata nel 1962; nessun licensing body attivo riscontrato (titolarità storica incerta, editore in liquidazione); uso dei soli item **parafrasati** (nessuna riproduzione verbatim) |
| **NRS-2002 (componente "final screening")** | ESPEN — Kondrup et al., 2003 | **Adottata.** Strumento pubblicato da società scientifica; nessun ente di licenza; descrizioni **parafrasate**; inclusa solo la componente "final screening" (dichiarato) |
| **NRS dolore** | Scala numerica generica 0–10 | **Adottata.** Scala numerica di pubblico dominio; incluse solo le **ancore** pubblicate (0 = "nessun dolore", 10 = "peggior dolore possibile"), nessun descrittore intermedio |

## Storico — scale rimosse (per riferimento)

| Scala rimossa | Titolare | Motivo rimozione |
|---|---|---|
| **Barthel** — rimossa in **v1.1** | MedChi (Maryland State Medical Society); licensing tramite Mapi Research Trust / ePROVIDE | **Copyright attivo**: licenza richiesta anche per uso commerciale; rimossa in via prudenziale |
| **Valutazione rischio cadute** (ex-Conley) — rimossa in **v1.1** | Strumento interno | **Non validato** e non pertinente al wound care |
| Braden | Health Sense Ai (ex Prevention Plus) | Licenza richiesta per uso a fini di profitto |
| BWAT (Bates-Jensen) | © 2001 Barbara Bates-Jensen | Permesso richiesto |
| MNA® | Nestlé Nutrition Institute (marchio MNA®) | Permesso/termini da confermare |
| MUST | BAPEN (Elia, 2003) | Uso commerciale da verificare |
| TIME / TIMERS | Schultz 2003 / Atkin 2019 | Codice morto; framework rimosso per igiene regolatoria |

## Scale da NON usare mai

| Scala | Titolare | Motivo |
|---|---|---|
| **Wong-Baker FACES®** | Wong-Baker FACES Foundation | **Non usata e da non usare**: marchio registrato con programma di licenza |

Nota tecnica: nei backup/database di installazioni precedenti possono esistere identificatori
legacy delle scale rimosse (es. `barthel`, `conley`); all'import tali record vengono
**ignorati**.

## Punti residui prima della vendita commerciale

- [ ] **FR/ES**: i contenuti clinici degli item per FR/ES restano in **inglese** (scelta
  dichiarata, nessuna traduzione propria). Per vendere in Francia/Spagna, verificare se
  esistono **traduzioni ufficiali** delle scale adottate (Norton, NRS-2002) e a quali
  condizioni siano utilizzabili; in mancanza, mantenere il ripiego in inglese.
- [ ] Conservare nel dossier la documentazione delle scelte fatte (parafrasi, rimozioni)
  come prova di diligenza.

## Fonti consultate (storico)

- Braden — bradenscale.com (FAQ/licenze)
- BWAT — RNAO Best Practice Guidelines (BWAT ©)
- MNA® — mna-elderly.com
- MUST — bapen.org.uk
- Barthel — Mapi Research Trust / ePROVIDE (eprovide.mapi-trust.org)
- Wong-Baker FACES® — wongbakerfaces.org (licensing)
