package com.example.woundmanagement.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.R
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.loc4
import java.text.SimpleDateFormat
import java.util.*

// Zone del corpo mappate alle location del database.
// La mappatura (dbLocations) NON va modificata: garantisce che una ferita salvata
// con un certo codice WoundLocation venga evidenziata nella zona corretta.
enum class BodyZone(val label: String, val labelEn: String, val labelFr: String, val labelEs: String, val dbLocations: List<String>) {
    // ANTERIORE
    FRONTE("Fronte", "Forehead", "Front", "Frente", listOf("FRONTE")),
    VISO("Viso", "Face", "Visage", "Cara", listOf("VISO")),
    COLLO_ANT("Collo Ant.", "Neck Ant.", "Cou ant.", "Cuello ant.", listOf("COLLO_ANT")),
    SPALLA_DX_ANT("Spalla Dx Ant.", "R.Shoulder Ant.", "Épaule dr. ant.", "Hombro der. ant.", listOf("SPALLA_DX_ANT")),
    SPALLA_SX_ANT("Spalla Sx Ant.", "L.Shoulder Ant.", "Épaule g. ant.", "Hombro izq. ant.", listOf("SPALLA_SX_ANT")),
    BRACCIO_DX_ANT("Braccio Dx Ant.", "R.Arm Ant.", "Bras dr. ant.", "Brazo der. ant.", listOf("BRACCIO_DX_ANT", "GOMITO_DX", "MANO_DX", "AVAMBRACCIO_DX", "AVAMBRACCIO")),
    BRACCIO_SX_ANT("Braccio Sx Ant.", "L.Arm Ant.", "Bras g. ant.", "Brazo izq. ant.", listOf("BRACCIO_SX_ANT", "GOMITO_SX", "MANO_SX", "AVAMBRACCIO_SX")),
    TORACE("Torace", "Chest", "Thorax", "Tórax", listOf("TORACE")),
    ADDOME("Addome", "Abdomen", "Abdomen", "Abdomen", listOf("ADDOME")),
    COSCIA_DX_ANT("Coscia Dx Ant.", "R.Thigh Ant.", "Cuisse dr. ant.", "Muslo der. ant.", listOf("COSCIA_DX_ANT", "COSCIA_DX")),
    COSCIA_SX_ANT("Coscia Sx Ant.", "L.Thigh Ant.", "Cuisse g. ant.", "Muslo izq. ant.", listOf("COSCIA_SX_ANT", "COSCIA_SX")),
    GINOCCHIO_DX_ANT("Ginocchio Dx Ant.", "R.Knee Ant.", "Genou dr. ant.", "Rodilla der. ant.", listOf("GINOCCHIO_DX_ANT")),
    GINOCCHIO_SX_ANT("Ginocchio Sx Ant.", "L.Knee Ant.", "Genou g. ant.", "Rodilla izq. ant.", listOf("GINOCCHIO_SX_ANT")),
    GAMBA_DX_ANT("Gamba Dx Ant.", "R.Leg Ant.", "Jambe dr. ant.", "Pierna der. ant.", listOf("GAMBA_DX_ANT")),
    GAMBA_SX_ANT("Gamba Sx Ant.", "L.Leg Ant.", "Jambe g. ant.", "Pierna izq. ant.", listOf("GAMBA_SX_ANT")),
    CAVIGLIA_DX_ANT("Caviglia Dx Ant.", "R.Ankle Ant.", "Cheville dr. ant.", "Tobillo der. ant.", listOf("MALLEOLO_DX")),
    CAVIGLIA_SX_ANT("Caviglia Sx Ant.", "L.Ankle Ant.", "Cheville g. ant.", "Tobillo izq. ant.", listOf("MALLEOLO_SX")),
    PIEDE_DX("Piede Dx", "R.Foot", "Pied dr.", "Pie der.", listOf("PIEDE_DX")),
    PIEDE_SX("Piede Sx", "L.Foot", "Pied g.", "Pie izq.", listOf("PIEDE_SX")),
    PERINEO("Perineo", "Perineum", "Périnée", "Periné", listOf("PERINEO")),

