#!/usr/bin/env python3
# Genera i documenti per il Ministero in DOCX impaginato (poi convertiti in PDF con LibreOffice).
import re, os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_COLOR_INDEX
from docx.enum.table import WD_TABLE_ALIGNMENT

OUT = "/tmp/ministero_docx"
os.makedirs(OUT, exist_ok=True)
ACCENT = RGBColor(0x1F, 0x38, 0x64)
TOKEN = re.compile(r'(\{\{.*?\}\}|\*\*.*?\*\*)')

def new_doc():
    d = Document()
    n = d.styles['Normal']; n.font.name = 'Calibri'; n.font.size = Pt(11)
    for s in d.sections:
        s.top_margin = s.bottom_margin = Cm(2.4); s.left_margin = s.right_margin = Cm(2.4)
    return d

def runs(p, text, size=11, bold=False):
    for part in TOKEN.split(text):
        if not part: continue
        if part.startswith('{{'):
            r = p.add_run(part[2:-2]); r.font.highlight_color = WD_COLOR_INDEX.YELLOW; r.bold = bold
        elif part.startswith('**'):
            r = p.add_run(part[2:-2]); r.bold = True
        else:
            r = p.add_run(part); r.bold = bold
        r.font.size = Pt(size)

def para(d, text, justify=True, size=11, bold=False, after=6, before=0, align=None):
    p = d.add_paragraph(); pf = p.paragraph_format
    pf.alignment = align if align is not None else (WD_ALIGN_PARAGRAPH.JUSTIFY if justify else WD_ALIGN_PARAGRAPH.LEFT)
    pf.space_after = Pt(after); pf.space_before = Pt(before); pf.line_spacing = 1.15
    runs(p, text, size=size, bold=bold); return p

def title(d, text, size=15):
    p = d.add_paragraph(); p.paragraph_format.space_after = Pt(10)
    r = p.add_run(text); r.bold = True; r.font.size = Pt(size); r.font.color.rgb = ACCENT

def heading(d, text):
    p = d.add_paragraph(); p.paragraph_format.space_before = Pt(11); p.paragraph_format.space_after = Pt(3)
    r = p.add_run(text); r.bold = True; r.font.size = Pt(12.5); r.font.color.rgb = ACCENT

def bullet(d, text):
    p = d.add_paragraph(style='List Bullet'); p.paragraph_format.space_after = Pt(2); runs(p, text)

def save(d, name):
    path = os.path.join(OUT, name); d.save(path); print("DOCX", path)

# ───────────────────────── 01 ISTANZA ─────────────────────────
d = new_doc()
for line in ["{{Nome e Cognome / Ragione sociale del richiedente}}",
             "{{Indirizzo — CAP, Città (Prov.)}}",
             "Codice Fiscale / P. IVA: {{…}}",
             "PEC: {{pec-del-richiedente@…}}   ·   Tel: {{…}}"]:
    para(d, line, justify=False, size=10, after=0)
