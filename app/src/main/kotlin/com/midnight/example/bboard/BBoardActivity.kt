package com.midnight.example.bboard

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import com.midnight.kuira.core.compact.ContractCallStage
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.midnight.kuira.dapp.ContractCallProgressBar
import com.midnight.kuira.dapp.PanelBar
import com.midnight.kuira.dapp.dappPressable
import androidx.activity.result.contract.ActivityResultContracts
import com.midnight.kuira.dapp.wallet.WalletAppShell
import com.midnight.kuira.sdk.walletruntime.WalletNotifications
import com.midnight.kuira.dapp.sigil.SigilStatus
import com.midnight.kuira.sdk.walletruntime.SessionLock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ── Design Tokens ──

// bboard is the HOST demo app — its job is to show the neutral, monochrome Kuira
// pills dropping into a strongly-branded host. The brand here is a Renaissance
// proclamation scroll: aged parchment + period pigments (iron-gall ink, vermilion
// rubric red, lapis ultramarine, burnished gold). The value NAMES are kept so the
// whole screen re-themes from this one block; only the meanings changed.
private object Colors {
    // Parchment surfaces.
    val Background = Color(0xFFEAD9B0)       // aged vellum
    val BackgroundDeep = Color(0xFFD3BC8A)   // shaded edge of the sheet
    val Surface = Color(0xFFF1E6C6)          // lit parchment plaque
    val ErrorSurface = Color(0xFFE9D3BE)     // faintly scorched parchment

    // Period pigments, keyed to meaning across the screens.
    val Electric = Color(0xFF27406E)         // ultramarine (lapis) — primary / links
    val Magenta = Color(0xFFA82815)          // vermilion — the posted notice
    val Violet = Color(0xFFB0892E)           // burnished gold — vacant / drafting
    val Teal = Color(0xFF3E5C4B)             // verdigris — addresses / on-chain
    val Amber = Color(0xFFB0892E)            // gold — identity / attention

    val Accent = Electric
    val Success = Color(0xFF3E5C4B)          // verdigris green
    val Error = Color(0xFF7E1C12)            // deep oxblood

    val OnSurface = Color(0xFF3A2A17)        // iron-gall ink — primary text
    val OnSurfaceDim = Color(0xFF5C4A30)     // soft ink — secondary
    val OnSurfaceSubtle = Color(0xFF8A7148)  // faint ink — tertiary / hints
    val Disabled = Color(0x1A3A2A17)         // faint ink wash
    val OnBright = Color(0xFFF3E7C8)         // parchment-light lettering on a vermilion plaque
}

private object Gradients {
    val Brand = Brush.linearGradient(listOf(Color(0xFFA82815), Color(0xFF7E1C12)))            // vermilion seal
    val Aurora = Brush.linearGradient(listOf(Colors.Violet, Colors.Magenta, Colors.Electric)) // gilt → rubric → lapis
    val Screen = Brush.verticalGradient(listOf(Colors.Surface, Colors.Background, Colors.BackgroundDeep)) // lit vellum
    val Note = Brush.linearGradient(listOf(Color(0xFFF6EAC8), Color(0xFFE6D2A6)))             // parchment notice
}

// Period typefaces, bundled (both SIL Open Font License):
//  · EB Garamond — a digitization of Claude Garamond's 1592 Berner specimen; the body hand.
//  · Cinzel — Roman inscriptional capitals; the display/heading hand.
// Variable fonts: the weight axis is driven from each Font's `weight` argument.
private val Garamond = FontFamily(
    Font(R.font.eb_garamond, FontWeight.Normal),
    Font(R.font.eb_garamond, FontWeight.Medium),
    Font(R.font.eb_garamond, FontWeight.SemiBold),
    Font(R.font.eb_garamond, FontWeight.Bold),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.eb_garamond_italic, FontWeight.Medium, FontStyle.Italic),
)
private val Cinzel = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel, FontWeight.SemiBold),
    Font(R.font.cinzel, FontWeight.Bold),
)

private object Type {
    val Title = 24.sp
    val Subtitle = 14.sp
    val Body = 14.sp
    val Label = 13.sp
    val Caption = 12.sp
    val Mono = 11.sp // monospace addresses — smallest allowed
}

