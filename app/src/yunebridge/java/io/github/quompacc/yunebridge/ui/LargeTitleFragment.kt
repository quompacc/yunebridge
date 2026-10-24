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
package io.github.quompacc.yunebridge.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Basis für YuneBridge-Tabs mit eigener großer Überschrift: Solange der Tab sichtbar ist,
 * blendet er den kleinen Titel der Toolbar aus (Menü-Symbol und Aktionen bleiben).
 * ViewPager2 führt nur den sichtbaren Tab bis RESUMED, daher genügen onResume/onPause.
 */
abstract class LargeTitleFragment : Fragment() {
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onPause() {
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(true)
        super.onPause()
    }
}
