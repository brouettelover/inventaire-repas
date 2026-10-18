package fr.inventairerepas.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Semaine affichée, en nombre de semaines depuis la semaine en cours ; partagée entre « Semaine » et « Courses ». */
@Singleton
class SelectedWeek @Inject constructor() {
    private val _offset = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset.asStateFlow()

    fun previous() = _offset.update { it - 1 }

    /** On ne planifie pas au-delà de la semaine suivante. */
    fun next() = _offset.update { minOf(it + 1, MAX_OFFSET) }

    companion object {
        const val MAX_OFFSET = 1
    }
}
