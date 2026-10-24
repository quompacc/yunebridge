/*  Copyright (C) 2026 quompacc

    This file is part of YuneBridge, a fork of Gadgetbridge.

    YuneBridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    YuneBridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package io.github.quompacc.yunebridge.ui.overview

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.quompacc.yunebridge.ui.icons.YuneIcons
import io.github.quompacc.yunebridge.ui.theme.Yune
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCalendarActivity
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.widgets.GaugeSpec
import nodomain.freeyourgadget.gadgetbridge.widgets.StatusLine
import nodomain.freeyourgadget.gadgetbridge.widgets.TileModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Neue Gerätedaten lösen ein Neuladen aus – nur solange die Übersicht sichtbar ist. */
@Composable
private fun ReloadOnNewData(onNewData: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lbm = LocalBroadcastManager.getInstance(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = onNewData()
        }
        val filter = IntentFilter(GBApplication.ACTION_NEW_DATA)
        var registered = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!registered) {
                    lbm.registerReceiver(receiver, filter)
                    registered = true
                }

                Lifecycle.Event.ON_STOP -> if (registered) {
                    lbm.unregisterReceiver(receiver)
                    registered = false
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (registered) lbm.unregisterReceiver(receiver)
        }
    }
}

private fun openCharts(activity: FragmentActivity, device: GBDevice?, tab: String) {
    if (device == null) return
    val intent = Intent(activity, ActivityChartsActivity::class.java).putExtra(GBDevice.EXTRA_DEVICE, device)
    val index = ActivityChartsActivity.getChartsTabIndex(tab, device, activity)
    if (index >= 0) intent.putExtra(ActivityChartsActivity.EXTRA_FRAGMENT_ID, index)
    activity.startActivity(intent)
}

private val numberFormat: NumberFormat get() = NumberFormat.getIntegerInstance()

private fun hoursMinutes(minutes: Long): String = "%d:%02d".format(minutes / 60, minutes % 60)

@Composable
fun OverviewScreen(activity: FragmentActivity, viewModel: OverviewViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val day by viewModel.day.collectAsStateWithLifecycle()
    ReloadOnNewData(onNewData = viewModel::refresh)

    val calendar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val millis = result.data?.getLongExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, 0L) ?: 0L
        if (result.resultCode == Activity.RESULT_OK && millis > 0) {
            viewModel.selectDay(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }
    val c = Yune.colors

    LazyColumn(
        modifier = Modifier
            .background(c.background)
            .nestedScroll(rememberNestedScrollInteropConnection()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Header(
                day = day,
                onPrevious = viewModel::previousDay,
                onNext = viewModel::nextDay,
                onCalendar = {
                    val millis = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    calendar.launch(
                        Intent(activity, DashboardCalendarActivity::class.java)
                            .putExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, millis)
                    )
                },
            )
        }
        val d = data
        if (d != null) {
            item(key = "activity") { ActivityCard(d, onClick = { openCharts(activity, d.chartsDevice, "activity") }) }
            item(key = "health_title") {
                Text(
                    stringResource(R.string.yb_overview_health),
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.text,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            item(key = "health") { HealthGrid(d, activity) }
            item(key = "extras") { ExtrasSection(d.extras) }
        }
    }
}

@Composable
private fun Header(day: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onCalendar: () -> Unit) {
    val c = Yune.colors
    val today = day == LocalDate.now()
    val locale = Locale.getDefault()
    val dateText = day.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))
    val subtitle = if (today) stringResource(R.string.yb_today) + " · " + dateText else dateText
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp)) {
        Text(
            subtitle.uppercase(locale),
            style = MaterialTheme.typography.labelLarge,
            color = c.secondaryText,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.yb_overview_title),
                style = MaterialTheme.typography.displaySmall,
                color = c.text,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundButton(YuneIcons.ChevronLeft, stringResource(R.string.yb_previous_day), enabled = true, onClick = onPrevious)
                RoundButton(YuneIcons.Calendar, stringResource(R.string.yb_calendar), enabled = true, onClick = onCalendar)
                RoundButton(YuneIcons.ChevronRight, stringResource(R.string.yb_next_day), enabled = !today, onClick = onNext)
            }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Yune.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.card)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) c.tint else c.track, modifier = Modifier.size(22.dp))
    }
}

private class RingSpec(val label: String, val value: String, val goal: String, val fraction: Float, val color: Color, val ink: Color)

