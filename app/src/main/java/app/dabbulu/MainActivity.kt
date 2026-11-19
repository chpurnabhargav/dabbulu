package app.dabbulu

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dabbulu.domain.Category
import app.dabbulu.domain.Money
import app.dabbulu.quickadd.QuickAddSheet
import app.dabbulu.quickadd.QuickAddViewModel
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Press3D
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.components.Sheen
import app.dabbulu.ui.form.ExpenseForm
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.history.HistoryScreen
import app.dabbulu.ui.home.HomeScreen
import app.dabbulu.ui.insights.InsightsScreen
import app.dabbulu.ui.settings.AmountSheet
import app.dabbulu.ui.settings.CategoryBudgetsScreen
import app.dabbulu.ui.settings.SettingsScreen
import app.dabbulu.ui.settings.StartDaySheet
import app.dabbulu.ui.theme.DabbuluTheme
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels { MainViewModel.Factory }
    private val quick: QuickAddViewModel by viewModels { QuickAddViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        setContent {
            DabbuluTheme {
                MainScreen(vm, quick, version)
            }
        }
    }
}

enum class Tab(val label: String, val glyph: Glyph) {
    Home("Home", Glyph.Home),
    Insights("Insights", Glyph.Chart),
    History("History", Glyph.History),
    Settings("Settings", Glyph.Sliders),
}

private sealed interface Sheet {
    data object Budget : Sheet
    data object Income : Sheet
    data object StartDay : Sheet
    data class CategoryBudget(val category: Category) : Sheet
}