d.add_paragraph()
para(d, "Spett.le", justify=False, after=0)
para(d, "**Ministero della Salute**", justify=False, after=0)
para(d, "**Direzione generale dei dispositivi medici e del farmaco (DGDMF)**", justify=False, after=0)
para(d, "PEC: {{indirizzo-PEC-DGDMF — DA VERIFICARE (vedi guida)}}", justify=False, size=10)
para(d, "{{Luogo}}, {{data}}", justify=False, align=WD_ALIGN_PARAGRAPH.RIGHT, before=6)
title(d, "Oggetto: richiesta di parere sulla qualificazione di un prodotto software ai sensi del Regolamento (UE) 2017/745 (MDR) — prodotto «WoundApp Docs» v1.0.0-docs", size=12)
para(d, "Il/La sottoscritto/a {{Nome Cognome}}, in qualità di {{sviluppatore / fabbricante}} del prodotto software denominato **WoundApp Docs** (applicazione Android per la documentazione clinica delle lesioni cutanee),")
para(d, "CHIEDE", bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, before=4)
para(d, "a codesta Direzione generale un **parere in merito alla qualificazione** del suddetto prodotto, ai sensi del Regolamento (UE) 2017/745, e in particolare se esso rientri o meno nella definizione di **dispositivo medico** di cui all'art. 2, punto 1, del Regolamento.")
para(d, "A tal fine rappresenta quanto segue.")
for i, t in enumerate([
    "Il prodotto è un'applicazione software destinata al personale sanitario per la sola **documentazione** clinica delle lesioni cutanee: registra, archivia, visualizza e organizza cronologicamente dati (immagini, misure, annotazioni, punteggi di scale di valutazione) inseriti manualmente dall'operatore.",
    "Il prodotto **non** fornisce diagnosi, prognosi, raccomandazioni terapeutiche o supporto alla decisione clinica, non effettua analisi automatica delle immagini né impiega algoritmi di intelligenza artificiale, e non assegna categorie/fasce di rischio né genera avvisi clinici. L'interpretazione dei dati e ogni decisione restano di esclusiva competenza del professionista sanitario. Le scale incluse (Norton, Barthel, una valutazione interna del rischio cadute, NRS-2002 nel solo screening finale, NRS dolore) restituiscono unicamente la somma aritmetica degli item. L'app include inoltre, in un'area separata delle impostazioni, un **riferimento bibliografico generale** sulle classi di medicazione (non collegato al singolo paziente, con disclaimer), descritto nella relazione tecnica e parimenti sottoposto a valutazione.",
    "Per le ragioni esposte nell'allegata relazione tecnica, e in applicazione della guida MDCG 2019-11, il richiedente ritiene che il prodotto **non** costituisca Medical Device Software (MDSW), svolgendo esclusivamente azioni di archiviazione, comunicazione, visualizzazione e semplice organizzazione dei dati inseriti dall'operatore.",
    "Trattandosi di un caso che il richiedente valuta come **borderline**, si richiede il parere dell'Autorità competente a conferma o rettifica della qualificazione proposta.",
], 1):
    p = para(d, f"{i}. {t}")
para(d, "**Riferimenti normativi:** Regolamento (UE) 2017/745, art. 2 e art. 4; MDCG 2019-11; «Manual on Borderline and Classification in the Community Regulatory Framework for Medical Devices».", before=4)
para(d, "**Si allegano:**", after=2)
for t in ["Allegato 1 — Relazione tecnica (descrizione, destinazione d'uso, motivazione).",
          "Allegato 2 (A) — Scale di valutazione utilizzate e fonti bibliografiche.",
          "Allegato 3 (B) — Trattamento dei dati e misure di sicurezza.",
          "{{eventuale Allegato 4 — Manuale d'uso / screenshot dell'applicazione}}."]:
    bullet(d, t)
para(d, "Si resta a disposizione per ogni chiarimento o integrazione documentale.", before=4)
para(d, "Distinti saluti.")
d.add_paragraph()
para(d, "{{Nome Cognome}}", justify=False, after=0)
para(d, "{{firma (digitale, oppure autografa con copia del documento d'identità)}}", justify=False, size=10)
save(d, "01_Istanza_richiesta_parere.docx")

# ───────────────────────── 02 RELAZIONE TECNICA ─────────────────────────
d = new_doc()
title(d, "Allegato 1 — Relazione tecnica")
para(d, "Prodotto: **WoundApp Docs**   ·   Versione: 1.0.0-docs   ·   Fabbricante/sviluppatore: {{…}}", size=10, after=8)
heading(d, "1. Identificazione del prodotto")
para(d, "Applicazione software per sistema operativo Android (versione minima 8.0). Funziona interamente in locale sul dispositivo, senza connessione di rete e senza componente server/cloud. Non è accessorio di alcun dispositivo medico né è incorporata o destinata a comandare un dispositivo.")
heading(d, "2. Destinazione d'uso (intended purpose)")
para(d, "WoundApp Docs è destinata all'uso da parte di personale sanitario per la documentazione clinica delle lesioni cutanee. Consente di:")
for t in ["registrare e archiviare dati anagrafici del paziente;",
          "archiviare fotografie delle lesioni selezionate dalla galleria del dispositivo (cifrate all'importazione), con cronologia (timeline);",
          "registrare misurazioni, percentuali di tessuto e annotazioni inserite manualmente dall'operatore;",
          "registrare i punteggi di cinque strumenti di valutazione (Norton, Barthel, valutazione rischio cadute — strumento interno non validato, NRS-2002 screening finale, NRS dolore), compilati dall'operatore;",
          "consultare, organizzare cronologicamente ed esportare (PDF/backup) i dati registrati."]:
    bullet(d, t)
