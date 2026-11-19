package app.dabbulu

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dabbulu.quickadd.QuickAddSheet
import app.dabbulu.quickadd.QuickAddViewModel
import app.dabbulu.ui.art.Glyph
import app.dabbulu.ui.art.LineIcon
import app.dabbulu.ui.components.Press3D
import app.dabbulu.ui.components.SheetOverlay
import app.dabbulu.ui.form.ExpenseForm
import app.dabbulu.ui.form.FormActions
import app.dabbulu.ui.history.HistoryScreen
import app.dabbulu.ui.home.HomeScreen
import app.dabbulu.ui.settings.BudgetSheet
import app.dabbulu.ui.settings.SettingsScreen
import app.dabbulu.ui.theme.DabbuluTheme
import app.dabbulu.ui.theme.Palette
import app.dabbulu.ui.theme.Type
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

enum class Tab { Home, History }

@Composable
fun MainScreen(vm: MainViewModel, quick: QuickAddViewModel, versionName: String) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var budgetOpen by remember { mutableStateOf(false) }

    val home by vm.home.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val edit by vm.editUi.collectAsStateWithLifecycle()
    val budget by vm.monthlyBudget.collectAsStateWithLifecycle()
    val quickUi by quick.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is MainEvent.Deleted -> {
                    val result = snackbar.showSnackbar(ev.label, actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) vm.restore(ev.expense)
                }
            }
        }
    }

    val overlay = adding || edit != null || budgetOpen
    val blur by animateDpAsState(if (overlay) 14.dp else 0.dp, label = "blur")

    BackHandler(enabled = settingsOpen && !overlay) { settingsOpen = false }
    BackHandler(enabled = !settingsOpen && tab == Tab.History && !overlay) { tab = Tab.Home }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Box(Modifier.fillMaxSize().blur(blur)) {
            val pad = PaddingValues(start = 18.dp, end = 18.dp, top = top + 8.dp, bottom = bottom + 120.dp)
            when {
                settingsOpen -> SettingsScreen(
                    monthlyBudget = budget,
                    versionName = versionName,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 8.dp, bottom = bottom + 24.dp),
                    onBack = { settingsOpen = false },
                    onEditBudget = { budgetOpen = true },
                )
                tab == Tab.Home -> home?.let {
                    HomeScreen(
                        ui = it,
                        contentPadding = pad,
                        onOpenSettings = { settingsOpen = true },
                        onSetBudget = { budgetOpen = true },
                        onSeeAll = { tab = Tab.History },
                        onEdit = vm::startEdit,
                    )
                }
                else -> history?.let {
                    HistoryScreen(it, pad, onQuery = vm::setQuery, onFilter = vm::setFilter, onEdit = vm::startEdit)
                }
            }
            if (!settingsOpen) {
                BottomBar(
                    tab = tab,
                    onTab = { tab = it },
                    onAdd = { quick.reset(); adding = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
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
                shape = RoundedCornerShape(18.dp),
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
        if (budgetOpen) {
            BudgetSheet(current = budget, today = LocalDate.now(), onSave = vm::setBudget, onDismissed = { budgetOpen = false })
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Bg.copy(alpha = 0.94f), Palette.Bg)))
            .navigationBarsPadding()
            .padding(top = 22.dp, bottom = 10.dp),
    ) {
        val bar = RoundedCornerShape(32.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(64.dp)
                .clip(bar)
                .background(Brush.verticalGradient(listOf(Color(0xFF22242C), Color(0xFF17181E))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))), bar),
        ) {
            NavItem(Glyph.Home, "Home", tab == Tab.Home, Modifier.weight(1f)) { onTab(Tab.Home) }
            Spacer(Modifier.width(88.dp))
            NavItem(Glyph.History, "History", tab == Tab.History, Modifier.weight(1f)) { onTab(Tab.History) }
        }
        Press3D(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
                .width(68.dp),
            height = 68.dp,
            edge = 5.dp,
            shape = CircleShape,
            top = Brush.verticalGradient(listOf(lerp(Palette.Accent, Color.White, 0.3f), Palette.Accent)),
            edgeColor = Palette.AccentEdge,
            glow = Palette.Accent,
            description = "Add expense",
        ) {
            LineIcon(Glyph.Plus, Palette.OnAccent, size = 30.dp, strokeWidth = 3f)
        }
    }
}

@Composable
private fun NavItem(glyph: Glyph, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) Palette.Accent else Palette.TextDim
    Column(
        modifier
            .fillMaxSize()
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LineIcon(glyph, color, size = 22.dp)
        Spacer(Modifier.height(3.dp))
        Text(label, style = Type.Tiny, color = color)
    }
}
