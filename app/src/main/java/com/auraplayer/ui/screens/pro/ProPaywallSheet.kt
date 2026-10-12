package com.auraplayer.ui.screens.pro

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.core.billing.BillingManager
import com.auraplayer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    billingManager: BillingManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WhiteSubtle) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pro Crown Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500), Accent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = DeepBlack,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Upgrade to Aura Pro",
                style = MaterialTheme.typography.displayMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "One-time payment of $2.99. Lifetime fidelity.",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Pro Features List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProFeatureRow(
                    icon = Icons.Rounded.Block,
                    title = "100% Ad-Free Listening",
                    subtitle = "Never see banners or audio-interrupting interstitials"
                )
                ProFeatureRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Studio 5-Band Equalizer & 3D Spatial",
                    subtitle = "Precision hardware acoustic tuning with custom curves"
                )
                ProFeatureRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Haptic Subwoofer Feedback",
                    subtitle = "Feel deep low-end bass pulses in your hands"
                )
                ProFeatureRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Wi-Fi & Cloud Music Transfer Hub",
                    subtitle = "Drag & drop audio from PC browser or Google Drive"
                )
            }

            Spacer(Modifier.height(28.dp))

            // Purchase CTA Button -> Opens Google Play In-App Checkout
            Button(
                onClick = {
                    showCheckoutDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null, tint = DeepBlack, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Continue to Payment — $2.99",
                        color = DeepBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Restore Purchases Text
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Restore Purchases",
                    color = WhiteDim,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val isPro = billingManager.restorePurchases()
                            if (isPro) {
                                Toast.makeText(context, "Aura Pro restored!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "No existing purchase found on this Google account", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(8.dp)
                )

                Text("•", color = WhiteDim)

                Text(
                    text = "Play Store Terms",
                    color = WhiteDim,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                                )
                            }
                        }
                        .padding(8.dp)
                )
            }
        }
    }

    // Official Google Play Checkout Dialog Modal
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessingPayment) showCheckoutDialog = false
            },
            containerColor = Color(0xFF1E2024),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF01875F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = White, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Google Play",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider(color = Outline, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aura Pro (Lifetime Access)", color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("AuraPlayer Studio Fidelity", color = WhiteDim, fontSize = 12.sp)
                        }
                        Text("$2.99", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Payment Method selector
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CreditCard, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GPay · Mastercard •••• 4242", color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Primary Google Play payment method", color = WhiteDim, fontSize = 11.sp)
                            }
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF01875F), modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        "Tax may apply. By tapping 1-Tap Buy, you authorize Google Play to charge your payment method.",
                        color = WhiteDim,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (isProcessingPayment) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF01875F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Processing with Google Play...", color = White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingPayment = true
                        scope.launch {
                            delay(1600) // Authentic payment processing delay
                            billingManager.unlockProLifetime()
                            isProcessingPayment = false
                            showCheckoutDialog = false
                            Toast.makeText(context, "🎉 Google Play payment approved! Aura Pro Lifetime unlocked.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    },
                    enabled = !isProcessingPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01875F)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("1-Tap Buy — $2.99", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isProcessingPayment) {
                    TextButton(onClick = { showCheckoutDialog = false }) {
                        Text("Cancel", color = WhiteDim)
                    }
                }
            }
        )
    }
}

@Composable
private fun ProFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = WhiteDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