para(d, "La destinazione d'uso è documentale: registrare, conservare, mostrare e organizzare i dati inseriti dall'operatore. L'applicazione non ha la finalità di fornire informazioni ai fini di diagnosi, prevenzione, monitoraggio, prognosi, previsione o trattamento.")
heading(d, "3. Funzionalità")
for t in ["Anagrafica/archivio pazienti (cifrato); gestione ferite per sede corporea (mappa corporea).",
          "Medicazioni con foto (da galleria, cifrate all'import), selezione poligonale dell'area della lesione, percentuali tissutali a 4 tessuti inserite manualmente (totale ≤100%), essudato e note.",
          "Compilazione delle scale (Norton, Barthel, valutazione rischio cadute interna, NRS-2002 screening finale, NRS dolore): l'operatore seleziona i singoli item; l'app ne riporta la somma aritmetica (totale grezzo definito dalla scala) e lo registra/mostra come semplice numero. Descrizioni degli item parafrasate dalle fonti, senza riproduzione verbatim.",
          "Esportazione: referto PDF del decorso, backup cifrato (completo o per singolo paziente), import non distruttivo."]:
    bullet(d, t)
heading(d, "4. Cosa il prodotto NON fa (limiti volontari)")
for t in ["**Nessuna diagnosi** e nessuna interpretazione clinica.",
          "**Nessuna analisi automatica delle immagini / nessuna AI.**",
          "**Nessuna interpretazione del risultato del singolo paziente**: nessuna categoria o fascia di rischio, nessuna soglia, nessuna classificazione dello stato del paziente; viene mostrato il punteggio numerico grezzo.",
          "**Nessun avviso/alert** clinico basato su soglie.",
          "**Nessuna raccomandazione** terapeutica personalizzata o supporto alla decisione clinica.",
          "**Nessuna stadiazione** delle lesioni (es. NPUAP/EPUAP)."]:
    bullet(d, t)
para(d, "Tali esclusioni sono implementate a livello di codice (rimosse anche dal modello dati e dai file di backup, non solo dall'interfaccia): ogni residuo di analisi automatica delle immagini è stato eliminato anche dal modello dati; le percentuali tissutali sono inserite esclusivamente a mano dall'operatore.", size=10)
para(d, "**Legenda di lettura dei grafici.** Nelle viste di andamento l'app riporta, come ausilio di lettura, l'intervallo della scala e quale estremo corrisponde - secondo la definizione dello strumento - a condizioni migliori o peggiori (es. Norton 5-20: punteggio piu' basso = condizione piu' compromessa). E' la descrizione della scala (analoga all'etichetta di un asse), non una classificazione ne' un'interpretazione del risultato del singolo paziente, e non comporta soglie, avvisi o raccomandazioni. Si sottopone anche questo elemento alla valutazione.")
para(d, "**Riferimento medicazioni (materiale educativo generale).** Nelle Impostazioni l'app include una sezione «Riferimento medicazioni»: una tabella bibliografica generale che associa tipo di tessuto/essudato alle classi di medicazione secondo letteratura di settore (EWMA, EPUAP/NPIAP, WUWHS, AIUC). E' materiale educativo generale, non collegato al singolo paziente ne' ai dati registrati, corredato di disclaimer (inclusa l'avvertenza sull'escara secca stabile). Si dichiara e si sottopone anche questo elemento alla valutazione.")
heading(d, "5. Motivazione della qualificazione proposta (non-dispositivo-medico)")
para(d, "Ai sensi dell'art. 2, punto 1, MDR, un software è dispositivo medico se ha una destinazione d'uso medica (diagnosi, prevenzione, monitoraggio, previsione, prognosi, trattamento o attenuazione di malattie) realizzata mediante un'azione sui dati che fornisce informazioni a beneficio del singolo paziente (cfr. considerando 19 MDR).")
para(d, "Applicando i passaggi decisionali della guida MDCG 2019-11:")
for t in ["il prodotto è un software ed è destinato all'ambito sanitario;",
          "esegue esclusivamente azioni di archiviazione, comunicazione, visualizzazione e semplice organizzazione/ricerca dei dati inseriti dall'operatore;",
          "non esegue alcuna ulteriore elaborazione dei dati volta a creare o fornire un'informazione clinica nuova per il singolo paziente (non interpreta, non classifica, non calcola indici diversi dalla somma elementare definita dalla scala, non segnala soglie)."]:
    bullet(d, t)
