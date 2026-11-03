package com.example.woundmanagement

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.woundmanagement.ui.theme.WoundManagementTheme

class MainActivity : FragmentActivity() {

    // File di backup aperto/condiviso dal file manager (ACTION_VIEW / ACTION_SEND).
    private val importUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Impedisce screenshot e screen recording su TUTTE le schermate, sempre (anche nei
        // build di debug): contiene dati sanitari. Per gli screenshot dello store si usa un
        // emulatore disabilitando temporaneamente questo flag, non una deroga nel codice.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        importUri.value = extractBackupUri(intent)
        setContent {
            WoundManagementTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WoundManagementNavHost(
                        importUri = importUri.value,
                        onImportConsumed = { importUri.value = null }
                    )
                }
            }
        }
    }

    // launchMode di default: un "apri file" a freddo arriva da onCreate(getIntent).
    // onNewIntent resta come rete di sicurezza se l'Activity viene riusata (intent SINGLE_TOP).
    // NB: niente singleTask/singleInstance, che romperebbero startActivityForResult (il selettore file).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractBackupUri(intent)?.let { importUri.value = it }
    }

    private fun extractBackupUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
        else -> null
    }
}