    // POSTERIORE
    OCCIPITE("Occipite", "Occiput", "Occiput", "Occipucio", listOf("OCCIPITE", "ORECCHIO_DX", "ORECCHIO_SX")),
    COLLO_POST("Collo Post.", "Neck Post.", "Cou post.", "Cuello post.", listOf("COLLO_POST")),
    SPALLA_DX_POST("Spalla Dx Post.", "R.Shoulder Post.", "Épaule dr. post.", "Hombro der. post.", listOf("SCAPOLA_DX")),
    SPALLA_SX_POST("Spalla Sx Post.", "L.Shoulder Post.", "Épaule g. post.", "Hombro izq. post.", listOf("SCAPOLA_SX")),
    BRACCIO_DX_POST("Braccio Dx Post.", "R.Arm Post.", "Bras dr. post.", "Brazo der. post.", listOf("BRACCIO_DX_POST")),
    BRACCIO_SX_POST("Braccio Sx Post.", "L.Arm Post.", "Bras g. post.", "Brazo izq. post.", listOf("BRACCIO_SX_POST")),
    DORSO("Dorso", "Back", "Dos", "Espalda", listOf("DORSO", "RACHIDE")),
    SACRO("Sacro", "Sacrum", "Sacrum", "Sacro", listOf("SACRO")),
    GLUTEO_DX("Gluteo Dx", "R.Buttock", "Fesse dr.", "Glúteo der.", listOf("ISCHIO_DX", "TROCANTERE_DX", "ISCHIO")),
    GLUTEO_SX("Gluteo Sx", "L.Buttock", "Fesse g.", "Glúteo izq.", listOf("ISCHIO_SX", "TROCANTERE_SX")),
    COSCIA_DX_POST("Coscia Dx Post.", "R.Thigh Post.", "Cuisse dr. post.", "Muslo der. post.", listOf("COSCIA_DX_POST")),
    COSCIA_SX_POST("Coscia Sx Post.", "L.Thigh Post.", "Cuisse g. post.", "Muslo izq. post.", listOf("COSCIA_SX_POST")),
    GINOCCHIO_DX_POST("Ginocchio Dx Post.", "R.Knee Post.", "Genou dr. post.", "Rodilla der. post.", listOf("GINOCCHIO_DX_POST")),
    GINOCCHIO_SX_POST("Ginocchio Sx Post.", "L.Knee Post.", "Genou g. post.", "Rodilla izq. post.", listOf("GINOCCHIO_SX_POST")),
    GAMBA_DX_POST("Gamba Dx Post.", "R.Leg Post.", "Jambe dr. post.", "Pierna der. post.", listOf("GAMBA_DX")),
    GAMBA_SX_POST("Gamba Sx Post.", "L.Leg Post.", "Jambe g. post.", "Pierna izq. post.", listOf("GAMBA_SX")),
    TALLONE_DX("Tallone Dx", "R.Heel", "Talon dr.", "Talón der.", listOf("TALLONE_DX")),
    TALLONE_SX("Tallone Sx", "L.Heel", "Talon g.", "Talón izq.", listOf("TALLONE_SX"));

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }

    fun hasWound(wounds: List<WoundAssessmentEntity>): Boolean {
        return wounds.any { wound -> dbLocations.contains(wound.location) || wound.location == name }
    }

    fun getWounds(wounds: List<WoundAssessmentEntity>): List<WoundAssessmentEntity> {
        return wounds.filter { wound -> dbLocations.contains(wound.location) || wound.location == name }
    }
}

