package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlin.math.PI
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.ui.theme.*
import kotlin.math.sin

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    isRegisterMode: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onToggleMode: () -> Unit
) {
    val colors = LocalAppColors.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "particles"
    )
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.93f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
            .drawBehind { drawParticles(particleAngle) }
    ) {
        // Glow orbs — using Box+CircleShape as CMP-safe blur alternative
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(GoldPrimary.copy(alpha = 0.12f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(listOf(GreenSuccess.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()  // Prevents IME from covering content
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(88.dp))

            // ── Logo ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(GoldLight, GoldDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = (44).sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Finance LifeLine",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                Strings.uiLoginSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(44.dp))

            // ── Try Demo — prominent CTA for new users ────────────────────
            if (!isRegisterMode) {
                Button(
                    onClick  = { onLogin("demo", "demo") },
                    enabled  = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = GreenMedium,
                        contentColor   = Color(0xFF002010)
                    )
                ) {
                    Text(
                        "🎮  ${Strings.uiLoginTryDemo}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        Strings.uiLoginTryDemoSub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF002010).copy(alpha = 0.70f)
                    )
                }

                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier  = Modifier.weight(1f),
                        thickness = 1.dp,
                        color     = colors.textHint.copy(alpha = 0.40f)
                    )
                    Text(
                        "  ${Strings.uiLoginOr}  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textHint
                    )
                    HorizontalDivider(
                        modifier  = Modifier.weight(1f),
                        thickness = 1.dp,
                        color     = colors.textHint.copy(alpha = 0.40f)
                    )
                }
            }

            // ── Glass card ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    // Mode title
                    AnimatedContent(
                        targetState = isRegisterMode,
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() togetherWith
                                    slideOutVertically { it } + fadeOut()
                        },
                        label = "modeTitle"
                    ) { isReg ->
                        Text(
                            if (isReg) Strings.uiLoginTabRegister else Strings.uiLoginTabLogin,
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // Username
                    FinanceTextField(
                        value       = username,
                        onChange    = { username = it },
                        label       = Strings.uiLoginFieldUsername,
                        leadingIcon = { Text("👤", fontSize = 20.sp) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    // Password
                    FinanceTextField(
                        value       = password,
                        onChange    = { password = it },
                        label       = Strings.uiLoginFieldPassword,
                        leadingIcon = { Text("🔒", fontSize = 20.sp) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "🙈" else "👁️",
                                    fontSize = 20.sp
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (isRegisterMode) onRegister(username, password)
                                else onLogin(username, password)
                            }
                        )
                    )

                    // Error banner
                    AnimatedVisibility(
                        visible = error != null,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        error?.let {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RedDanger.copy(alpha = 0.13f))
                                    .border(1.dp, RedDanger.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RedLight
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Primary button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (isRegisterMode) onRegister(username, password)
                            else onLogin(username, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled  = !isLoading,
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor   = colors.backgroundDeep
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                color       = colors.backgroundDeep,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isRegisterMode) Strings.uiLoginBtnRegister else Strings.uiLoginBtnLogin,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        }
                    }

                    // Demo hint removed — replaced by prominent "Try Demo" button above the card
                }
            }

            Spacer(Modifier.height(18.dp))

            TextButton(onClick = onToggleMode) {
                Text(
                    if (isRegisterMode) Strings.uiLoginAlreadyHaveAccount
                    else Strings.uiLoginNoAccount,
                    color = GoldLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FinanceTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value                = value,
        onValueChange        = onChange,
        label                = { Text(label, color = colors.textSecondary) },
        leadingIcon          = leadingIcon,
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        keyboardActions      = keyboardActions,
        modifier             = Modifier.fillMaxWidth(),
        singleLine           = true,
        shape                = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = GoldPrimary,
            unfocusedBorderColor = colors.textHint,
            focusedTextColor     = colors.textPrimary,
            unfocusedTextColor   = colors.textPrimary,
            cursorColor          = GoldPrimary,
            focusedContainerColor   = colors.backgroundCard,
            unfocusedContainerColor = colors.backgroundCard
        )
    )
}

private fun DrawScope.drawParticles(angle: Float) {
    val r = size.minDimension * 0.55f
    for (i in 0 until 22) {
        val a = (angle + i * (360f / 22f)) * (PI.toFloat() / 180f)
        val ri = r * (0.25f + 0.75f * ((i % 5) / 5f))
        val x = center.x + ri * sin(a.toDouble()).toFloat() * 0.6f
        val y = center.y + ri * sin((a + 1.2f).toDouble()).toFloat()
        drawCircle(
            color  = GoldPrimary.copy(alpha = 0.025f + (i % 4) * 0.007f),
            radius = 3f + (i % 3) * 2.5f,
            center = Offset(x, y)
        )
    }
}
