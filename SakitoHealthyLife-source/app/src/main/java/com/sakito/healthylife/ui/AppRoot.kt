package com.sakito.healthylife.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sakito.healthylife.ui.screens.AddRecordScreen
import com.sakito.healthylife.ui.screens.BodyScreen
import com.sakito.healthylife.ui.screens.CombinedFoodScreen
import com.sakito.healthylife.ui.screens.FoodEditScreen
import com.sakito.healthylife.ui.screens.FoodLibraryScreen
import com.sakito.healthylife.ui.screens.HomeScreen
import com.sakito.healthylife.ui.screens.SettingsScreen
import com.sakito.healthylife.ui.screens.StatsScreen
import com.sakito.healthylife.ui.viewmodel.MainViewModel

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomItems = listOf(
    BottomItem("home", "首页", Icons.Default.Home),
    BottomItem("foods", "食物库", Icons.Default.Restaurant),
    BottomItem("body", "身体", Icons.Default.MonitorWeight),
    BottomItem("stats", "统计", Icons.Default.BarChart),
    BottomItem("settings", "设置", Icons.Default.Settings)
)

@Composable
fun AppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onAddRecord = { date -> navController.navigate("record/add?date=$date") },
                    onEditRecord = { id, date -> navController.navigate("record/$id?date=$date") },
                    onManageFoods = { navController.navigate("foods") }
                )
            }
            composable("foods") {
                FoodLibraryScreen(
                    onNewFood = { navController.navigate("food/new") },
                    onEditFood = { id -> navController.navigate("food/$id") },
                    onNewCombined = { navController.navigate("food/combined") },
                    onImportFoods = { navController.navigate("settings") }, // Quick path: settings has CSV import
                    onExportFoods = { navController.navigate("settings") }
                )
            }
            composable("body") {
                BodyScreen()
            }
            composable("stats") {
                StatsScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("food/new") {
                FoodEditScreen(
                    foodId = 0,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "food/{foodId}",
                arguments = listOf(androidx.navigation.navArgument("foodId") { type = androidx.navigation.NavType.LongType })
            ) { entry ->
                val foodId = entry.arguments?.getLong("foodId") ?: 0L
                FoodEditScreen(
                    foodId = foodId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("food/combined") {
                CombinedFoodScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "record/add?date={date}",
                arguments = listOf(
                    androidx.navigation.navArgument("date") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val date = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }
                AddRecordScreen(
                    recordId = 0,
                    date = date,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "record/{recordId}?date={date}",
                arguments = listOf(
                    androidx.navigation.navArgument("recordId") { type = androidx.navigation.NavType.LongType },
                    androidx.navigation.navArgument("date") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val recordId = entry.arguments?.getLong("recordId") ?: 0L
                val date = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }
                AddRecordScreen(
                    recordId = recordId,
                    date = date,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