@Composable
private fun ActivityCard(d: OverviewData, onClick: () -> Unit) {
    val c = Yune.colors
    val rings = listOf(
        RingSpec(
            stringResource(R.string.yb_steps), numberFormat.format(d.steps), numberFormat.format(d.stepsGoal),
            d.steps.toFloat() / max(d.stepsGoal, 1), c.steps, c.stepsInk,
        ),
        RingSpec(
            stringResource(R.string.yb_active_time), d.activeMinutes.toString(), stringResource(R.string.yb_minutes_value, d.activeGoalMinutes),
            d.activeMinutes.toFloat() / max(d.activeGoalMinutes, 1), c.active, c.activeInk,
        ),
        RingSpec(
            stringResource(R.string.yb_distance), "%.2f".format(d.distanceMeters / 1000f), stringResource(R.string.yb_km_value, d.distanceGoalMeters / 1000f),
            d.distanceMeters / max(d.distanceGoalMeters, 1), c.distance, c.distanceInk,
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.yb_activity), style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.yb_daily_goals), style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Rings(rings, Modifier.size(148.dp))
            Spacer(Modifier.width(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rings.forEach { r ->
                    Column {
                        Text(r.label, style = MaterialTheme.typography.labelLarge, color = r.ink)
                        Text(
                            buildAnnotatedString {
                                append(r.value)
                                withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.secondaryText)) {
                                    append(" / " + r.goal)
                                }
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, lineHeight = 26.sp),
                            color = c.text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Rings(rings: List<RingSpec>, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.1125f
        rings.forEachIndexed { i, r ->
            val inset = stroke / 2 + i * stroke * 1.12f
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            drawArc(r.color.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * min(max(r.fraction, 0.012f), 1f)
            drawArc(r.color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun HealthGrid(d: OverviewData, activity: FragmentActivity) {
    val c = Yune.colors
    val noData = stringResource(R.string.yb_no_data)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                icon = YuneIcons.Heart, label = stringResource(R.string.yb_heart_rate), ink = c.heartInk,
                value = d.heartRate?.toString() ?: "–",
                unit = if (d.heartRate != null) stringResource(R.string.yb_bpm) else noData,
                modifier = Modifier.weight(1f),
                onClick = { openCharts(activity, d.chartsDevice, "activity") },
            ) { Sparkline(d.heartRateLine, c.heart) }
            Tile(
                icon = YuneIcons.Moon, label = stringResource(R.string.yb_sleep), ink = c.sleepInk,
                value = if (d.sleepMinutes > 0) hoursMinutes(d.sleepMinutes) else "–",
                unit = if (d.sleepMinutes > 0) stringResource(R.string.yb_hours_short) else noData,
                modifier = Modifier.weight(1f),
                onClick = { openCharts(activity, d.chartsDevice, "sleep") },
            ) { SleepBar(d.sleepStages) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalKcal = d.activeCalories + d.restingCalories
            Tile(
                icon = YuneIcons.Flame, label = stringResource(R.string.yb_calories), ink = c.caloriesInk,
                value = if (totalKcal > 0) numberFormat.format(totalKcal) else "–",
                unit = if (totalKcal > 0) "kcal" else noData,
                modifier = Modifier.weight(1f),
                onClick = { openCharts(activity, d.chartsDevice, "activity") },
            ) {
                ProgressBar(fraction = if (totalKcal > 0) d.activeCalories.toFloat() / totalKcal else 0f, color = c.calories)
                if (totalKcal > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.yb_active_kcal, d.activeCalories), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal, color = c.secondaryText)
                }
            }
            Tile(
                icon = YuneIcons.Stress, label = stringResource(R.string.yb_stress), ink = c.stressInk,
                value = d.stress?.toString() ?: "–",
                unit = if (d.stress != null) stringResource(R.string.yb_of_100) else noData,
                modifier = Modifier.weight(1f),
                onClick = null,
            ) { StressBar(d.stress) }
        }
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    label: String,
    ink: Color,
    value: String,
    unit: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    visual: @Composable () -> Unit,
) {
    val c = Yune.colors
    Column(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1)
        }
        Column { visual() }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)) { append(value) }
                withStyle(SpanStyle(fontSize = 14.sp, color = c.secondaryText)) { append(" $unit") }
            },
            style = MaterialTheme.typography.titleMedium,
            color = c.text,
            maxLines = 1,
        )
    }
}

/** Eigene Symbole für die weiteren Werte; unbekannte Widgets behalten ihr Gadgetbridge-Symbol. */
private fun extraIcon(id: String): ImageVector? = when (id) {
    "hrv", "bloodpressure" -> YuneIcons.Heart
    "sleepscore" -> YuneIcons.Moon
    "respiratoryrate" -> YuneIcons.Stress
    "spo2", "hydration" -> YuneIcons.Data
    "weight", "bmi", "bmr", "bodyfat", "bodywater", "musclemass" -> YuneIcons.Weight
    "bodyenergy" -> YuneIcons.Flame
    "pai", "vo2max" -> YuneIcons.Trends
    "trainingload", "trainingreadiness" -> YuneIcons.Stopwatch
    else -> null
}

