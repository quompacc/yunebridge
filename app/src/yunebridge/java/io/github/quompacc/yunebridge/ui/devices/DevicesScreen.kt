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
package io.github.quompacc.yunebridge.ui.devices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.quompacc.yunebridge.ui.icons.YuneIcons
import io.github.quompacc.yunebridge.ui.theme.Yune
import io.github.quompacc.yunebridge.ui.theme.YuneColors
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.BatteryInfoActivity
import nodomain.freeyourgadget.gadgetbridge.activities.DeviceDeleteActivity
import nodomain.freeyourgadget.gadgetbridge.activities.OpenFwAppInstallerActivity
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceCardItemBinder
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager
import nodomain.freeyourgadget.gadgetbridge.devices.cards.ActionCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.BatteryCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.ColorCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.DeviceCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.DeviceCardLayout
import nodomain.freeyourgadget.gadgetbridge.devices.cards.HeartRateCardItem
import nodomain.freeyourgadget.gadgetbridge.devices.cards.MoreCardItem
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("YbDevicesScreen")

/** Schnellaktionen in dieser Reihenfolge bevorzugen; der Rest landet in der Liste. */
private val QUICK_PRIORITY = listOf("sync", "find_device", "alarms", "apps")
private const val QUICK_MAX = 4
private const val ID_SETTINGS = "device_specific_settings"
private const val ID_INSTALLER = "yb_installer"

/** Ein Eintrag der Geräte-Card: Schnellaktion oder Listenzeile. */
private class CardEntry(
    val id: String,
    val title: String,
    /** Kurze Beschriftung für die Schnellaktionen (Originaltexte sind dafür oft zu lang). */
    val shortTitle: String,
    val value: String?,
    val vector: ImageVector?,
    @param:DrawableRes val iconRes: Int,
    val onClick: (() -> Unit)?,
)

private class DeviceUi(
    val device: GBDevice,
    val name: String,
    val status: String,
    val battery: String?,
    val icon: ImageVector,
    val quick: List<CardEntry>,
    val rows: List<CardEntry>,
)

/**
 * Zähler, der sich erhöht, wenn sich Geräte ändern. Der Empfänger ist nur registriert,
 * solange der Screen sichtbar ist (STARTED), damit im Hintergrund nichts neu aufgebaut wird.
 * DeviceManager bündelt Änderungen bereits (ACTION_DEVICES_CHANGED, gedrosselt).
 */
@Composable
private fun rememberDevicesVersion(): Int {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val lbm = LocalBroadcastManager.getInstance(context)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                version++
            }
        }
        val filter = IntentFilter(DeviceManager.ACTION_DEVICES_CHANGED)
        var registered = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!registered) {
                    lbm.registerReceiver(receiver, filter)
                    registered = true
                    version++
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
    return version
}

private fun clickAction(item: DeviceCardItem, device: GBDevice, activity: FragmentActivity): (() -> Unit)? = when (item) {
    is ActionCardItem -> {
        { item.action.onClick(device, activity) }
    }

    is BatteryCardItem -> {
        {
            activity.startActivity(
                Intent(activity, BatteryInfoActivity::class.java)
                    .putExtra(GBDevice.EXTRA_DEVICE, device)
                    .putExtra(GBDevice.BATTERY_INDEX, item.batteryIndex)
            )
        }
    }

    is ColorCardItem -> {
        { item.onClick(device, activity) }
    }

    is HeartRateCardItem -> if (item.liveOnly) null else {
        { item.onClick(device, activity) }
    }

    MoreCardItem -> null
}

private fun vectorFor(id: String): ImageVector? = when {
    id == "sync" -> YuneIcons.Sync
    id == "find_device" -> YuneIcons.Find
    id == "alarms" -> YuneIcons.Alarm
    id == "reminders" -> YuneIcons.Bell
    id == "apps" -> YuneIcons.Apps
    id == "charts" -> YuneIcons.Trends
    id == "workouts" -> YuneIcons.Stopwatch
    id == "heart_rate" -> YuneIcons.Heart
    id == ID_SETTINGS -> YuneIcons.Sliders
    id == ID_INSTALLER -> YuneIcons.Chip
    id.startsWith("battery_") -> YuneIcons.Battery
    else -> null
}