private object Spacing {
    val ScreenPadding = 24.dp
    val CardPadding = 20.dp
    val SectionGap = 20.dp
    val ItemGap = 12.dp
    val SmallGap = 8.dp
    val TinyGap = 4.dp
}

private object Shapes {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(10.dp)
}

private const val BUTTON_HEIGHT_DP = 48
private const val CHIP_HEIGHT_DP = 40
private const val PROGRESS_BAR_HEIGHT_DP = 3
private const val SPINNER_SIZE_DP = 24

// ── Activity ──

@AndroidEntryPoint
class BBoardActivity : FragmentActivity() {
    /** Session auto-lock (#14) — onUserInteraction resets its foreground idle timer. */
    @Inject lateinit var sessionLock: SessionLock

    // POST_NOTIFICATIONS for the dust-sync Live Update (#235). Best-effort: if the
    // user denies, the background sync still runs — just without a visible notification.
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (WalletNotifications.shouldRequest(this)) {
            notifPermission.launch(WalletNotifications.PERMISSION)
        }
        setContent { WalletAppShell { BBoardApp() } }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionLock.onUserActivity()
    }
}

@Composable
fun BBoardApp(viewModel: BBoardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // The wallet panel (in PanelBar) hosts the biometric prompt now — BBoard's
    // connect/deploy are followers that wait on the shared SDK, so this screen
    // no longer needs a FragmentActivity handle.

    // The wallet panel is the network authority: its chip drives the SDK's
    // durable NetworkPreferenceStore, and BBoard's contract ops read the active
    // network straight off the shared SDK. No host-side mirror (that was a second
    // source of truth that drifted) — the store survives a kill on its own.

    // Mirror of the sigil panel's status so BBoard can gate the onboarding
    // banner. Panel emits None on first composition until it loads any
    // persisted sigil + then transitions to Forged. The banner only renders
    // while status is None (or has flipped to Error during a forge attempt)
    // — once Forged the user has all the affordances they need in the chip.
    //
    // Plain `remember` (not `rememberSaveable`) because [SigilStatus] is a
    // sealed class with non-parcelable variants; the panel re-emits on
    // recomposition / process restore anyway, so this is fine.
    var sigilStatus: SigilStatus by remember { mutableStateOf<SigilStatus>(SigilStatus.None) }

    // #285: the SDK's durable NetworkPreferenceStore is the single source of
    // truth for the active network — it survives process death, so BBoard seeds
    // the panel from it and writes back through it (no host-side prefs mirror).
    val selectedNetwork by viewModel.selectedNetwork.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Gradients.Screen)) {
        // Garamond is the default hand for everything below — INCLUDING the Kuira
        // pills in PanelBar, which inherit LocalTextStyle (the SDK pins only its
        // structural monospace fields, e.g. the DID/address). Providing LocalTextStyle
        // here is how a host themes the pill's typography; its colour is themed
        // separately via WalletPanelColors.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Garamond),
        ) {
        // Content fills the screen; the Kuira chips float OVER it as draggable widgets
        // (PanelBar(floating = true), placed last so it overlays). The content keeps clear of
        // the status bar itself now that the fixed top bar is gone.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = Spacing.SectionGap,
                    start = Spacing.ScreenPadding,
                    end = Spacing.ScreenPadding,
                    bottom = Spacing.ScreenPadding,
                )
        ) {
                Text(
                    "BBOARD",
                    style = TextStyle(brush = animatedAuroraBrush(), fontFamily = Cinzel),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                )
                Text(
                    "Tabula Nuntiorum · a midnight bulletin board",
                    color = Colors.OnSurfaceDim,
                    fontStyle = FontStyle.Italic,
                    fontSize = Type.Caption,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(Spacing.ItemGap))
                OrnamentRule()
                Spacer(modifier = Modifier.height(Spacing.SectionGap))

                when (val s = state) {
                is BBoardState.Setup -> SetupScreen(
                    sigilStatus = sigilStatus,
                    onConnectSdk = { addr -> viewModel.connectWithSdk(addr) },
                    onDeploySdk = { viewModel.deployAndConnect() },
                )
                is BBoardState.Connecting -> ConnectingView(s.stage)
                is BBoardState.Error -> ErrorView(s.message) { viewModel.disconnect() }
                    is BBoardState.Connected -> ConnectedScreen(
                        state = s,
                        onPost = viewModel::post,
                        onTakeDown = viewModel::takeDown,
                        onRefresh = viewModel::refresh,
                        onDisconnect = viewModel::disconnect,
                    )
                }
            }
            // Kuira chips as draggable floaters (Phase 1) — overlays the content above.
            PanelBar(
                floating = true,
                network = selectedNetwork,
                onNetworkChange = { viewModel.selectNetwork(it) },
                onSigilStatusChange = { sigilStatus = it },
            )
        }
    }
}