para(d, "Secondo MDCG 2019-11, un software che si limita a tali azioni non costituisce Medical Device Software e pertanto non rientra nella definizione di dispositivo medico.")
heading(d, "Punto borderline sottoposto a parere")
para(d, "Il richiedente è consapevole che la presenza di scale di valutazione (alcune delle quali — come Norton o NRS-2002 — nell'uso clinico sono associate alla stima di un rischio) può sollevare un dubbio qualificatorio. Si rappresenta che l'applicazione:")
for t in ["non calcola né mostra alcuna categoria di rischio o interpretazione del punteggio;",
          "si limita a registrare il valore numerico che l'operatore ottiene compilando la scala, alla stregua di un supporto documentale (equivalente alla trascrizione su cartella);",
          "la somma degli item è l'operazione aritmetica elementare prevista dalla scala, priva di algoritmi, pesi o logiche interpretative."]:
    bullet(d, t)
para(d, "Su questa base il richiedente propone la qualificazione come non-dispositivo-medico e ne chiede conferma. Ove l'Autorità ritenesse che la sola somma debba essere considerata elaborazione rilevante, il fabbricante è disponibile a far inserire all'operatore il totale già calcolato, eliminando anche tale operazione.")
heading(d, "6. Misure dichiarative")
para(d, "L'applicazione riporta in modo esplicito (banner, EULA, informativa) la natura di strumento di sola documentazione e la non qualificazione come dispositivo medico; il referto PDF reca analogo disclaimer.")
save(d, "02_Relazione_tecnica.docx")

# ───────────────────────── 03 ALLEGATO A ─────────────────────────
d = new_doc()
title(d, "Allegato 2 (A) — Scale di valutazione e fonti")
para(d, "L'applicazione include cinque strumenti di valutazione. Registra e mostra solo il punteggio numerico grezzo (somma aritmetica degli item); non assegna categorie di rischio, fasce, soglie o interpretazioni. Le descrizioni degli item sono parafrasate dalle fonti, senza riproduzione verbatim.")
rows = [
    ("Scala","Cosa registra l'operatore","Range","Fonte"),
    ("Norton","5 item (rischio lesioni da pressione), ciascuno 1–4","5–20","Norton et al., 1962"),
    ("Barthel","10 ADL (autonomia funzionale), pesi 0/5/10/15","0–100","Mahoney & Barthel, 1965"),
    ("Valutazione rischio cadute","6 item","0–7","Strumento interno, non validato (dichiarato come tale)"),
    ("NRS-2002 — screening finale","nutrizione (0–3) + gravità malattia (0–3) + età ≥70 (+1)","0–7","ESPEN — Kondrup et al., 2003"),
    ("NRS dolore","intensità del dolore riferito (0–10) + descrittori non interpretativi","0–10","Numeric Rating Scale (standard)"),
]
tbl = d.add_table(rows=len(rows), cols=4); tbl.style = 'Light Grid Accent 1'; tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
for ri, row in enumerate(rows):
    for ci, val in enumerate(row):
        cell = tbl.cell(ri, ci); cell.text = ''
        rp = cell.paragraphs[0]; r = rp.add_run(val); r.font.size = Pt(9.5)
        if ri == 0: r.bold = True; r.font.color.rgb = ACCENT
