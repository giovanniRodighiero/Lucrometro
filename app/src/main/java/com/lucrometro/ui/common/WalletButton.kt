package com.lucrometro.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lucrometro.R

/**
 * The public/private switch in the top bars (decision 0042): an outlined wallet in public, a filled one on tertiary
 * container in private, so it never reads as the pressed auto-refresh button. One tap, no authentication.
 */
@Composable
fun WalletButton(private: Boolean, onToggle: (Boolean) -> Unit) {
    IconToggleButton(
        checked = private,
        onCheckedChange = onToggle,
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Icon(
            painterResource(if (private) R.drawable.ic_wallet_filled else R.drawable.ic_wallet),
            stringResource(R.string.private_view),
        )
    }
}
