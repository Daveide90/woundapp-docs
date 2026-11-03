package com.example.woundmanagement.domain.model

import com.example.woundmanagement.ui.strings.AppLanguage

enum class WoundLocation(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    SACRO("Sacro", "Sacrum", "Sacrum", "Sacro"),
    TALLONE_SX("Tallone sinistro", "Left heel", "Talon gauche", "Talón izquierdo"),
    TALLONE_DX("Tallone destro", "Right heel", "Talon droit", "Talón derecho"),
    TROCANTERE_SX("Trocantere sinistro", "Left trochanter", "Trochanter gauche", "Trocánter izquierdo"),
    TROCANTERE_DX("Trocantere destro", "Right trochanter", "Trochanter droit", "Trocánter derecho"),
    ISCHIO_SX("Ischio sinistro", "Left ischium", "Ischion gauche", "Isquion izquierdo"),
    ISCHIO_DX("Ischio destro", "Right ischium", "Ischion droit", "Isquion derecho"),
    MALLEOLO_SX("Malleolo sinistro", "Left malleolus", "Malléole gauche", "Maléolo izquierdo"),
    MALLEOLO_DX("Malleolo destro", "Right malleolus", "Malléole droite", "Maléolo derecho"),
    GOMITO_SX("Gomito sinistro", "Left elbow", "Coude gauche", "Codo izquierdo"),
    GOMITO_DX("Gomito destro", "Right elbow", "Coude droit", "Codo derecho"),
    SCAPOLA_SX("Scapola sinistra", "Left scapula", "Omoplate gauche", "Escápula izquierda"),
    SCAPOLA_DX("Scapola destra", "Right scapula", "Omoplate droite", "Escápula derecha"),
    OCCIPITE("Occipite", "Occiput", "Occiput", "Occipucio"),
    ORECCHIO_SX("Orecchio sinistro", "Left ear", "Oreille gauche", "Oreja izquierda"),
    ORECCHIO_DX("Orecchio destro", "Right ear", "Oreille droite", "Oreja derecha"),
    RACHIDE("Rachide/Spinose vertebrali", "Spine/Vertebral processes", "Rachis/Apophyses épineuses", "Raquis/Apófisis espinosas"),
    GINOCCHIO_SX("Ginocchio sinistro", "Left knee", "Genou gauche", "Rodilla izquierda"),
    GINOCCHIO_DX("Ginocchio destro", "Right knee", "Genou droit", "Rodilla derecha"),
    COSCIA_SX("Coscia sinistra", "Left thigh", "Cuisse gauche", "Muslo izquierdo"),
    COSCIA_DX("Coscia destra", "Right thigh", "Cuisse droite", "Muslo derecho"),
    GLUTEO_SX("Gluteo sinistro", "Left buttock", "Fesse gauche", "Glúteo izquierdo"),
    GLUTEO_DX("Gluteo destro", "Right buttock", "Fesse droite", "Glúteo derecho"),
    GAMBA_SX("Gamba sinistra", "Left leg", "Jambe gauche", "Pierna izquierda"),
    GAMBA_DX("Gamba destra", "Right leg", "Jambe droite", "Pierna derecha"),
    PIEDE_SX("Piede sinistro", "Left foot", "Pied gauche", "Pie izquierdo"),
    PIEDE_DX("Piede destro", "Right foot", "Pied droit", "Pie derecho"),
    MANO_SX("Mano sinistra", "Left hand", "Main gauche", "Mano izquierda"),
    MANO_DX("Mano destra", "Right hand", "Main droite", "Mano derecha"),
    TORACE("Torace", "Chest", "Thorax", "Tórax"),
    ADDOME("Addome", "Abdomen", "Abdomen", "Abdomen"),
    ALTRO("Altro", "Other", "Autre", "Otro");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class WoundType(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    ULCERA_PRESSIONE("Ulcera da pressione", "Pressure ulcer", "Ulcère de pression", "Úlcera por presión"),
    ULCERA_VENOSA("Ulcera venosa", "Venous ulcer", "Ulcère veineux", "Úlcera venosa"),
    ULCERA_ARTERIOSA("Ulcera arteriosa", "Arterial ulcer", "Ulcère artériel", "Úlcera arterial"),
    ULCERA_DIABETICA("Ulcera del piede diabetico (DFU)", "Diabetic foot ulcer (DFU)", "Ulcère du pied diabétique (UPD)", "Úlcera de pie diabético (UPD)"),
    CHIRURGICA("Lesione chirurgica", "Surgical wound", "Plaie chirurgicale", "Herida quirúrgica"),
    TRAUMATICA("Lesione traumatica", "Traumatic wound", "Plaie traumatique", "Herida traumática"),
    USTIONE("Ustione", "Burn", "Brûlure", "Quemadura"),
    NEOPLASTICA("Lesione neoplastica/maligna", "Neoplastic/malignant wound", "Plaie néoplasique/maligne", "Herida neoplásica/maligna"),
    SKIN_TEAR("Skin tear", "Skin tear", "Déchirure cutanée", "Desgarro cutáneo"),
    MASD("Dermatite associata a umidità (MASD/IAD)", "Moisture-associated skin damage (MASD/IAD)", "Dermite associée à l'humidité (MASD/IAD)", "Dermatitis asociada a la humedad (MASD/IAD)"),
    MISTA("Mista", "Mixed", "Mixte", "Mixta"),
    ALTRO("Altro", "Other", "Autre", "Otro");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class WoundOrigin(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    // COMUNITA: solo per il render dei record storici; per le nuove lesioni si usa
    // DOMICILIO ("Domicilio/Comunità"), le due voci si sovrapponevano.
    COMUNITA("Comunità", "Community", "Communauté", "Comunidad"),
    OSPEDALE("Ospedale", "Hospital", "Hôpital", "Hospital"),
    RSA("RSA", "Nursing home", "EHPAD", "Residencia"),
    DOMICILIO("Domicilio/Comunità", "Home/Community", "Domicile/Communauté", "Domicilio/Comunidad"),
    SCONOSCIUTA("Sconosciuta", "Unknown", "Inconnue", "Desconocida");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class TissueType(val label: String, val labelEn: String, val colorHex: Long) {
    NECROTICO("Necrosi (nero)", "Necrosis (black)", 0xFF1A1A1A),
    FIBRINA("Fibrina/Slough (giallo)", "Slough (yellow)", 0xFFE6C619),
    GRANULAZIONE("Granulazione (rosso)", "Granulation (red)", 0xFFCC3333),
    EPITELIZZAZIONE("Epitelizzazione (rosa)", "Epithelialization (pink)", 0xFFFFB3BA)
}

enum class ExudateLevel(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    NON_SPECIFICATO("Non specificato", "Not specified", "Non précisé", "No especificado"),
    ASSENTE("Assente", "Absent", "Absent", "Ausente"),
    SCARSO("Scarso", "Scant", "Faible", "Escaso"),
    MODERATO("Moderato", "Moderate", "Modéré", "Moderado"),
    ABBONDANTE("Abbondante", "Large", "Abondant", "Abundante");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class ExudateType(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    NON_SPECIFICATO("Non specificato", "Not specified", "Non précisé", "No especificado"),
    SIEROSO("Sieroso", "Serous", "Séreux", "Seroso"),
    SIERO_EMATICO("Siero-ematico", "Serosanguineous", "Séro-sanglant", "Serosanguinolento"),
    EMATICO("Ematico", "Sanguineous", "Sanglant", "Sanguinolento"),
    PURULENTO("Purulento", "Purulent", "Purulent", "Purulento");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class PrimaryDressing(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    NON_SPECIFICATA("Non specificata", "Not specified", "Non précisé", "No especificado"),
    SCHIUMA("Schiuma", "Foam", "Mousse", "Espuma"),
    IDROCOLLOIDE("Idrocolloide", "Hydrocolloid", "Hydrocolloïde", "Hidrocoloide"),
    ALGINATO("Alginato", "Alginate", "Alginate", "Alginato"),
    IDROGEL("Idrogel", "Hydrogel", "Hydrogel", "Hidrogel"),
    ARGENTO("Argento", "Silver", "Argent", "Plata"),
    MIELE("Miele medicale", "Medical honey", "Miel médical", "Miel medicinal"),
    COLLAGENE("Collagene", "Collagen", "Collagène", "Colágeno"),
    IDROFIBRA("Idrofibra", "Hydrofiber", "Hydrofibre", "Hidrofibra"),
    GARZA_GRASSA("Garza grassa/Paraffinata", "Paraffin gauze", "Tulle gras/paraffiné", "Gasa parafinada"),
    GARZA_IODOFORMICA("Garza iodoformica", "Iodoform gauze", "Mèche iodoformée", "Gasa yodoformada"),
    CARBONE_ATTIVO("Carbone attivo", "Activated charcoal", "Charbon actif", "Carbón activado"),
    MATRICE_MODULANTE("Matrice modulante proteasi", "Protease modulating matrix", "Matrice modulant les protéases", "Matriz moduladora de proteasas"),
    VAC("Pressione negativa (VAC)", "Negative pressure (VAC)", "Pression négative (VAC)", "Presión negativa (VAC)"),
    CONTACT_LAYER("Interfaccia siliconica (contact layer)", "Silicone contact layer", "Interface siliconée", "Interfaz de silicona"),
    SUPERASSORBENTE("Superassorbente", "Superabsorbent", "Superabsorbant", "Superabsorbente"),
    ANTISETTICA("Antisettica (PHMB/iodio cadexomero)", "Antiseptic (PHMB/cadexomer iodine)", "Antiseptique (PHMB/iode cadexomère)", "Antiséptica (PHMB/yodo cadexómero)"),
    NESSUNA("Nessuna", "None", "Aucun", "Ninguno"),
    ALTRO("Altro", "Other", "Autre", "Otro");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

enum class SecondaryDressing(val label: String, val labelEn: String, val labelFr: String, val labelEs: String) {
    NON_SPECIFICATA("Non specificata", "Not specified", "Non précisé", "No especificado"),
    FILM("Film trasparente", "Transparent film", "Film transparent", "Película transparente"),
    SCHIUMA("Schiuma", "Foam", "Mousse", "Espuma"),
    GARZA("Garza", "Gauze", "Compresse", "Gasa"),
    BENDA_ELASTICA("Benda elastica", "Elastic bandage", "Bande élastique", "Venda elástica"),
    BENDA_COESIVA("Benda coesiva", "Cohesive bandage", "Bande cohésive", "Venda cohesiva"),
    CEROTTO("Cerotto", "Adhesive tape", "Sparadrap", "Esparadrapo"),
    RETE_TUBOLARE("Rete tubolare", "Tubular net", "Filet tubulaire", "Malla tubular"),
    COMPRESSIONE_MULTISTRATO("Bendaggio compressivo multistrato", "Multilayer compression bandage", "Bandage compressif multicouche", "Vendaje compresivo multicapa"),
    SUPERASSORBENTE("Superassorbente", "Superabsorbent", "Superabsorbant", "Superabsorbente"),
    NESSUNA("Nessuna", "None", "Aucun", "Ninguno"),
    ALTRO("Altro", "Other", "Autre", "Otro");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.IT -> label
        AppLanguage.EN -> labelEn
        AppLanguage.FR -> labelFr
        AppLanguage.ES -> labelEs
    }
}

/**
 * Risolve un codice sede (WoundLocation o BodyZone name) nell'etichetta localizzata.
 * Fallback: codice con underscore sostituiti (mai stringhe grezze tipo "CAVIGLIA_DX_ANT").
 */
fun locationDisplay(code: String, lang: AppLanguage): String {
    runCatching { return WoundLocation.valueOf(code).label(lang) }
    runCatching {
        return com.example.woundmanagement.ui.components.BodyZone.valueOf(code).label(lang)
    }
    return code.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
