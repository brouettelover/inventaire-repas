package fr.inventairerepas.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.inventairerepas.ui.recipe.detail.RecipeDetailScreen
import fr.inventairerepas.ui.recipe.edit.RecipeEditScreen
import fr.inventairerepas.ui.recipe.list.RecipeListScreen
import fr.inventairerepas.ui.settings.SettingsScreen
import fr.inventairerepas.ui.shopping.ShoppingScreen
import fr.inventairerepas.ui.stock.StockScreen
import fr.inventairerepas.ui.week.WeekScreen
import kotlin.reflect.KClass

private data class TopLevel(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>)

private val topLevels = listOf(
    TopLevel("Semaine", Icons.Filled.CalendarMonth, WeekRoute, WeekRoute::class),
    TopLevel("Recettes", Icons.AutoMirrored.Filled.MenuBook, RecipesRoute, RecipesRoute::class),
    TopLevel("Courses", Icons.Filled.ShoppingCart, ShoppingRoute, ShoppingRoute::class),
    TopLevel("Stock", Icons.Filled.Kitchen, StockRoute, StockRoute::class),
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevels.any { top -> destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevels.forEach { top ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true,
                            onClick = { navController.navigateTopLevel(top.route) },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(top.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = WeekRoute,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<WeekRoute> {
                WeekScreen(
                    onOpenRecipe = { recipeId, date -> navController.navigate(RecipeDetailRoute(recipeId, date.toEpochDay())) },
                    onPickRecipe = { weekStart, date ->
                        navController.navigate(PickRecipeRoute(weekStart.toEpochDay(), date.toEpochDay()))
                    },
                    onOpenShopping = { navController.navigateTopLevel(ShoppingRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<RecipesRoute> {
                RecipeListScreen(
                    onOpenRecipe = { navController.navigate(RecipeDetailRoute(it)) },
                    onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
                    onBack = null,
                )
            }
            composable<PickRecipeRoute> {
                RecipeListScreen(
                    onOpenRecipe = { navController.navigate(RecipeDetailRoute(it)) },
                    onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<RecipeDetailRoute> {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(RecipeEditRoute(it)) },
                )
            }
            composable<RecipeEditRoute> {
                RecipeEditScreen(
                    onBack = { navController.popBackStack() },
                    // Après suppression, le détail n'existe plus : on revient à la liste ou au menu.
                    onDeleted = {
                        if (!navController.popBackStack(RecipesRoute, inclusive = false)) {
                            navController.popBackStack(WeekRoute, inclusive = false)
                        }
                    },
                )
            }
            composable<ShoppingRoute> { ShoppingScreen() }
            composable<StockRoute> { StockScreen() }
            composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