// ── Setup Screen ──
//
// Stripped to two cards: sigil identity + contract connection. Network
// selection lives in WalletStatusPanel's sheet (anchored top-right by
// BBoardApp). Wallet status (balance / fund / register) is the panel too —
// the in-screen WalletStatusCard was redundant with that and was removed.
// Only one connection mode remains: standalone SDK. The old "Remote Wallet"
// (mn serve via WebSocket) path is gone — see commit log for rationale.

@Composable
private fun SetupScreen(
    sigilStatus: SigilStatus,
    onConnectSdk: (String) -> Unit,
    onDeploySdk: () -> Unit,
) {
    var address by remember { mutableStateOf("") }

    // ── Sigil onboarding nudge (only when no identity yet) ──
    //
    // The full identity flow (forge, backup, restore, test prf, DID display)
    // lives in `SigilStatusPanel`'s top sheet — tapped via the sigil chip in
    // the panel bar above. This banner is a one-time-ish onboarding cue: it
    // points the user at the chip when they don't have a sigil yet so they
    // don't try to deploy/connect a contract before they have an identity.
    //
    // Auto-hides once the user has forged (or restored) a sigil — the chip
    // is then the canonical entry point.
    if (sigilStatus is SigilStatus.None || sigilStatus is SigilStatus.Error) {
        DarkCard(accent = Colors.Amber) {
            Text("identity required", color = Colors.Amber, fontSize = Type.Caption, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(Spacing.SmallGap))
            Text(
                "You need a sigil before posting to the board. Tap the sigil chip in the top bar to forge one.",
                color = Colors.OnSurfaceDim,
                fontSize = Type.Caption,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.SectionGap))
    }

    // ── Contract Connection Card ──
    DarkCard(accent = Colors.Electric) {
        Text("join a board", color = Colors.Electric, fontSize = Type.Caption, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(Spacing.TinyGap))
        Text(
            "Paste the contract address someone shared to connect to their bulletin board.",
            color = Colors.OnSurfaceSubtle,
            fontSize = Type.Caption,
        )
        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it.trim() },
            label = { Text("contract address", color = Colors.OnSurfaceDim) },
            placeholder = { Text("paste a board's contract address", color = Colors.OnSurfaceSubtle) },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(Spacing.TinyGap))
        Text(
            "A contract address is 64 characters. The network comes from the wallet pill above.",
            color = Colors.OnSurfaceSubtle,
            fontSize = Type.Caption,
        )

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        // Gated on a forged sigil: without one, the wallet panel never builds
        // the shared SDK, so connect would wait forever (awaitSdk). The
        // "identity required" banner above explains why it's disabled.
        ActionButton("connect", enabled = address.length == 64 && sigilStatus is SigilStatus.Forged) {
            onConnectSdk(address)
        }

        Spacer(modifier = Modifier.height(Spacing.SmallGap))
        Text(
            "— or start a new board —",
            color = Colors.OnSurfaceSubtle,
            fontSize = Type.Caption,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.SmallGap))
        ActionButton("create a new board", enabled = sigilStatus is SigilStatus.Forged) {
            onDeploySdk()
        }
        Spacer(modifier = Modifier.height(Spacing.TinyGap))
        Text(
            "Deploys a fresh board on-chain and connects you to it — you'll get an address to share.",
            color = Colors.OnSurfaceSubtle,
            fontSize = Type.Caption,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


// ── Connected Screen ──

@Composable
private fun ConnectedScreen(
    state: BBoardState.Connected,
    onPost: (String) -> Unit,
    onTakeDown: () -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val isReady = state.dustSyncStatus is DustSyncStatus.Ready

    // Authorize panel removed in the post-A2 cleanup. Under PRF-derived
    // sigil + wallet, both keys are siblings of the same passkey — the
    // act of signing in via the sigil chip IS authorization. See the
    // commit log + Kicks wishlist #27 for the future delegated-access-
    // keys consumer that will revive a similar UI in a scoped context.

    DarkCard(accent = Colors.Teal) {
        // Contract address \u2014 labeled + tap-to-copy.
        //
        // Beginners land here right after connecting. Previously this was a
        // bare monospace hash next to "tap to copy" with no label \u2014 no way to
        // tell what it was. Now: a clear label, the hash as the card's primary
        // (brighter) content, an obvious copy affordance, and one line on what
        // it's for.
        Text("contract address", color = Colors.Teal, fontSize = Type.Caption, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(Spacing.TinyGap))

        val clipboardManager = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                clipboardManager.setText(AnnotatedString(state.contractAddress))
                copied = true
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.contractAddress,
                color = Colors.OnSurface,
                fontSize = Type.Mono,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (copied) "\u2713 copied" else "copy",
                color = if (copied) Colors.Success else Colors.Accent,
                fontSize = Type.Caption,
                modifier = Modifier.padding(start = Spacing.SmallGap),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.TinyGap))
        Text(
            "The on-chain board you're connected to. Share this address so others can join the same board.",
            color = Colors.OnSurfaceSubtle,
            fontSize = Type.Caption,
        )

        // Dust sync progress — inline, non-blocking. Exhaustive when over the
        // sealed status (smart-casts each branch; no redundant flag checks).
        when (val dust = state.dustSyncStatus) {
            is DustSyncStatus.Syncing -> {
                Spacer(modifier = Modifier.height(Spacing.ItemGap))
                SyncProgressBar(progress = dust.percent / 100f, label = "syncing dust: ${dust.percent}% — ${dust.detail}")
            }
            is DustSyncStatus.Processing -> {
                Spacer(modifier = Modifier.height(Spacing.ItemGap))
                SyncProgressBar(progress = null, label = dust.detail)
            }
            is DustSyncStatus.Ready -> {
                state.lastTimingMs?.let {
                    Spacer(modifier = Modifier.height(Spacing.TinyGap))
                    Text("last tx: ${it}ms", color = Colors.Success.copy(alpha = 0.7f), fontSize = Type.Caption)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.SectionGap))

    DarkCard(accent = Colors.Violet) {
        when (val board = state.boardState) {
            is BoardState.Vacant -> VacantBoard(onPost = onPost, isEnabled = isReady)
            is BoardState.Working -> WorkingBoard(board.stage)
            is BoardState.Occupied -> OccupiedBoard(board.message, onTakeDown, onRefresh, isEnabled = isReady)
            is BoardState.CallError -> CallErrorView(board.message, onRefresh)
        }
    }

    Spacer(modifier = Modifier.height(Spacing.SectionGap * 2))

    Box(
        modifier = Modifier.fillMaxWidth().height(CHIP_HEIGHT_DP.dp).clickable(onClick = onDisconnect),
        contentAlignment = Alignment.Center,
    ) {
        Text("disconnect", color = Colors.OnSurfaceSubtle, fontSize = Type.Caption)
    }
}

@Composable
private fun VacantBoard(onPost: (String) -> Unit, isEnabled: Boolean = true) {
    var message by remember { mutableStateOf("") }
    Text("board is vacant", color = Colors.Violet, fontSize = Type.Caption, letterSpacing = 2.sp)
    Spacer(modifier = Modifier.height(Spacing.TinyGap))
    Text(
        "No message posted yet. Write one below — it's stored on-chain for anyone on this board to see.",
        color = Colors.OnSurfaceSubtle,
        fontSize = Type.Caption,
    )
    Spacer(modifier = Modifier.height(Spacing.SectionGap))
    OutlinedTextField(
        value = message,
        onValueChange = { message = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("your message", color = Colors.OnSurfaceSubtle) },
        colors = textFieldColors(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(Spacing.SectionGap))
    ActionButton("post", enabled = message.isNotBlank() && isEnabled) { onPost(message) }
    if (!isEnabled) {
        SyncWaitingHint()
    }
}

@Composable
private fun WorkingBoard(stage: ContractCallStage?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Colors.Electric, strokeWidth = 2.dp, modifier = Modifier.size(SPINNER_SIZE_DP.dp))
        Spacer(modifier = Modifier.height(Spacing.ItemGap))
        if (stage == null) {
            // Brief pre-stage beat before the first ContractCallStage arrives.
            Text("Preparing…", color = Colors.OnSurfaceDim, fontSize = Type.Label)
        } else {
            // SDK drop-in: staged label + monotonic, eased progress bar, fed by
            // the live ContractCallStage from the contract call's onProgress.
            ContractCallProgressBar(
                stage = stage,
                accent = Colors.Accent,
                trackColor = Colors.Disabled,
                labelColor = Colors.OnSurfaceDim,
            )
        }
    }
}