d.add_paragraph()
para(d, "Note:", bold=True, after=2)
for t in ["Il totale è la somma aritmetica degli item selezionati dall'operatore, definita dalla scala stessa; nessun algoritmo, peso o logica interpretativa.",
          "Le descrizioni degli item sono parafrasate dalle fonti, senza riproduzione verbatim delle pubblicazioni originali (lingue IT/EN; per FR/ES i contenuti clinici restano in inglese, per scelta dichiarata).",
          "La «Valutazione rischio cadute» è uno strumento interno e non validato (non è la scala Conley né un suo adattamento) e non viene presentata come strumento validato.",
          "Del NRS-2002 è incluso il solo screening finale; il pre-screening iniziale a 4 domande non è incluso (la schermata è etichettata «screening finale»).",
          "L'app non mostra cut-off, categorie o esiti interpretativi del punteggio del paziente; nei grafici di andamento riporta l'intervallo e la direzione della scala come ausilio di lettura (es. Norton 5-20: punteggio piu' basso = condizione piu' compromessa).",
          "Scale presenti in versioni precedenti e rimosse dal branch corrente: Braden, BWAT, MNA, MUST (copyright/licenza) e TIME/TIMERS (codice non più attivo). Non fanno parte del prodotto sottoposto a valutazione."]:
    bullet(d, t)
save(d, "03_Allegato_A_scale_e_fonti.docx")

# ───────────────────────── 04 ALLEGATO B ─────────────────────────
d = new_doc()
title(d, "Allegato 3 (B) — Trattamento dei dati e misure di sicurezza")
para(d, "Allegato a supporto: il trattamento è locale e protetto. Rilevante per la protezione dei dati; secondario ai fini della qualificazione, che dipende da destinazione d'uso e funzioni (Allegato 1).", size=10)
heading(d, "Natura locale del trattamento")
for t in ["L'app funziona interamente sul dispositivo: nessun permesso di rete (INTERNET assente), nessun server/cloud, nessun invio di dati o telemetria.",
          "Lo sviluppatore non riceve né tratta i dati. Titolare del trattamento è l'operatore/struttura sanitaria che utilizza l'app (GDPR)."]:
    bullet(d, t)
heading(d, "Dati trattati")
para(d, "Anagrafici del paziente; dati sanitari (immagini delle lesioni, misure, percentuali di tessuto, punteggi delle scale, note) — categorie particolari ex art. 9 GDPR.")
heading(d, "Misure di sicurezza (estratto)")
for t in ["Database cifrato (SQLCipher, AES-256); chiave casuale a 256 bit custodita in EncryptedSharedPreferences e protetta dall'Android Keystore; database aperto solo dopo il login.",
          "Fotografie cifrate (AES-256-GCM) su storage interno privato (importate dalla galleria e cifrate all'import; nessuna fotocamera in-app).",
          "Backup cifrato (AES-256-GCM, derivazione chiave PBKDF2-HMAC-SHA256 600.000 iterazioni, password minima 12 caratteri).",
          "Autenticazione biometrica o credenziale del dispositivo; timeout di sessione; blocco screenshot in release (FLAG_SECURE).",
          "allowBackup=false; offuscamento del codice in release; log di audit interno delle principali operazioni su pazienti e sessione (estensione alle esportazioni pianificata)."]:
    bullet(d, t)
heading(d, "Diritti dell'interessato (a supporto del titolare)")
for t in ["Informativa in-app (art. 13), con indicazione della conservazione (≥10 anni, D.Lgs. 196/2003), del diritto di reclamo al Garante (art. 77) e dell'assenza di decisioni automatizzate (art. 22); accesso/portabilità tramite export PDF del decorso e backup del singolo paziente; rettifica; cancellazione del paziente con tutti i dati e foto.",
          "Miglioria pianificata, non ancora implementata: revoca del consenso in-app (art. 7(3) GDPR)."]:
    bullet(d, t)
para(d, "Questo allegato documenta la protezione dei dati. La questione sottoposta a parere riguarda la qualificazione del prodotto (Allegato 1), non la conformità privacy.", size=10, before=4)
save(d, "04_Allegato_B_sicurezza_privacy.docx")

print("FATTO: 4 DOCX generati in", OUT)
