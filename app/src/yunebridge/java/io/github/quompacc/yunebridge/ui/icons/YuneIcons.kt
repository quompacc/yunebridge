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
package io.github.quompacc.yunebridge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Eigener YuneBridge-Symbolsatz: 24-px-Raster, 1,9 px Linie, runde Enden.
 * Die Pfade entsprechen der Design-Vorschau. Gezeichnet wird in Schwarz;
 * die Farbe setzt das Icon-Composable per Tint.
 */
object YuneIcons {
    private fun c(x: Float, y: Float, r: Float) =
        "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun e(x: Float, y: Float, rx: Float, ry: Float) =
        "M${x - rx} ${y}a$rx $ry 0 1 0 ${2 * rx} 0a$rx $ry 0 1 0 ${-2 * rx} 0"

    private fun r(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    private fun icon(name: String, path: String, stroke: Float = 1.9f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    val Overview by lazy { icon("overview", r(4f, 4f, 7f, 7f, 2f) + r(13f, 4f, 7f, 7f, 2f) + r(4f, 13f, 7f, 7f, 2f) + r(13f, 13f, 7f, 7f, 2f)) }
    val Trends by lazy { icon("trends", "M5 20v-7M10 20V5M15 20v-10M20 20v-5") }
    val Watch by lazy { icon("watch", r(6.5f, 6f, 11f, 12f, 3.2f) + "M9 6l.7-3h4.6l.7 3M9 18l.7 3h4.6l.7-3M12 9.5V12l1.6 1") }
    val Steps by lazy { icon("steps", e(7.2f, 8f, 2.8f, 4f) + "M5.2 14.2h4v1.3a2 2 0 0 1-4 0z" + e(16.8f, 11f, 2.8f, 4f) + "M14.8 17.2h4v1.3a2 2 0 0 1-4 0z") }
    val Heart by lazy { icon("heart", "M12 20s-7.5-4.6-7.5-10.2A4.1 4.1 0 0 1 12 7.4a4.1 4.1 0 0 1 7.5 2.4C19.5 15.4 12 20 12 20z") }
    val Moon by lazy { icon("moon", "M19.5 14.6A7.8 7.8 0 1 1 9.4 4.5a6.2 6.2 0 0 0 10.1 10.1z") }
    val Route by lazy { icon("route", c(6f, 18f, 2f) + c(18f, 6f, 2f) + "M8 18h7.5a3 3 0 0 0 0-6h-7a3 3 0 0 1 0-6H16") }
    val Stopwatch by lazy { icon("stopwatch", c(12f, 13.5f, 7f) + "M12 13.5V10M10 3h4M18.2 7.3l1.4-1.4") }
    val Flame by lazy { icon("flame", "M12 21c-3.8 0-6.5-2.6-6.5-6.2 0-3.4 2.5-5.3 3.7-8.3.5 1.6 1.4 2.6 2.4 3 .3-2.6 1.6-4.9 3.6-6.5-.3 2.7.7 4.6 2 6.4 1 1.4 1.3 3 1.3 4.6 0 3.6-2.7 7-6.5 7z") }
    val Stress by lazy { icon("stress", "M3 12h3.2l2.3-5.5 4 11 3-7.5 1.8 2H21") }
    val Sync by lazy { icon("sync", "M19.5 10A7.8 7.8 0 0 0 5.6 7.4M4.5 4v4h4M4.5 14a7.8 7.8 0 0 0 13.9 2.6M19.5 20v-4h-4") }
    val Find by lazy { icon("find", c(12f, 12f, 2f) + "M8.4 8.4a5.1 5.1 0 0 0 0 7.2M15.6 8.4a5.1 5.1 0 0 1 0 7.2M5.6 5.6a9 9 0 0 0 0 12.8M18.4 5.6a9 9 0 0 1 0 12.8") }
    val Alarm by lazy { icon("alarm", c(12f, 13f, 7f) + "M12 9.8V13l2.4 1.6M4.5 6l3-2.5M19.5 6l-3-2.5") }
    val Bell by lazy { icon("bell", "M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 2h-14zM10 20.5a2 2 0 0 0 4 0") }
    val Apps by lazy { icon("apps", r(4f, 4f, 16f, 16f, 4.5f) + "M9 9h.01M15 9h.01M9 15h.01M15 15h.01") }
    val Chip by lazy { icon("chip", r(7f, 7f, 10f, 10f, 2f) + "M10 4v3M14 4v3M10 17v3M14 17v3M4 10h3M4 14h3M17 10h3M17 14h3") }
    val Sliders by lazy { icon("sliders", "M4 7h9M19 7h1M4 17h3M13 17h7" + c(16f, 7f, 2.5f) + c(10f, 17f, 2.5f)) }
    val Battery by lazy { icon("battery", r(3f, 7.5f, 16f, 9f, 2.6f) + "M21.5 10.5v3M6.5 11v2") }
    val Calendar by lazy { icon("calendar", r(4f, 5.5f, 16f, 14.5f, 3f) + "M8 3.5v4M16 3.5v4M4 10.5h16") }
    val Plus by lazy { icon("plus", "M12 5v14M5 12h14", stroke = 2.2f) }
    val Share by lazy { icon("share", "M12 15V3.5M8 7.5l4-4 4 4M5.5 11v7.5a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V11") }
    val Data by lazy { icon("data", e(12f, 6f, 7f, 2.5f) + "M5 6v12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5V6M5 12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5") }
    val Band by lazy { icon("band", r(8f, 6.5f, 8f, 11f, 3f) + "M9.5 6.5V3M14.5 6.5V3M9.5 17.5V21M14.5 17.5V21M10.5 12h3") }
    val Ring by lazy { icon("ring", c(12f, 14.5f, 6f) + "M9.5 8.8l1-3.3h3l1 3.3") }
    val Headphones by lazy { icon("headphones", "M4.5 15.5V13a7.5 7.5 0 0 1 15 0v2.5" + r(3.5f, 14f, 4.5f, 6.5f, 1.8f) + r(16f, 14f, 4.5f, 6.5f, 1.8f)) }
    val Weight by lazy { icon("weight", r(4f, 4f, 16f, 16f, 4.5f) + "M8.5 10a5 5 0 0 1 7 0M12 11.5l1.4-2") }
    val Bluetooth by lazy { icon("bluetooth", "M7 7.5l10 9-5 4.5V3l5 4.5-10 9") }
    val ChevronRight by lazy { icon("chevronRight", "M9.5 6l6 6-6 6", stroke = 2f) }
    val ChevronLeft by lazy { icon("chevronLeft", "M14.5 6l-6 6 6 6", stroke = 2f) }
    val More by lazy { icon("more", c(6f, 12f, 0.6f) + c(12f, 12f, 0.6f) + c(18f, 12f, 0.6f), stroke = 2.6f) }
}
