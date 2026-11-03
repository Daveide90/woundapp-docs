# Allegato 2 (A) — Scale di valutazione e fonti

L'applicazione include **tre** strumenti di valutazione. L'app registra e mostra **solo il
punteggio numerico grezzo** (somma aritmetica degli item selezionati dall'operatore, o il
valore selezionato per la scala numerica del dolore); **non** assegna categorie di rischio,
fasce, soglie o interpretazioni. Le descrizioni degli item sono **parafrasate** dalle fonti,
**senza riproduzione verbatim** delle pubblicazioni.

| Scala | Cosa registra l'operatore | Range | Fonte |
|---|---|---|---|
| Norton | 5 item (rischio lesioni da pressione), ciascuno 1–4 | 5–20 | Norton D., McLaren R., Exton-Smith A.N., 1962 |
| NRS-2002 — sola componente "final screening" | stato nutrizionale (0–3) + gravità malattia (0–3) + età ≥70 (+1) | 0–7 | ESPEN — Kondrup et al., 2003 |
| NRS dolore | intensità del dolore riferito, scala numerica 0–10 | 0–10 | Numeric Rating Scale (scala numerica di pubblico dominio) |

Note:
- Il totale è la **somma aritmetica** degli item selezionati dall'operatore, definita dalla
  scala stessa; nessun algoritmo, peso o logica interpretativa.
- Le descrizioni degli item sono **parafrasate dalle fonti**, senza riproduzione verbatim
  delle pubblicazioni originali. L'app è in **4 lingue (IT/EN/FR/ES)**; le descrizioni
  cliniche degli item delle scale sono fornite in IT/EN e per FR/ES ripiegano sull'inglese,
  per scelta dichiarata.
- Del **NRS-2002** è inclusa la **sola componente "final screening"**; il pre-screening
  iniziale a 4 domande **non** è incluso, e ciò è dichiarato (la schermata è etichettata
  "final screening").
- La **NRS dolore** riporta esclusivamente le due **ancore pubblicate** (0 = "nessun
  dolore", 10 = "peggior dolore possibile"); **nessun descrittore intermedio** per i valori
  1–9.
- L'app **non** mostra cut-off, categorie o esiti interpretativi del punteggio del paziente.
- La cronologia dei punteggi mostra un grafico neutro (senza delta, frecce o indicatori di
  tendenza) e, per ciascuna scala, una legenda che descrive intervallo e direzione citando
  la fonte pubblicata (es. "Secondo Norton et al. 1962: punteggio più basso = condizione
  più compromessa"): descrive lo strumento, non interpreta il risultato del paziente.

## Scale rimosse (non fanno parte del prodotto sottoposto a valutazione)

- **Rimosse in v1.1**: l'**indice di Barthel** (copyright attivo: titolarità MedChi —
  Maryland State Medical Society, licensing tramite Mapi Research Trust/ePROVIDE, licenza
  richiesta anche per uso commerciale; rimossa in via prudenziale) e la **"Valutazione
  rischio cadute"** (strumento interno non validato e non pertinente al wound care).
- **Rimosse in versioni precedenti**: Braden, BWAT, MNA, MUST (questioni di
  copyright/licenza) e i framework TIME/TIMERS (codice non più attivo, eliminato).
- Nota tecnica: nei backup/database di installazioni precedenti possono esistere
  identificatori legacy delle scale rimosse (es. `barthel`, `conley`); all'import tali
  record vengono **ignorati** e non compaiono nell'app.