private fun shortTitle(id: String, context: Context, fallback: String): String = when (id) {
    "sync" -> context.getString(R.string.yb_action_sync)
    "find_device" -> context.getString(R.string.yb_action_find)
    "alarms" -> context.getString(R.string.yb_action_alarms)
    "reminders" -> context.getString(R.string.yb_action_reminders)
    "apps" -> context.getString(R.string.yb_action_apps)
    "charts" -> context.getString(R.string.yb_action_charts)
    "workouts" -> context.getString(R.string.yb_action_workouts)
    else -> fallback
}

private fun deviceIcon(kind: DeviceCoordinator.DeviceKind): ImageVector = when (kind) {
    DeviceCoordinator.DeviceKind.WATCH, DeviceCoordinator.DeviceKind.SMART_CLOCK -> YuneIcons.Watch
    DeviceCoordinator.DeviceKind.FITNESS_BAND, DeviceCoordinator.DeviceKind.CHEST_STRAP -> YuneIcons.Band
    DeviceCoordinator.DeviceKind.RING -> YuneIcons.Ring
    DeviceCoordinator.DeviceKind.HEADPHONES, DeviceCoordinator.DeviceKind.EARBUDS,
    DeviceCoordinator.DeviceKind.SPEAKER, DeviceCoordinator.DeviceKind.HEAD_MOUNTED -> YuneIcons.Headphones
    DeviceCoordinator.DeviceKind.SCALE -> YuneIcons.Weight
    else -> YuneIcons.Bluetooth
}

private fun YuneColors.rowColor(id: String): Color = when {
    id == ID_SETTINGS || id == "sync" -> distance
    id == "alarms" -> steps
    id == "reminders" || id == "heart_rate" -> heart
    id == "apps" -> sleep
    id == "charts" -> calories
    id == "workouts" -> active
    id == "find_device" -> stress
    id.startsWith("battery_") -> connected
    else -> Color(0xFF7A7F76)
}

private fun buildDeviceUi(device: GBDevice, activity: FragmentActivity): DeviceUi {
    val coordinator = device.deviceCoordinator
    val entries = mutableListOf<Pair<DeviceCardItem?, CardEntry>>()

    if (DeviceCardLayout.isShown(device)) {
        DeviceCardLayout.ITEMS.apply(device, coordinator.getCardItems(device))
            .filter { it !is MoreCardItem && it.isVisible(device) }
            .forEach { item ->
                val value = (item as? BatteryCardItem)
                    ?.let { device.getBatteryLevel(it.batteryIndex) }
                    ?.takeIf { it >= 0 }
                    ?.let { "$it %" }
                val title = DeviceCardItemBinder.title(item, device, activity)
                entries += item to CardEntry(
                    id = item.id,
                    title = title,
                    shortTitle = shortTitle(item.id, activity, title),
                    value = value,
                    vector = vectorFor(item.id),
                    iconRes = DeviceCardItemBinder.icon(item, device),
                    onClick = clickAction(item, device, activity),
                )
            }
    }

    // Wie das Kontextmenü der alten Card: Installer für Zifferblätter, Apps und Firmware
    if (coordinator.supportsAppsManagement(device) || coordinator.supportsFlashing(device)) {
        entries += null to CardEntry(
            id = ID_INSTALLER,
            title = activity.getString(R.string.open_fw_installer_info_text_title),
            shortTitle = activity.getString(R.string.open_fw_installer_info_text_title),
            value = null,
            vector = YuneIcons.Chip,
            iconRes = R.drawable.ic_settings,
            onClick = if (device.isInitialized) {
                {
                    activity.startActivity(
                        Intent(activity, OpenFwAppInstallerActivity::class.java)
                            .putExtra(GBDevice.EXTRA_DEVICE, device)
                    )
                }
            } else null,
        )
    }

    val actions = entries.filter { (item, _) -> item is ActionCardItem && item.id != ID_SETTINGS }.map { it.second }
    val quick = buildList {
        QUICK_PRIORITY.forEach { id -> actions.firstOrNull { it.id == id }?.let { add(it) } }
        actions.filter { it !in this }.forEach { if (size < QUICK_MAX) add(it) }
    }.take(QUICK_MAX)
    val rows = entries.map { it.second }.filter { it !in quick }

    val battery = device.getBatteryLevel(0).takeIf { it >= 0 }?.let { "$it %" }
    val status = if (device.isBusy) device.busyTask ?: device.getStateString(activity) else device.getStateString(activity)

    return DeviceUi(
        device = device,
        name = device.aliasOrName,
        status = status,
        battery = battery,
        icon = deviceIcon(coordinator.getDeviceKind(device)),
        quick = quick,
        rows = rows,
    )
}

