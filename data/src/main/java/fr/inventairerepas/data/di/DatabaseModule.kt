package fr.inventairerepas.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.inventairerepas.data.db.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideIngredientDao(db: AppDatabase) = db.ingredientDao()

    @Provides
    fun provideRecipeDao(db: AppDatabase) = db.recipeDao()

    @Provides
    fun provideStockDao(db: AppDatabase) = db.stockDao()

    @Provides
    fun providePlanDao(db: AppDatabase) = db.planDao()

    @Provides
    fun provideShoppingDao(db: AppDatabase) = db.shoppingDao()
}