@Composable
fun MainScreen(vm: MainViewModel, quick: QuickAddViewModel, versionName: String) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var categoryBudgetsOpen by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    val home by vm.home.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val edit by vm.editUi.collectAsStateWithLifecycle()
    val quickUi by quick.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is MainEvent.Deleted -> {
                    val result = snackbar.showSnackbar(ev.label, actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) vm.restore(ev.expense)
                }
                is MainEvent.Message -> snackbar.showSnackbar(ev.text, duration = SnackbarDuration.Short)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val (csv, count) = vm.csv()
                val ok = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } }.isSuccess
                }
                vm.message(if (ok) "Exported $count expenses" else "Couldn't save the file")
            }
        }
    }

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.setReminder(true)
            Reminders.schedule(context, true)
            vm.message("Reminder set for 9 PM every day")
        } else {
            vm.message("Allow notifications in Android settings to get the reminder")
        }
    }
    val setReminder: (Boolean) -> Unit = { on ->
        if (on && !Reminders.canNotify(context) && Build.VERSION.SDK_INT >= 33) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setReminder(on)
            Reminders.schedule(context, on)
            if (on) vm.message("Reminder set for 9 PM every day")
        }
    }

    val overlay = adding || edit != null || sheet != null
    val blur by animateDpAsState(if (overlay) 12.dp else 0.dp, label = "blur")

    BackHandler(enabled = categoryBudgetsOpen && !overlay) { categoryBudgetsOpen = false }
    BackHandler(enabled = !categoryBudgetsOpen && tab != Tab.Home && !overlay) { tab = Tab.Home }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pad = PaddingValues(start = 16.dp, end = 16.dp, top = top + 8.dp, bottom = bottom + 120.dp)
    val today = LocalDate.now()

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Box(Modifier.fillMaxSize().blur(blur)) {
            when (tab) {
                Tab.Home -> home?.let {
                    HomeScreen(
                        ui = it,
                        contentPadding = pad,
                        onSetBudget = { sheet = Sheet.Budget },
                        onSeeAll = { tab = Tab.History },
                        onInsights = { tab = Tab.Insights },
                        onEdit = vm::startEdit,
                    )
                }
                Tab.Insights -> insights?.let { ui ->
                    InsightsScreen(
                        ui = ui,
                        contentPadding = pad,
                        onKind = { k -> vm.setInsightsPeriod(vm.periodOfKind(ui.data.period, k, ui.today, ui.data.period.monthStartDay)) },
                        onStep = vm::setInsightsPeriod,
                        onEdit = vm::startEdit,
                    )
                }
                Tab.History -> history?.let { ui ->
                    HistoryScreen(
                        ui = ui,
                        contentPadding = pad,
                        onQuery = vm::setQuery,
                        onFilter = vm::setFilter,
                        onPeriod = vm::setHistoryPeriod,
                        onKind = { k -> vm.setHistoryPeriod(vm.periodOfKind(ui.period, k, ui.today, ui.monthStartDay)) },
                        onEdit = vm::startEdit,
                    )
                }
                Tab.Settings -> settings?.let { ui ->
                    if (categoryBudgetsOpen) {
                        CategoryBudgetsScreen(ui, pad, onBack = { categoryBudgetsOpen = false }, onEdit = { sheet = Sheet.CategoryBudget(it) })
                    } else {
                        SettingsScreen(
                            ui = ui,
                            versionName = versionName,
                            contentPadding = pad,
                            onBudget = { sheet = Sheet.Budget },
                            onStartDay = { sheet = Sheet.StartDay },
                            onCategoryBudgets = { categoryBudgetsOpen = true },
                            onIncome = { sheet = Sheet.Income },
                            onReminder = setReminder,
                            onExport = { exportLauncher.launch("dabbulu-expenses-$today.csv") },
                        )
                    }
                }
            }
            BottomBar(
                tab = tab,
                onTab = { tab = it; if (it != Tab.Settings) categoryBudgetsOpen = false },
                onAdd = { quick.reset(); adding = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
        ) { data ->
            Snackbar(
                data,
                containerColor = Palette.CardHi,
                contentColor = Palette.Text,
                actionColor = Palette.Accent,
                shape = RoundedCornerShape(16.dp),
            )
        }

        if (adding) {
            QuickAddSheet(quickUi, quick.actions, onUndo = quick::undo, onDismissed = { adding = false })
        }
        edit?.let { ui ->
            SheetOverlay(onDismissed = vm::endEdit) { close ->
                val actions = remember(close) {
                    object : FormActions by vm.editActions {
                        override fun submit() {
                            if (vm.canSaveEdit()) {
                                vm.editActions.submit()
                                close()
                            }
                        }
                    }
                }
                ExpenseForm(ui, actions, onDelete = { vm.deleteEditing(); close() })
            }
        }
        val s = settings?.settings
        when (val sh = sheet) {
            Sheet.Budget -> AmountSheet(
                title = "Monthly budget",
                subtitle = "How much do you want to spend in a month?",
                current = s?.monthlyBudget ?: 0L,
                onSave = { vm.setBudget(it) },
                onDismissed = { sheet = null },
                removeLabel = "Remove budget",
                hint = { if (it > 0) "About ${Money.format(it / 30 / 100 * 100)} a day" else "" },
            )
            Sheet.Income -> AmountSheet(
                title = "Monthly income",
                subtitle = "Your take-home pay. Used to show how much you save.",
                current = s?.monthlyIncome ?: 0L,
                onSave = { vm.setIncome(it) },
                onDismissed = { sheet = null },
                removeLabel = "Remove income",
            )
            Sheet.StartDay -> StartDaySheet(current = s?.monthStartDay ?: 1, onSave = { vm.setStartDay(it) }, onDismissed = { sheet = null })
            is Sheet.CategoryBudget -> AmountSheet(
                title = sh.category.name,
                subtitle = "Monthly limit for this category",
                current = sh.category.budget ?: 0L,
                onSave = { vm.setCategoryBudget(sh.category.id, it) },
                onDismissed = { sheet = null },
                removeLabel = "Remove limit",
            )
            null -> Unit
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Bg.copy(alpha = 0.95f), Palette.Bg)))
            .navigationBarsPadding()
            .padding(top = 22.dp, bottom = 10.dp),
    ) {
        val bar = RoundedCornerShape(26.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(64.dp)
                .shadow(16.dp, bar, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(bar)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1C22), Color(0xFF141519))))
                .border(1.dp, Sheen, bar),
        ) {
            NavItem(Tab.Home, tab, Modifier.weight(1f), onTab)
            NavItem(Tab.Insights, tab, Modifier.weight(1f), onTab)
            Spacer(Modifier.width(76.dp))
            NavItem(Tab.History, tab, Modifier.weight(1f), onTab)
            NavItem(Tab.Settings, tab, Modifier.weight(1f), onTab)
        }
        Press3D(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .width(62.dp),
            height = 62.dp,
            edge = 4.dp,
            shape = CircleShape,
            top = Brush.verticalGradient(listOf(lerp(Palette.Accent, Color.White, 0.22f), Palette.Accent)),
            edgeColor = Palette.AccentEdge,
            glow = Palette.Accent.copy(alpha = 0.7f),
            description = "Add expense",
        ) {
            LineIcon(Glyph.Plus, Palette.OnAccent, size = 28.dp, strokeWidth = 2.8f)
        }
    }
}

@Composable
private fun NavItem(item: Tab, current: Tab, modifier: Modifier, onTab: (Tab) -> Unit) {
    val selected = item == current
    val color = if (selected) Palette.Accent else Palette.TextDim
    Column(
        modifier
            .fillMaxSize()
            .clickable(role = Role.Tab) { onTab(item) }
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LineIcon(item.glyph, color, size = 21.dp)
        Spacer(Modifier.height(4.dp))
        Text(item.label, style = Type.Tiny, color = color)
    }
}
