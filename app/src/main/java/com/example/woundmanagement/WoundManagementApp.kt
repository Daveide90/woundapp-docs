package com.example.woundmanagement

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

class WoundManagementApp : Application() {
    /**
     * Il database viene aperto in modo LAZY, al primo accesso (che avviene solo dopo il
     * login: l'unico accesso pre-login era nel NavHost ed è stato rimosso). Così il DB
     * cifrato non resta montato a riposo mentre l'app è bloccata sulla schermata di login.
     */
    // Getter (non lazy): se il DB viene chiuso al timeout (closeInstance), il prossimo
    // accesso lo riapre via getInstance. Con `by lazy` resterebbe l'istanza chiusa.
    val database: com.example.woundmanagement.data.db.AppDatabase
        get() = com.example.woundmanagement.data.db.AppDatabase.getInstance(this)

    /**
     * Scope legato al ciclo di vita dell'applicazione (non a una schermata). Usato per i
     * salvataggi che NON devono essere cancellati quando la UI naviga via (es. salvataggio
     * delle scale, che prima girava su viewModelScope e veniva troncato dal popBackStack).
     */
    // SupervisorJob: un figlio fallito non uccide lo scope. L'handler evita che una
    // eccezione non gestita in un launch (es. SQLiteException) crashi l'intera app:
    // le operazioni critiche hanno comunque i loro try/catch per il feedback in UI.
    val applicationScope = CoroutineScope(
        SupervisorJob() + kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
            if (BuildConfig.DEBUG) android.util.Log.e("WoundApp", "Uncaught in applicationScope", e)
        }
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        // NB: il DB NON viene aperto qui (vedi `database`): si apre al primo accesso post-login.
        clearExportCache()
    }

    /**
     * Pulizia best-effort dei residui di export: PDF di referto e backup vengono creati in
     * cacheDir/exports e cacheDir/backups per la condivisione. All'avvio cancelliamo quei file
     * (in chiaro o cifrati) così non restano indefinitamente in cache. Mai bloccante.
     */
    fun clearExportCache() {
        for (dirName in listOf("exports", "backups")) {
            try {
                File(cacheDir, dirName).listFiles()?.forEach { file ->
                    try { if (file.isFile) file.delete() } catch (_: Exception) { }
                }
            } catch (_: Exception) { /* ignora: non deve mai bloccare l'avvio */ }
        }
    }

    companion object {
        lateinit var instance: WoundManagementApp
            private set
    }
}
