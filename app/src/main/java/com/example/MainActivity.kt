package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ElectricMeter
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ApplianceEntity
import com.example.data.local.TariffPreferencesRepository
import com.example.data.local.WattWiseDatabase
import com.example.data.repository.EnergyRepository
import com.example.ui.components.ApplianceEditorSheet
import com.example.ui.components.BillReadingDialog
import com.example.ui.screens.AppliancesScreen
import com.example.ui.screens.OptimizerScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.TariffAndBillsScreen
import com.example.ui.theme.WattWiseTheme
import com.example.ui.viewmodel.WattWiseViewModel

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_APPLIANCES = "appliances"
const val ROUTE_OPTIMIZER = "optimizer"
const val ROUTE_TARIFF_BILLS = "tariff_bills"

private data class NavDestinationItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val NAV_ITEMS =
  listOf(
    NavDestinationItem(
      route = ROUTE_OVERVIEW,
      labelRes = R.string.nav_overview,
      selectedIcon = Icons.Filled.ElectricMeter,
      unselectedIcon = Icons.Outlined.ElectricMeter,
      testTag = "nav_overview",
    ),
    NavDestinationItem(
      route = ROUTE_APPLIANCES,
      labelRes = R.string.nav_appliances,
      selectedIcon = Icons.Filled.Bolt,
      unselectedIcon = Icons.Outlined.Bolt,
      testTag = "nav_appliances",
    ),
    NavDestinationItem(
      route = ROUTE_OPTIMIZER,
      labelRes = R.string.nav_optimizer,
      selectedIcon = Icons.Filled.AutoAwesome,
      unselectedIcon = Icons.Outlined.AutoAwesome,
      testTag = "nav_optimizer",
    ),
    NavDestinationItem(
      route = ROUTE_TARIFF_BILLS,
      labelRes = R.string.nav_tariff_bills,
      selectedIcon = Icons.Filled.ReceiptLong,
      unselectedIcon = Icons.Outlined.ReceiptLong,
      testTag = "nav_tariff_bills",
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { WattWiseTheme { WattWiseApp() } }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WattWiseApp() {
  val context = LocalContext.current.applicationContext
  val viewModel: WattWiseViewModel =
    viewModel(
      factory =
        remember(context) {
          val db = WattWiseDatabase.getInstance(context)
          val prefs = TariffPreferencesRepository(context)
          val repo = EnergyRepository(db.energyDao(), prefs)
          WattWiseViewModel.Factory(repo)
        }
    )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_OVERVIEW
  val snackbarHostState = remember { SnackbarHostState() }

  var showApplianceSheet by remember { mutableStateOf(false) }
  var editingAppliance by remember { mutableStateOf<ApplianceEntity?>(null) }
  var showAddBillDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { msg -> snackbarHostState.showSnackbar(msg) }
  }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val currentTitle =
    when (currentRoute) {
      ROUTE_APPLIANCES -> stringResource(R.string.nav_appliances)
      ROUTE_OPTIMIZER -> stringResource(R.string.nav_optimizer)
      ROUTE_TARIFF_BILLS -> stringResource(R.string.nav_tariff_bills)
      else -> stringResource(R.string.app_name)
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = { Text(text = currentTitle, style = MaterialTheme.typography.titleLarge) },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar {
            NAV_ITEMS.forEach { item ->
              val selected = currentRoute == item.route
              val label = stringResource(item.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            NAV_ITEMS.forEach { item ->
              val selected = currentRoute == item.route
              val label = stringResource(item.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          NavHost(navController = navController, startDestination = ROUTE_OVERVIEW) {
            composable(ROUTE_OVERVIEW) {
              OverviewScreen(
                uiState = uiState,
                onAddApplianceClick = {
                  editingAppliance = null
                  showApplianceSheet = true
                },
                onLoadDemoClick = { viewModel.loadDemoHousehold() },
                onApplyRecommendation = { action ->
                  viewModel.applyOptimization(action, uiState.tariffConfig.currencySymbol)
                },
                onApplyAllClick = {
                  viewModel.applyAllRecommendations(uiState.report.recommendations)
                },
                onOpenOptimizerTab = { navigateToTab(ROUTE_OPTIMIZER) },
              )
            }
            composable(ROUTE_APPLIANCES) {
              AppliancesScreen(
                uiState = uiState,
                onSearchChange = viewModel::setSearchQuery,
                onCategorySelect = viewModel::setSelectedCategory,
                onAddAppliance = {
                  editingAppliance = null
                  showApplianceSheet = true
                },
                onEditAppliance = { app ->
                  editingAppliance = app
                  showApplianceSheet = true
                },
                onToggleAppliance = viewModel::toggleAppliance,
                onDeleteAppliance = viewModel::deleteAppliance,
                onLoadDemo = viewModel::loadDemoHousehold,
              )
            }
            composable(ROUTE_OPTIMIZER) {
              OptimizerScreen(
                uiState = uiState,
                onSimulationChange = viewModel::updateSimulationParams,
                onApplySimulationToSchedule = viewModel::applySimulationToSchedule,
                onApplyRecommendation = { action ->
                  viewModel.applyOptimization(action, uiState.tariffConfig.currencySymbol)
                },
                onApplyAllRecommendations = {
                  viewModel.applyAllRecommendations(uiState.report.recommendations)
                },
              )
            }
            composable(ROUTE_TARIFF_BILLS) {
              TariffAndBillsScreen(
                uiState = uiState,
                onSaveTariff = viewModel::saveTariffConfig,
                onOpenAddBillDialog = { showAddBillDialog = true },
                onDeleteBill = viewModel::deleteBillReading,
                onClearAllData = viewModel::clearAllData,
              )
            }
          }
        }
      }
    }
  }

  if (showApplianceSheet) {
    ApplianceEditorSheet(
      existing = editingAppliance,
      onDismiss = { showApplianceSheet = false },
      onSave = { entity ->
        viewModel.saveAppliance(entity)
        showApplianceSheet = false
      },
      onDelete =
        editingAppliance?.let { target ->
          {
            viewModel.deleteAppliance(target)
            showApplianceSheet = false
          }
        },
    )
  }

  if (showAddBillDialog) {
    BillReadingDialog(
      currencySymbol = uiState.tariffConfig.currencySymbol,
      onDismiss = { showAddBillDialog = false },
      onSave = { reading ->
        viewModel.addBillReading(reading)
        showAddBillDialog = false
      },
    )
  }
}
