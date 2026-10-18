package fr.inventairerepas.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Réglages stockés dans DataStore. Une durée maximale négative signifie « illimitée ». */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val firstShoppingDay = stringPreferencesKey("first_shopping_day")
        val defaultServings = intPreferencesKey("default_servings")
        val googleAccount = stringPreferencesKey("google_account")
        fun maxMinutes(day: DayOfWeek) = intPreferencesKey("max_minutes_${day.name.lowercase()}")
    }

    private val defaults = Settings()

    override val settings: Flow<Settings> = context.settingsDataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (Settings) -> Settings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.firstShoppingDay] = updated.firstShoppingDay.name
            prefs[Keys.defaultServings] = updated.defaultServings
            DayOfWeek.entries.forEach { day ->
                prefs[Keys.maxMinutes(day)] = updated.maxMinutesFor(day) ?: UNLIMITED
            }
            val account = updated.googleAccountEmail
            if (account == null) {
                prefs.remove(Keys.googleAccount)
            } else {
                prefs[Keys.googleAccount] = account
            }
        }
    }

    private fun Preferences.toSettings() = Settings(
        firstShoppingDay = this[Keys.firstShoppingDay]?.let(DayOfWeek::valueOf) ?: defaults.firstShoppingDay,
        defaultServings = this[Keys.defaultServings] ?: defaults.defaultServings,
        maxMinutesByDay = DayOfWeek.entries.associateWith { day ->
            when (val stored = this[Keys.maxMinutes(day)]) {
                null -> defaults.maxMinutesFor(day)
                UNLIMITED -> null
                else -> stored
            }
        },
        googleAccountEmail = this[Keys.googleAccount],
    )

    private companion object {
        const val UNLIMITED = -1
    }
}
