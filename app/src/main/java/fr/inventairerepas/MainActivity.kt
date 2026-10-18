package fr.inventairerepas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import fr.inventairerepas.ui.navigation.AppNavHost
import fr.inventairerepas.ui.theme.InventaireRepasTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InventaireRepasTheme {
                AppNavHost()
            }
        }
    }
}
