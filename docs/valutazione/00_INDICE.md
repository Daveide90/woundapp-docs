# Richiesta di parere di qualificazione — WoundApp Docs

Pacchetto documentale da inviare al **Ministero della Salute — Direzione generale dei
dispositivi medici e del farmaco (DGDMF)** per chiedere un **parere sulla qualificazione**
del software "WoundApp Docs" ai sensi del Regolamento (UE) 2017/745 (MDR): cioè se il
prodotto rientri o meno nella definizione di dispositivo medico (caso borderline).

Versione app: v1.1.0-docs (versionCode 33, `com.daveide.wounddocs`) · Aggiornamento: luglio 2026

## Indice completo del pacchetto

| # | File | Contenuto | Da inviare? |
|---|---|---|---|
| 00 | [`00_INDICE.md`](00_INDICE.md) | Questo indice | No (uso interno) |
| 01 | [`01_ISTANZA_richiesta_parere_qualificazione.md`](01_ISTANZA_richiesta_parere_qualificazione.md) | Lettera/istanza formale da firmare e inviare via PEC | **Sì** |
| 02 | [`02_RELAZIONE_TECNICA.md`](02_RELAZIONE_TECNICA.md) | Allegato 1 — descrizione del prodotto, **destinazione d'uso**, funzionalità, limiti volontari (incl. legenda grafici; nessuna guida sulle medicazioni), **motivazione della qualificazione proposta** (non-dispositivo-medico) | **Sì** |
| 03 | [`03_ALLEGATO_A_scale_e_fonti.md`](03_ALLEGATO_A_scale_e_fonti.md) | Allegato 2 (A) — le 3 scale incluse (Norton; NRS-2002, sola componente "final screening"; NRS dolore) e fonti; nota sulle scale rimosse | **Sì** |
| 04 | [`04_ALLEGATO_B_sicurezza_e_privacy.md`](04_ALLEGATO_B_sicurezza_e_privacy.md) | Allegato 3 (B) — trattamento dei dati (locale) e misure di sicurezza | **Sì** |
| 05 | [`05_GUIDA_INVIO_e_checklist.md`](05_GUIDA_INVIO_e_checklist.md) | A chi e come inviare, cosa allegare, dati da compilare prima dell'invio | No (guida interna) |
| 06 | [`06_licenze_scale.md`](06_licenze_scale.md) | Scheda licenze/copyright delle scale (scale protette rimosse; set libero attuale) | No (uso interno) |
| 07 | [`07_STATO_APP.md`](07_STATO_APP.md) | Snapshot dello stato implementato dell'app (per verificare la coerenza codice↔documenti) | Facoltativo (utile al valutatore) |
| — | [`genera_documenti.py`](genera_documenti.py) | Script di impaginazione DOCX/PDF dei documenti da inviare | No (strumento) |
| — | ＜Allegato 4 — manuale d'uso / screenshot＞ | Da preparare a parte (consigliato) | Consigliato |

## Versioni impaginate (DOCX/PDF)

I file `.md` qui sono il **sorgente**. Le versioni **impaginate e pronte da consegnare**
(DOCX editabili con campi evidenziati + PDF finali) si rigenerano con
[`genera_documenti.py`](genera_documenti.py) (richiede `python-docx` e LibreOffice per il PDF):

```bash
python3 genera_documenti.py && soffice --headless --convert-to pdf --outdir /tmp/ministero_docx /tmp/ministero_docx/*.docx
```

Copie pronte anche nella cartella di consegna (`WoundAppDocs_consegna/documenti_ministero/`).

## Prima di inviare

- Compilare i campi tra parentesi angolari «…» (dati del richiedente/fabbricante, data, firma).
- **Verificare l'indirizzo PEC esatto del DGDMF** (vedi guida): le pagine ufficiali vanno
  consultate direttamente, il valore va confermato su IndicePA / salute.gov.it.
- Allegare eventuale manuale d'uso / screenshot dell'app (non inclusi qui).

## Avvertenza

Bozza tecnica predisposta dallo sviluppatore. Non costituisce consulenza legale. Per una
pratica formale è consigliabile la revisione di un consulente regolatorio/legale specializzato
in dispositivi medici.
