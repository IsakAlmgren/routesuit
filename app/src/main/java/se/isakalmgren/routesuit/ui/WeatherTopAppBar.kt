package se.isakalmgren.routesuit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.isakalmgren.routesuit.R
import se.isakalmgren.routesuit.ui.theme.RouteSuitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTopAppBar(onSettingsClick: () -> Unit) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.content_description_app_icon),
                modifier = Modifier.Companion
                    .padding(start = 16.dp)
                    .size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.content_description_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Preview(showBackground = true, name = "Top App Bar")
@Composable
fun WeatherTopAppBarPreview() {
    RouteSuitTheme {
        Scaffold(
            topBar = {
                WeatherTopAppBar(onSettingsClick = {})
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Content area",
                    modifier = Modifier.Companion
                        .align(Alignment.Companion.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}