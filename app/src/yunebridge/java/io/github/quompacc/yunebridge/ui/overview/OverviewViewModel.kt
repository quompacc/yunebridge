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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.widgets.EmptySharedPreferences
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser
import nodomain.freeyourgadget.gadgetbridge.widgets.DashboardQuery
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetConfig
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetDataScope
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetInstance
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetRegistry
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetVariant
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.HeartRateWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.MetricWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.StressWidget
import org.slf4j.LoggerFactory
import java.time.LocalDate

/** Tageswerte für die Übersicht. Null bedeutet: keine Daten oder vom Gerät nicht unterstützt. */
data class OverviewData(
    val day: LocalDate,
    val isToday: Boolean,
    val hasDevices: Boolean,
    val steps: Int,
    val stepsGoal: Int,
    val distanceMeters: Float,
    val distanceGoalMeters: Int,
    val activeMinutes: Long,
    val activeGoalMinutes: Int,
    /** Leicht, Tief, REM, Wach in Minuten. */
    val sleepStages: LongArray?,
    val sleepMinutes: Long,
    val activeCalories: Int,
    val restingCalories: Int,
    val heartRate: Int?,
    val restingHeartRate: Int?,
    val heartRateLine: FloatArray?,
    val stress: Int?,
    /** Gerät für die Diagramme beim Antippen einer Kachel. */
    val chartsDevice: GBDevice?,
    /** Weitere Werte, die die Geräte liefern (HRV, SpO2, Gewicht …). */
    val extras: List<ExtraMetric> = emptyList(),
)

/** Geladene Daten eines Gadgetbridge-Widgets; das Anzeige-Modell entsteht in der UI (Sprache/Context). */
class ExtraMetric(val widget: MetricWidget<*>, val config: WidgetConfig, val data: Any?)

/** Werte, die die Übersicht schon selbst zeigt. */
private val SHOWN_IN_OVERVIEW = setOf("steps", "distance", "activetime", "activecalories", "calories", "heartrate", "sleep", "stress")

/**
 * Lädt die Tageswerte lesend über die Datenklassen von Gadgetbridge (WidgetDataScope).
 * Geladen wird nur, solange die Übersicht sichtbar ist (WhileSubscribed), bei Datumswechsel
 * und wenn neue Gerätedaten eintreffen. Kein Polling.
 */
class OverviewViewModel : ViewModel() {
    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val reload = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: StateFlow<OverviewData?> = combine(selectedDay, reload) { day, _ -> day }
        .mapLatest { day -> load(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val day: StateFlow<LocalDate> get() = selectedDay

    fun previousDay() {
        selectedDay.value = selectedDay.value.minusDays(1)
    }

    fun nextDay() {
        val next = selectedDay.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) selectedDay.value = next
    }

    fun selectDay(day: LocalDate) {
        selectedDay.value = if (day.isAfter(LocalDate.now())) LocalDate.now() else day
    }

    fun refresh() {
        reload.value++
    }

    private suspend fun load(day: LocalDate): OverviewData {
        val devices = WidgetRegistry.dashboardDevices()
        val scope = WidgetDataScope(DashboardQuery.forDay(day), devices)
        val user = ActivityUser()

        val sleepMinutes = safe(0L) { scope.sleepMinutesTotal() }
        val stages = if (sleepMinutes > 0) safe<LongArray?>(null) { scope.sleepStageMinutes() } else null

        val hr = safe<HeartRateWidget.Data?>(null) {
            HeartRateWidget.loadData(scope, config(HeartRateWidget.id, WidgetVariant.TALL))
        }
        val stress = safe<Int?>(null) {
            StressWidget.loadData(scope, config(StressWidget.id, WidgetVariant.GAUGE))?.value?.takeIf { it > 0 }
        }

        return OverviewData(
            day = day,
            isToday = scope.isToday,
            hasDevices = devices.isNotEmpty(),
            steps = safe(0) { scope.stepsTotal() },
            stepsGoal = user.stepsGoal,
            distanceMeters = safe(0f) { scope.distanceTotal() },
            distanceGoalMeters = user.distanceGoalMeters,
            activeMinutes = safe(0L) { scope.activeMinutesTotal() },
            activeGoalMinutes = user.activeTimeGoalMinutes,
            sleepStages = stages,
            sleepMinutes = sleepMinutes,
            activeCalories = safe(0) { scope.activeCaloriesTotal() },
            restingCalories = safe(0) { scope.restingCaloriesTotal() },
            heartRate = hr?.heartRate?.takeIf { it > 0 },
            restingHeartRate = hr?.resting?.takeIf { it > 0 },
            heartRateLine = hr?.line,
            stress = stress,
            // Bevorzugt das verbundene Gerät, sonst das erste mit Aktivitätsdaten
            chartsDevice = devices.filter { it.deviceCoordinator.supportsActivityTracking(it) }
                .sortedByDescending { it.isInitialized }
                .firstOrNull(),
            extras = loadExtras(day, devices),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun loadExtras(day: LocalDate, devices: List<GBDevice>): List<ExtraMetric> {
        val result = mutableListOf<ExtraMetric>()
        for (widget in WidgetRegistry.available().filterIsInstance<MetricWidget<*>>()) {
            if (widget.id in SHOWN_IN_OVERVIEW) continue
            val supported = devices.filter { widget.isSupportedBy(it) }
            if (supported.isEmpty()) continue
            val config = config(widget.id, WidgetVariant.GAUGE)
            val scope = WidgetDataScope(DashboardQuery.forDay(day), supported)
            val data = safe<Any?>(NOT_LOADED) { (widget as MetricWidget<Any?>).loadData(scope, config) }
            if (data !== NOT_LOADED) result += ExtraMetric(widget, config, data)
        }
        return result
    }

    private fun config(typeId: String, variant: WidgetVariant) = WidgetConfig(
        WidgetInstance("yunebridge_overview_$typeId", typeId),
        Prefs(EmptySharedPreferences),
        true,
        emptySet(),
        variant,
    )

    private inline fun <T> safe(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        LOG.warn("Overview: could not load value", e)
        fallback
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OverviewViewModel::class.java)
        private val NOT_LOADED = Any()
    }
}
