package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.fragment.app.Fragment;

import io.github.quompacc.yunebridge.ui.devices.YbDevicesFragment;
import io.github.quompacc.yunebridge.ui.overview.YbOverviewFragment;
import io.github.quompacc.yunebridge.ui.workouts.YbWorkoutsFragment;

/**
 * YuneBridge: eigene Compose-Screens für die Tabs des Hauptscreens.
 */
public final class FlavorFragments {
    private FlavorFragments() {
    }

    public static Fragment dashboard() {
        return new YbOverviewFragment();
    }

    public static Fragment workouts() {
        return new YbWorkoutsFragment();
    }

    public static Fragment devices() {
        return new YbDevicesFragment();
    }
}
