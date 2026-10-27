package fr.inventairerepas.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.inventairerepas.data.repository.RoomDriveLinkRepository
import fr.inventairerepas.data.repository.RoomIngredientRepository
import fr.inventairerepas.data.repository.RoomPlanRepository
import fr.inventairerepas.data.repository.RoomRecipeRepository
import fr.inventairerepas.data.repository.RoomShoppingRepository
import fr.inventairerepas.data.repository.RoomStockRepository
import fr.inventairerepas.data.repository.RoomTransactionRunner
import fr.inventairerepas.data.settings.DataStoreSettingsRepository
import fr.inventairerepas.domain.drive.DriveLinkRepository
import fr.inventairerepas.domain.menu.MenuGenerator
import fr.inventairerepas.domain.repository.IngredientRepository
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun recipes(impl: RoomRecipeRepository): RecipeRepository

    @Binds abstract fun ingredients(impl: RoomIngredientRepository): IngredientRepository

    @Binds abstract fun stock(impl: RoomStockRepository): StockRepository

    @Binds abstract fun plans(impl: RoomPlanRepository): PlanRepository

    @Binds abstract fun shopping(impl: RoomShoppingRepository): ShoppingRepository

    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds abstract fun transactions(impl: RoomTransactionRunner): TransactionRunner

    @Binds abstract fun driveLinks(impl: RoomDriveLinkRepository): DriveLinkRepository

    companion object {
        @Provides
        fun menuGenerator(): MenuGenerator = MenuGenerator(Random.Default)
    }
}
