package fr.inventairerepas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import fr.inventairerepas.data.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class InventaireRepasApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seeder.seedIfEmpty() }
    }
}
