package fr.inventairerepas.data.db

import androidx.room.TypeConverter
import fr.inventairerepas.domain.model.RelaxedConstraint
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun relaxedToString(relaxed: Set<RelaxedConstraint>): String =
        relaxed.sortedBy { it.ordinal }.joinToString(",") { it.name }

    @TypeConverter
    fun stringToRelaxed(value: String): Set<RelaxedConstraint> =
        value.split(',').filter { it.isNotBlank() }.map(RelaxedConstraint::valueOf).toSet()
}