/** Weitere Werte aus den Gadgetbridge-Widgets – nur solche mit Daten für den Tag. */
@Composable
private fun ExtrasSection(extras: List<ExtraMetric>) {
    val c = Yune.colors
    val context = LocalContext.current
    val empty = stringResource(R.string.stats_empty_value)
    val tiles = remember(extras) {
        extras.mapNotNull { e ->
            runCatching { e to e.widget.tileModelUnsafe(context, e.config, e.data) }.getOrNull()
        }.filter { (_, model) -> model.value.toString().trim() != empty && model.value.isNotBlank() }
    }
    if (tiles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.yb_overview_more),
            style = MaterialTheme.typography.headlineSmall,
            color = c.text,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (extra, model) -> ExtraTile(extra, model, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExtraTile(extra: ExtraMetric, model: TileModel, modifier: Modifier) {
    val c = Yune.colors
    val status = when (val s = model.status) {
        is StatusLine.Text -> s.text.toString()
        is StatusLine.Chip -> s.text.toString()
        null -> null
    }
    Column(
        modifier = modifier
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val vector = extraIcon(extra.widget.id)
            if (vector != null) {
                Icon(vector, contentDescription = null, tint = c.secondaryText, modifier = Modifier.size(17.dp))
            } else {
                Icon(painterResource(extra.widget.icon), contentDescription = null, tint = c.secondaryText, modifier = Modifier.size(17.dp))
            }
            Text(
                stringResource(extra.widget.label),
                style = MaterialTheme.typography.labelLarge,
                color = c.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GaugeBar(model.gauge)
        Column {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)) {
                        append(model.value.toString())
                    }
                    model.valueSuffix?.let { withStyle(SpanStyle(fontSize = 13.sp, color = c.secondaryText)) { append(" $it") } }
                },
                style = MaterialTheme.typography.titleMedium,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GaugeBar(gauge: GaugeSpec) {
    val c = Yune.colors
    when (gauge) {
        is GaugeSpec.Progress -> if (gauge.fraction >= 0f) ProgressBar(gauge.fraction, c.tint) else Spacer(Modifier.height(10.dp))
        is GaugeSpec.Zones -> {
            val total = gauge.segments.sum().takeIf { it > 0f } ?: 1f
            var start = 0f
            val active = gauge.segments.indices.firstOrNull { i ->
                val end = start + gauge.segments[i]
                (gauge.marker in start..end).also { start = end }
            }
            Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gauge.segments.forEachIndexed { i, w ->
                    if (w > 0f) {
                        Box(
                            Modifier
                                .weight(w / total)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(gauge.colors[i % gauge.colors.size]).copy(alpha = if (active == null || i == active) 1f else 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sparkline(line: FloatArray?, color: Color) {
    val values = line?.filter { it > 0f }.orEmpty()
    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
        if (values.size < 2) {
            drawLine(color.copy(alpha = 0.25f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            return@Canvas
        }
        val lo = values.min()
        val hi = max(values.max(), lo + 1f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - (v - lo) / (hi - lo) * (size.height - 4.dp.toPx()) - 2.dp.toPx()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun SleepBar(stages: LongArray?) {
    val c = Yune.colors
    // Reihenfolge der Daten: leicht, tief, REM, wach – angezeigt wie in der Vorschau: tief, leicht, REM, wach
    val parts = if (stages == null || stages.sum() <= 0) emptyList() else listOf(
        stages[1] to c.sleepDeep, stages[0] to c.sleepLight, stages[2] to c.sleepRem, stages[3] to c.sleepAwake,
    ).filter { it.first > 0 }
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (parts.isEmpty()) {
            Box(Modifier.weight(1f).height(10.dp).background(c.track))
        } else {
            parts.forEach { (minutes, color) ->
                Box(Modifier.weight(minutes.toFloat()).height(10.dp).background(color))
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    val c = Yune.colors
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.track)) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(min(fraction, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        }
    }
}

@Composable
private fun StressBar(value: Int?) {
    val c = Yune.colors
    // Gadgetbridge-Stufen: entspannt < 40, leicht < 60, mittel < 80, hoch
    val level = when {
        value == null -> -1
        value < 40 -> 0
        value < 60 -> 1
        value < 80 -> 2
        else -> 3
    }
    Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(4) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (i == level) c.stress else c.track)
            )
        }
    }
}