@Composable
private fun OccupiedBoard(message: String, onTakeDown: () -> Unit, onRefresh: () -> Unit, isEnabled: Boolean = true) {
    Text("board is occupied", color = Colors.Magenta, fontSize = Type.Caption, letterSpacing = 2.sp)
    Spacer(modifier = Modifier.height(Spacing.ItemGap))
    PinnedNote(message)
    Spacer(modifier = Modifier.height(Spacing.SectionGap))
    ActionButton("take down", enabled = isEnabled, dimmed = true, onClick = onTakeDown)
    Spacer(modifier = Modifier.height(Spacing.SmallGap))
    ActionButton("refresh", enabled = true, dimmed = true, onClick = onRefresh)
    if (!isEnabled) {
        SyncWaitingHint()
    }
}

@Composable
private fun CallErrorView(message: String, onRetry: () -> Unit) {
    Text(message, color = Colors.Error, fontSize = Type.Label)
    Spacer(modifier = Modifier.height(Spacing.SectionGap))
    ActionButton("retry", enabled = true, dimmed = true, onClick = onRetry)
}


// ── Shared Components ──

@Composable
private fun ConnectingView(stage: String) {
    DarkCard(accent = Colors.Electric) {
        Column(Modifier.fillMaxWidth().padding(vertical = Spacing.ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Colors.Electric, strokeWidth = 2.dp, modifier = Modifier.size(SPINNER_SIZE_DP.dp))
            Spacer(modifier = Modifier.height(Spacing.ItemGap))
            Text(stage, color = Colors.OnSurfaceDim, fontSize = Type.Label)
        }
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit) {
    DarkCard(color = Colors.ErrorSurface, accent = Colors.Error) {
        Text(message, color = Colors.Error, fontSize = Type.Label)
        Spacer(modifier = Modifier.height(Spacing.SectionGap))
        ActionButton("back", enabled = true, dimmed = true, onClick = onBack)
    }
}

/** Inline progress bar with label — used for dust sync. */
@Composable
private fun SyncProgressBar(progress: Float?, label: String) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(PROGRESS_BAR_HEIGHT_DP.dp),
            color = Colors.Accent,
            trackColor = Colors.Disabled,
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(PROGRESS_BAR_HEIGHT_DP.dp),
            color = Colors.Accent,
            trackColor = Colors.Disabled,
        )
    }
    Spacer(modifier = Modifier.height(Spacing.TinyGap))
    Text(label, color = Colors.Accent.copy(alpha = 0.8f), fontSize = Type.Caption)
}