private fun connect(device: GBDevice) {
    if (!device.deviceCoordinator.isConnectable) {
        device.setState(GBDevice.State.WAITING_FOR_SCAN)
        device.sendDeviceUpdateIntent(GBApplication.getContext(), GBDevice.DeviceUpdateSubject.CONNECTION_STATE)
        return
    }
    GBApplication.deviceService(device).connect()
}

private fun disconnect(device: GBDevice) {
    if (device.state != GBDevice.State.NOT_CONNECTED) {
        GBApplication.deviceService(device).disconnect()
    }
    // Wie die alte Card: nach manuellem Trennen nicht automatisch wieder verbinden
    val prefs = GBApplication.getPrefs()
    val last = prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, emptySet())
    if (last.contains(device.address)) {
        prefs.preferences.edit().putStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, last - device.address).apply()
    }
}

private fun setAlias(context: Context, device: GBDevice, alias: String) {
    try {
        GBApplication.acquireDB().use { db ->
            val dbDevice = DBHelper.getDevice(device, db.daoSession)
            dbDevice.alias = alias
            dbDevice.update()
            device.alias = alias
        }
    } catch (e: Exception) {
        LOG.error("Failed to set alias", e)
    } finally {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(DeviceManager.ACTION_REFRESH_DEVICELIST))
    }
}

private fun removeDevice(activity: FragmentActivity, device: GBDevice, deleteFiles: Boolean) {
    activity.startActivity(
        Intent(activity, DeviceDeleteActivity::class.java)
            .putExtra(DeviceDeleteActivity.EXTRA_DEVICE, device)
            .putExtra(DeviceDeleteActivity.EXTRA_DELETE_FILES, deleteFiles)
    )
}

private fun openDiscovery(activity: FragmentActivity) {
    activity.startActivity(Intent(activity, DiscoveryActivityV2::class.java))
}

@Composable
fun DevicesScreen(activity: FragmentActivity) {
    val version = rememberDevicesVersion()
    val devices = remember(version) {
        GBApplication.app().deviceManager.devices.map { buildDeviceUi(it, activity) }
    }
    val c = Yune.colors

    LazyColumn(
        modifier = Modifier
            .background(c.background)
            .nestedScroll(rememberNestedScrollInteropConnection()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Header(count = devices.size, onAdd = { openDiscovery(activity) }) }
        if (devices.isEmpty()) {
            item(key = "empty") { EmptyState(onPair = { openDiscovery(activity) }) }
        } else {
            items(devices, key = { it.device.address }) { ui -> DeviceCard(ui, activity) }
        }
    }
}

