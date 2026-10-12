package com.auraplayer.ui.screens.pro

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    billingManager: BillingManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                    title = "Fluid Vector Waveform Art",
                    subtitle = "Reactive dynamic audio curves that pulse while playing"
                )
                ProFeatureRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Haptic Subwoofer & Studio EQ",
                    subtitle = "Feel deep low-end bass pulses in your hands"
                )
                ProFeatureRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Wi-Fi & Cloud Music Transfer",
                    subtitle = "Drag & drop your audio from PC or Google Drive"
                )
            }

            Spacer(Modifier.height(28.dp))

            // Purchase CTA Button
            Button(
                onClick = {
                    billingManager.unlockProLifetime()
                    Toast.makeText(context, "Welcome to Aura Pro! Ads removed.", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Unlock Lifetime Access — $2.99",
                    color = DeepBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Restore Purchases Text
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
                            Toast.makeText(context, "No existing purchase found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(8.dp)
            )
        }
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
