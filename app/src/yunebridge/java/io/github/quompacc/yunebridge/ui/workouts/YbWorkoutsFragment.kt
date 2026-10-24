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
package io.github.quompacc.yunebridge.ui.workouts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.quompacc.yunebridge.ui.LargeTitleFragment
import io.github.quompacc.yunebridge.ui.theme.Yune
import io.github.quompacc.yunebridge.ui.theme.YuneTheme
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutListFragment

/**
 * Workouts-Tab: große Überschrift wie in den anderen YuneBridge-Tabs, darunter der
 * unveränderte Gadgetbridge-Tab (Filter, Auswahl, Upload, Health Connect bleiben erhalten).
 */
class YbWorkoutsFragment : LargeTitleFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val header = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YuneTheme {
                    Text(
                        stringResource(R.string.bottom_nav_workouts),
                        style = MaterialTheme.typography.displaySmall,
                        color = Yune.colors.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Yune.colors.background)
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
            }
        }
        val content = FrameLayout(context).apply { id = R.id.yb_child_container }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentById(R.id.yb_child_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.yb_child_container, WorkoutListFragment())
                .commitNow()
        }
    }
}