@Composable
private fun Header(count: Int, onAdd: () -> Unit) {
    val c = Yune.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (count > 0) {
                Text(
                    text = pluralStringResource(R.plurals.yb_devices_paired, count, count).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.secondaryText,
                    letterSpacing = 0.4.sp,
                )
            }
            Text(
                text = stringResource(R.string.bottom_nav_devices),
                style = MaterialTheme.typography.displaySmall,
                color = c.text,
            )
        }
        RoundIconButton(icon = YuneIcons.Plus, description = stringResource(R.string.yb_devices_add), onClick = onAdd)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Yune.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.card)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = c.tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyState(onPair: () -> Unit) {
    val c = Yune.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.tintSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(YuneIcons.Watch, contentDescription = null, tint = c.tint, modifier = Modifier.size(38.dp))
        }
        Text(stringResource(R.string.yb_devices_empty_title), style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(
            stringResource(R.string.yb_devices_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = c.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onPair,
            colors = ButtonDefaults.buttonColors(containerColor = c.tint),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.yb_devices_pair), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun DeviceCard(ui: DeviceUi, activity: FragmentActivity) {
    val c = Yune.colors
    val device = ui.device
    val connected = device.isInitialized

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = device.state == GBDevice.State.NOT_CONNECTED) { connect(device) }
                .padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (connected) c.tintSoft else c.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ui.icon,
                    contentDescription = null,
                    tint = if (connected) c.tint else c.secondaryText,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    ui.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusLine(ui)
            }
            DeviceMenu(ui, activity)
        }

        if (ui.quick.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ui.quick.forEach { entry -> QuickAction(entry, Modifier.weight(1f)) }
                repeat(QUICK_MAX - ui.quick.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (ui.rows.isNotEmpty()) {
            HorizontalDivider(color = c.separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
            ui.rows.forEachIndexed { index, entry ->
                ListRow(entry, showDivider = index < ui.rows.lastIndex)
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusLine(ui: DeviceUi) {
    val c = Yune.colors
    val device = ui.device
    val dotColor = when {
        device.isBusy -> c.stress
        device.isInitialized -> c.connected
        device.state.ordinal >= GBDevice.State.CONNECTING.ordinal -> c.stress
        else -> c.secondaryText
    }
    val textColor = if (device.isInitialized) c.connectedInk else c.secondaryText
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(
            ui.status,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (device.isInitialized) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        ui.battery?.let {
            Text("·", style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
            Icon(YuneIcons.Battery, contentDescription = null, tint = c.secondaryText, modifier = Modifier.size(16.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
        }
    }
}

@Composable
private fun EntryIcon(entry: CardEntry, tint: Color, size: Int) {
    if (entry.vector != null) {
        Icon(entry.vector, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
    } else {
        Icon(painterResource(entry.iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun QuickAction(entry: CardEntry, modifier: Modifier) {
    val c = Yune.colors
    val enabled = entry.onClick != null
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button) { entry.onClick?.invoke() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(c.fill),
            contentAlignment = Alignment.Center,
        ) {
            EntryIcon(entry, tint = if (enabled) c.tint else c.secondaryText, size = 24)
        }
        Text(
            entry.shortTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = c.text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ListRow(entry: CardEntry, showDivider: Boolean) {
    val c = Yune.colors
    val enabled = entry.onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button) { entry.onClick?.invoke() }
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (enabled) c.rowColor(entry.id) else c.track),
            contentAlignment = Alignment.Center,
        ) {
            EntryIcon(entry, tint = if (enabled) Color.White else c.secondaryText, size = 18)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.heightIn(min = 52.dp).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.shortTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) c.text else c.secondaryText,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                )
                entry.value?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
                    Spacer(Modifier.width(4.dp))
                }
                if (enabled) {
                    Icon(YuneIcons.ChevronRight, contentDescription = null, tint = c.secondaryText, modifier = Modifier.size(18.dp))
                }
            }
            if (showDivider) {
                HorizontalDivider(color = c.separator, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun DeviceMenu(ui: DeviceUi, activity: FragmentActivity) {
    val c = Yune.colors
    val device = ui.device
    var expanded by remember { mutableStateOf(false) }
    var showAlias by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(YuneIcons.More, contentDescription = stringResource(R.string.yb_device_more), tint = c.secondaryText)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (device.state == GBDevice.State.NOT_CONNECTED) {
                DropdownMenuItem(text = { Text(stringResource(R.string.controlcenter_connect)) }, onClick = {
                    expanded = false
                    connect(device)
                })
            } else {
                DropdownMenuItem(text = { Text(stringResource(R.string.controlcenter_disconnect)) }, onClick = {
                    expanded = false
                    disconnect(device)
                })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.controlcenter_set_alias)) }, onClick = {
                expanded = false
                showAlias = true
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.controlcenter_delete_device)) }, onClick = {
                expanded = false
                showRemove = true
            })
        }
    }

    if (showAlias) {
        var alias by remember { mutableStateOf(device.alias ?: "") }
        AlertDialog(
            onDismissRequest = { showAlias = false },
            title = { Text(stringResource(R.string.controlcenter_set_alias)) },
            text = { OutlinedTextField(value = alias, onValueChange = { alias = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    showAlias = false
                    setAlias(activity, device, alias)
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showAlias = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showRemove) {
        AlertDialog(
            onDismissRequest = { showRemove = false },
            title = { Text(stringResource(R.string.controlcenter_delete_device_name, ui.name)) },
            text = { Text(stringResource(R.string.controlcenter_delete_device_dialogmessage)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemove = false
                    removeDevice(activity, device, deleteFiles = true)
                }) { Text(stringResource(R.string.delete), color = c.heartInk) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showRemove = false
                        removeDevice(activity, device, deleteFiles = false)
                    }) { Text(stringResource(R.string.delete_device_and_retain_files)) }
                    TextButton(onClick = { showRemove = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}
