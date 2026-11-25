package com.lucrometro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lucrometro.data.AssetType

/** A ticker code (or a type's name in "By type") with its type icon right after it, when the type is known. */
@Composable
fun TickerCode(text: String, type: AssetType?, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        // Not filled: a long code at a large font wraps and leaves the icon its room.
        Text(text, style = style, modifier = Modifier.weight(1f, fill = false))
        type?.let { AssetTypeIcon(it) }
    }
}
