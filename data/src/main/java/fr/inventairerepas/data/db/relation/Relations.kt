package fr.inventairerepas.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import fr.inventairerepas.data.db.entity.DayPlanEntity
import fr.inventairerepas.data.db.entity.IngredientEntity
import fr.inventairerepas.data.db.entity.RecipeEntity
import fr.inventairerepas.data.db.entity.RecipeIngredientEntity
import fr.inventairerepas.data.db.entity.RecipeStepEntity
import fr.inventairerepas.data.db.entity.ShoppingItemEntity
import fr.inventairerepas.data.db.entity.ShoppingListEntity
import fr.inventairerepas.data.db.entity.StockLineEntity

data class RecipeIngredientWithIngredient(
    @Embedded val line: RecipeIngredientEntity,
    @Relation(parentColumn = "ingredient_id", entityColumn = "id")
    val ingredient: IngredientEntity,
)

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(entity = RecipeIngredientEntity::class, parentColumn = "id", entityColumn = "recipe_id")
    val ingredients: List<RecipeIngredientWithIngredient>,
    @Relation(parentColumn = "id", entityColumn = "recipe_id")
    val steps: List<RecipeStepEntity>,
)

data class StockLineWithIngredient(
    @Embedded val line: StockLineEntity,
    @Relation(parentColumn = "ingredient_id", entityColumn = "id")
    val ingredient: IngredientEntity,
)

data class DayPlanWithRecipe(
    @Embedded val day: DayPlanEntity,
    @Relation(entity = RecipeEntity::class, parentColumn = "recipe_id", entityColumn = "id")
    val recipe: RecipeWithDetails?,
)

data class ShoppingItemWithIngredient(
    @Embedded val item: ShoppingItemEntity,
    @Relation(parentColumn = "ingredient_id", entityColumn = "id")
    val ingredient: IngredientEntity,
)

data class ShoppingListWithItems(
    @Embedded val list: ShoppingListEntity,
    @Relation(entity = ShoppingItemEntity::class, parentColumn = "id", entityColumn = "list_id")
    val items: List<ShoppingItemWithIngredient>,
)