/** "waiting for dust sync..." hint shown below disabled buttons. */
@Composable
private fun SyncWaitingHint() {
    Spacer(modifier = Modifier.height(Spacing.TinyGap))
    Text("waiting for dust sync...", color = Colors.OnSurfaceDim, fontSize = Type.Caption)
}

/** Horizontal chip row for selection (mode, network). */
@Composable
private fun ChipRow(
    options: List<String>,
    selectedIndex: Int,
    accentSelected: Boolean = false,
    onSelect: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.SmallGap)) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CHIP_HEIGHT_DP.dp)
                    .clip(Shapes.Chip)
                    .background(
                        when {
                            isSelected && accentSelected -> Colors.Accent
                            isSelected -> Colors.OnSurface
                            else -> Colors.Disabled
                        }
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.Black else Colors.OnSurfaceDim,
                    fontSize = Type.Label,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun DarkCard(
    color: Color = Colors.Surface,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    // An inset parchment panel, ruled with a gold hairline (the section's hue if
    // given) — flat, like a panel inked onto the sheet rather than a floating card.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = Shapes.Card,
        border = BorderStroke(1.dp, (accent ?: Colors.Amber).copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.CardPadding)) { content() }
    }
}

/** A gilt manuscript divider: two gold rules flanking a small lozenge. */
@Composable
private fun OrnamentRule(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f).height(1.5.dp).background(Colors.Amber.copy(alpha = 0.7f)))
        Canvas(Modifier.size(9.dp)) {
            val s = size.minDimension
            val lozenge = Path().apply {
                moveTo(s / 2f, 0f); lineTo(s, s / 2f); lineTo(s / 2f, s); lineTo(0f, s / 2f); close()
            }
            drawPath(lozenge, Colors.Amber)
        }
        Box(Modifier.weight(1f).height(1.5.dp).background(Colors.Amber.copy(alpha = 0.7f)))
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    // The primary CTA wears the brand gradient; secondary/disabled stay flat.
    val primary = enabled && !dimmed
    val fg = when {
        !enabled -> Colors.OnSurfaceSubtle
        dimmed -> Colors.OnSurfaceDim
        else -> Colors.OnBright
    }
    val fill = when {
        primary -> Modifier.background(Gradients.Brand)
        dimmed -> Modifier.background(Colors.Surface).border(BorderStroke(1.dp, Colors.Amber.copy(alpha = 0.6f)), Shapes.Button)
        else -> Modifier.background(Colors.Disabled)
    }
    Box(
        modifier = modifier
            .height(BUTTON_HEIGHT_DP.dp)
            .dappPressable(shape = Shapes.Button, enabled = enabled, onClick = onClick)
            .then(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = fg,
            fontFamily = Cinzel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Colors.OnSurface,
    unfocusedTextColor = Colors.OnSurface,
    focusedBorderColor = Colors.Electric,
    unfocusedBorderColor = Colors.Disabled,
    cursorColor = Colors.Electric,
)

// ── Decorative / hero elements ──

/** A slow horizontal aurora sweep — gives the hero title a living shimmer on video. */
@Composable
private fun animatedAuroraBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "aurora")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift",
    )
    val span = 700f
    return Brush.linearGradient(
        colors = listOf(Colors.Violet, Colors.Electric, Colors.Teal, Colors.Magenta),
        start = Offset(shift * span, 0f),
        end = Offset(shift * span + span, span * 0.4f),
    )
}

