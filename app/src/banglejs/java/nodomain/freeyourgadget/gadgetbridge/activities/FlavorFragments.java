package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.fragment.app.Fragment;

/**
 * Fragments, die je nach Produkt-Flavor ausgetauscht werden können.
 * Der Flavor "yunebridge" liefert hier eigene Compose-Screens.
 */
public final class FlavorFragments {
    private FlavorFragments() {
    }

    public static Fragment dashboard() {
        return new DashboardFragment();
    }

    public static Fragment devices() {
        return new DevicesFragment();
    }
}