// Posizione (x, y) di ogni zona, in frazioni dell'immagine UNICA body_map.png
// (figura anteriore a sinistra, posteriore a destra). Misurate sull'immagine reale.
// Convenzione anatomica: vista anteriore = lato Dx del paziente a sinistra schermo;
// vista posteriore (da dietro) = lato Dx del paziente a destra schermo.
// Marker ANATOMICI: il centro del cerchio cade DENTRO il distretto corporeo (non sul
// contorno della figura), e le coppie bilaterali sono simmetriche rispetto all'asse mediano
// (anteriore 0.247, posteriore 0.747). Coordinate in frazioni dell'immagine body_map.png.
private const val AXIS_FRONT = 0.247f
private const val AXIS_BACK = 0.747f
private val bodyMapPositions: List<Pair<BodyZone, Pair<Float, Float>>> = listOf(
    // ── Anteriore (figura di sinistra), simmetrici su AXIS_FRONT ──
    BodyZone.FRONTE to (AXIS_FRONT to 0.048f),
    BodyZone.VISO to (AXIS_FRONT to 0.088f),
    BodyZone.COLLO_ANT to (AXIS_FRONT to 0.132f),
    BodyZone.SPALLA_DX_ANT to (0.149f to 0.190f),
    BodyZone.SPALLA_SX_ANT to (0.345f to 0.190f),
    BodyZone.TORACE to (AXIS_FRONT to 0.210f),
    BodyZone.BRACCIO_DX_ANT to (0.122f to 0.345f),
    BodyZone.BRACCIO_SX_ANT to (0.372f to 0.345f),
    BodyZone.ADDOME to (AXIS_FRONT to 0.345f),
    BodyZone.PERINEO to (AXIS_FRONT to 0.512f),
    BodyZone.COSCIA_DX_ANT to (0.205f to 0.545f),
    BodyZone.COSCIA_SX_ANT to (0.289f to 0.545f),
    BodyZone.GINOCCHIO_DX_ANT to (0.214f to 0.665f),
    BodyZone.GINOCCHIO_SX_ANT to (0.280f to 0.665f),
    // sotto il ginocchio: marker spostati più all'esterno (seguono la gamba, non il centro)
    BodyZone.GAMBA_DX_ANT to (0.199f to 0.775f),
    BodyZone.GAMBA_SX_ANT to (0.295f to 0.775f),
    BodyZone.CAVIGLIA_DX_ANT to (0.207f to 0.880f),
    BodyZone.CAVIGLIA_SX_ANT to (0.287f to 0.880f),
    BodyZone.PIEDE_DX to (0.195f to 0.950f),
    BodyZone.PIEDE_SX to (0.299f to 0.950f),
    // ── Posteriore (figura di destra), simmetrici su AXIS_BACK ──
    BodyZone.OCCIPITE to (AXIS_BACK to 0.050f),
    BodyZone.COLLO_POST to (AXIS_BACK to 0.132f),
    BodyZone.SPALLA_SX_POST to (0.667f to 0.185f),
    BodyZone.SPALLA_DX_POST to (0.827f to 0.185f),
    BodyZone.DORSO to (AXIS_BACK to 0.270f),
    BodyZone.BRACCIO_SX_POST to (0.624f to 0.345f),
    BodyZone.BRACCIO_DX_POST to (0.870f to 0.345f),
    BodyZone.SACRO to (AXIS_BACK to 0.430f),
    BodyZone.GLUTEO_SX to (0.707f to 0.525f),
    BodyZone.GLUTEO_DX to (0.787f to 0.525f),
    BodyZone.COSCIA_SX_POST to (0.713f to 0.595f),
    BodyZone.COSCIA_DX_POST to (0.781f to 0.595f),
    BodyZone.GINOCCHIO_SX_POST to (0.714f to 0.660f),
    BodyZone.GINOCCHIO_DX_POST to (0.780f to 0.660f),
    // sotto il ginocchio: marker spostati più all'esterno
    BodyZone.GAMBA_SX_POST to (0.699f to 0.770f),
    BodyZone.GAMBA_DX_POST to (0.795f to 0.770f),
    BodyZone.TALLONE_SX to (0.707f to 0.935f),
    BodyZone.TALLONE_DX to (0.787f to 0.935f),
)

