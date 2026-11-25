package com.lucrometro.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.AssetType

/** The word for one instrument of [type]: "ETF" / "Stock". */
val AssetType.label: Int
    get() = when (this) {
        AssetType.ETF -> R.string.asset_etf
        AssetType.STOCK -> R.string.asset_stock
    }

/** The 16dp asset type icon (decision 0057). [described] false where the word is written next to it. */
@Composable
fun AssetTypeIcon(type: AssetType, modifier: Modifier = Modifier, described: Boolean = true) {
    Icon(
        painterResource(if (type == AssetType.ETF) R.drawable.ic_pie_chart else R.drawable.ic_business),
        contentDescription = if (described) stringResource(type.label) else null,
        modifier = modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
