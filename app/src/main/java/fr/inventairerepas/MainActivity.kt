package fr.inventairerepas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fr.inventairerepas.sync.RecipeDriveSync
import fr.inventairerepas.ui.navigation.AppNavHost
import fr.inventairerepas.ui.theme.InventaireRepasTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var driveSync: RecipeDriveSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InventaireRepasTheme {
                AppNavHost()
            }
        }
        // Recettes Drive : synchronisées à l'ouverture de l'app, puis régulièrement tant qu'elle est affichée.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    driveSync.requestSync()
                    delay(SYNC_INTERVAL_MS)
                }
            }
        }
    }

    private companion object {
        const val SYNC_INTERVAL_MS = 5 * 60 * 1000L
    }
}