@Composable
fun WoundBodyMap(
    wounds: List<WoundAssessmentEntity>,
    onZoneClick: (BodyZone, List<WoundAssessmentEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalLanguage.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Titoli sopra le due figure
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                when (lang) {
                    AppLanguage.IT -> "FRONTE"
                    AppLanguage.FR -> "FACE"
                    AppLanguage.ES -> "FRENTE"
                    else -> "FRONT"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                when (lang) {
                    AppLanguage.IT -> "RETRO"
                    AppLanguage.FR -> "DOS"
                    AppLanguage.ES -> "ESPALDA"
                    else -> "BACK"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        BodyFigure(wounds = wounds, onZoneClick = onZoneClick)

        Spacer(modifier = Modifier.height(12.dp))

        // Legenda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                wound = false,
                label = when (lang) {
                    AppLanguage.IT -> "Nessuna lesione"
                    AppLanguage.FR -> "Aucune plaie"
                    AppLanguage.ES -> "Sin herida"
                    else -> "No wound"
                }
            )
            LegendItem(
                wound = true,
                label = when (lang) {
                    AppLanguage.IT -> "Lesione presente"
                    AppLanguage.FR -> "Plaie présente"
                    AppLanguage.ES -> "Herida presente"
                    else -> "Wound present"
                }
            )
        }
    }
}

