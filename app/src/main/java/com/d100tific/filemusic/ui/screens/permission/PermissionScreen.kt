package com.d100tific.filemusic.ui.screens.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.ui.components.Win2kTextButton
import com.d100tific.filemusic.ui.theme.LocalWinColors

@Composable
fun PermissionScreen(
    wasDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalWinColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))
            Text(
                text = stringResource(R.string.permission_title),
                color = colors.onPage,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.permission_message),
                color = colors.onPage,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            if (wasDenied) {
                Text(
                    text = stringResource(R.string.permission_denied_message),
                    color = colors.disabled,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.padding(top = 16.dp))
                Win2kTextButton(text = stringResource(R.string.action_open_settings), onClick = onOpenSettings)
            } else {
                Win2kTextButton(text = stringResource(R.string.action_grant_access), onClick = onRequestPermission)
            }
        }
    }
}