/**
 * Ambient background: a few oversized, heavily-blurred gradient orbs that drift
 * on a slow cycle. Each radial gradient already fades to transparent, so even
 * where [blur] is a no-op (API < 31) the glow stays soft. Purely decorative —
 * drawn behind all content, never interactive.
 */
@Composable
private fun FloatingOrbs() {
    val transition = rememberInfiniteTransition(label = "orbs")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Orb(color = Colors.Violet, size = 360.dp, x = (-90).dp, y = (40 + drift * 70).dp)
        Orb(color = Colors.Electric, size = 300.dp, x = (210 - drift * 90).dp, y = (300 + drift * 40).dp)
        Orb(color = Colors.Magenta, size = 320.dp, x = (30 + drift * 70).dp, y = (600 - drift * 80).dp)
    }
}

@Composable
private fun Orb(color: Color, size: Dp, x: Dp, y: Dp) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .blur(70.dp)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent)),
                CircleShape,
            ),
    )
}

/**
 * The posted message, shown like a note pinned to a corkboard: a slightly tilted
 * gradient card held by a tack. This is the connected screen's hero moment.
 */
@Composable
private fun PinnedNote(message: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.SmallGap)
                .rotate(-1.5f)
                .clip(Shapes.Card)
                .background(Gradients.Note)
                .padding(horizontal = Spacing.CardPadding, vertical = Spacing.SectionGap),
        ) {
            Text(
                "POSTED",
                color = Colors.Magenta,
                fontSize = Type.Caption,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(Spacing.SmallGap))
            Text(
                message,
                color = Colors.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.W600,
                lineHeight = 28.sp,
            )
        }
        // The wax seal fixing the notice to the board.
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Colors.Magenta)
                .border(BorderStroke(3.dp, Colors.OnSurface.copy(alpha = 0.15f)), CircleShape),
        )
    }
}