@Composable
private fun LegendItem(wound: Boolean, label: String) {
    val dotColor = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (wound) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .border(1.5.dp, MaterialTheme.colorScheme.errorContainer, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.20f))
                    .border(1.4.dp, dotColor.copy(alpha = 0.7f), CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Mostra l'immagine anatomica (anteriore + posteriore affiancate) e vi sovrappone i punti
 * cliccabili delle zone. L'immagine, con FillWidth, definisce l'altezza del riquadro; i punti
 * usano le dimensioni REALI di quello stesso riquadro → allineamento garantito, nessun taglio.
 */
@Composable
private fun BodyFigure(
    wounds: List<WoundAssessmentEntity>,
    onZoneClick: (BodyZone, List<WoundAssessmentEntity>) -> Unit
) {
    val dotColor = MaterialTheme.colorScheme.primary
    val lang = LocalLanguage.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.body_map),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
        // Hit-test "nearest marker": il tocco seleziona il marker più vicino entro 28dp,
        // così due zone ravvicinate (es. sacro/glutei) non si rubano il tocco a vicenda.
        BoxWithConstraints(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(wounds) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectTapGestures
                        var best: BodyZone? = null
                        var bestDist = Float.MAX_VALUE
                        for ((zone, pos) in bodyMapPositions) {
                            val dx = offset.x - pos.first * w
                            val dy = offset.y - pos.second * h
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist < bestDist) { bestDist = dist; best = zone }
                        }
                        val threshold = with(density) { 28.dp.toPx() }
                        best?.takeIf { bestDist <= threshold }
                            ?.let { onZoneClick(it, it.getWounds(wounds)) }
                    }
                }
        ) {
            val w = maxWidth
            val h = maxHeight
            for ((zone, pos) in bodyMapPositions) {
                val zoneWounds = zone.getWounds(wounds)
                val hasWound = zoneWounds.isNotEmpty()
                val state = when (lang) {
                    AppLanguage.IT -> if (hasWound) "lesione presente" else "nessuna lesione"
                    AppLanguage.FR -> if (hasWound) "plaie présente" else "aucune plaie"
                    AppLanguage.ES -> if (hasWound) "herida presente" else "sin herida"
                    else -> if (hasWound) "wound present" else "no wound"
                }
                val desc = "${zone.label(lang)} – $state"

                key(zone) {
                    Box(
                        modifier = Modifier
                            // Punto centrato sulla zona: offset = -metà della size (50dp -> -25dp).
                            .offset(x = w * pos.first - 25.dp, y = h * pos.second - 25.dp)
                            .size(50.dp)
                            // Solo semantica per l'accessibilità: il tocco reale lo gestisce
                            // il pointerInput del padre (nearest marker).
                            .semantics {
                                contentDescription = desc
                                role = Role.Button
                                onClick(label = desc) { onZoneClick(zone, zoneWounds); true }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasWound) {
                            // Palette error del tema (un solo punto di verità), non rossi hardcoded
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .border(2.dp, MaterialTheme.colorScheme.errorContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Healing,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = 0.22f))
                                    .border(1.5.dp, dotColor.copy(alpha = 0.75f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneWoundsDialog(
    zone: BodyZone,
    zoneWounds: List<WoundAssessmentEntity>,
    onDismiss: () -> Unit,
    onNewWound: () -> Unit,
    onWoundClick: (Long) -> Unit,
    onDeleteWound: (WoundAssessmentEntity) -> Unit = {},
    onEditWoundNotes: (WoundAssessmentEntity) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val lang = LocalLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(zone.label(lang)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Con 3+ lesioni nella stessa zona il contenuto supera l'altezza del
                    // dialog: senza scroll veniva tagliato (come già in NewPatientDialog)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (zoneWounds.isEmpty()) {
                    Text(
                        loc4(
                            it = "Nessuna lesione in questa zona",
                            en = "No wounds in this zone",
                            fr = "Aucune plaie dans cette zone",
                            es = "Sin heridas en esta zona"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        loc4(it = "Lesioni presenti:", en = "Wounds present:", fr = "Plaies présentes :", es = "Heridas presentes:"),
                        style = MaterialTheme.typography.titleSmall
                    )
                    zoneWounds.forEach { wound ->
                        // B6: codice enum -> etichetta localizzata (fallback: codice leggibile)
                        val baseTypeLabel = runCatching {
                            com.example.woundmanagement.domain.model.WoundType.valueOf(wound.type)
                        }.getOrNull()?.label(lang) ?: wound.type.replace("_", " ")
                        // Tipo ALTRO con testo libero: "Altro: <specifica>"
                        val typeLabel =
                            if (wound.type == "ALTRO" && wound.typeOther.isNotBlank())
                                "$baseTypeLabel: ${wound.typeOther}"
                            else baseTypeLabel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWoundClick(wound.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Nome ferita in evidenza + cestino
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        wound.name.ifBlank { typeLabel },
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteWound(wound) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = loc4(it = "Elimina lesione", en = "Delete wound", fr = "Supprimer la plaie", es = "Eliminar herida"),
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    typeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (wound.lengthCm > 0 || wound.widthCm > 0) {
                                    Text(
                                        // Stesso formato del resto dell'app: il Float grezzo
                                        // mostrava code decimali tipo "3.0999999"
                                        "%.1f × %.1f cm".format(wound.lengthCm, wound.widthCm),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                // Note/anamnesi con matita per la modifica
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        wound.notes.ifBlank {
                                            loc4(it = "Aggiungi note…", en = "Add notes…", fr = "Ajouter des notes…", es = "Añadir notas…")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        color = if (wound.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onEditWoundNotes(wound) }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = loc4(it = "Modifica note", en = "Edit notes", fr = "Modifier les notes", es = "Editar notas"),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    dateFormat.format(Date(wound.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    loc4(
                                        it = "Tocca per nuova medicazione →",
                                        en = "Tap for new dressing →",
                                        fr = "Touchez pour un nouveau pansement →",
                                        es = "Toque para nueva cura →"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onNewWound) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(loc4(it = "Nuova Lesione", en = "New Wound", fr = "Nouvelle plaie", es = "Nueva herida"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(loc4(it = "Chiudi", en = "Close", fr = "Fermer", es = "Cerrar"))
            }
        }
    )
}
