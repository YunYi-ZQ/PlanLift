package app.planlift.fit.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.data.settings.FontStyle
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.common.SectionCard

private fun familyOf(style: FontStyle): FontFamily = when (style) {
    FontStyle.SYSTEM -> FontFamily.Default
    FontStyle.SERIF -> FontFamily.Serif
    FontStyle.MONOSPACE -> FontFamily.Monospace
    FontStyle.CURSIVE -> FontFamily.Cursive
}

private fun labelOf(style: FontStyle): Int = when (style) {
    FontStyle.SYSTEM -> R.string.font_system
    FontStyle.SERIF -> R.string.font_serif
    FontStyle.MONOSPACE -> R.string.font_mono
    FontStyle.CURSIVE -> R.string.font_cursive
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by ThemePrefs.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_font_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ " + stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.font_page_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FontStyle.entries.forEach { style ->
                val selected = s.font == style
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ThemePrefs.update(ctx, s.copy(font = style)) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(labelOf(style)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = familyOf(style)
                            )
                            if (selected) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.font_preview_text),
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = familyOf(style)
                        )
                        Text(
                            "PlanLift 0123456789 kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = familyOf(style),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionCard {
                Text(
                    stringResource(R.string.font_note_title),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.font_note_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
