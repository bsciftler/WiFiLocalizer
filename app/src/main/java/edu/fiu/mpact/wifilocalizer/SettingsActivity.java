package edu.fiu.mpact.wifilocalizer;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import com.google.common.collect.ImmutableSet;


public class SettingsActivity extends AppCompatActivity {
    public enum COLLECTION_MODES {CONTINUOUS, PASSES, SAMPLES};
    private static final ImmutableSet<String> MODE_OPTIONS = ImmutableSet.of
            ("pref_continuous_toggle", "pref_passes_toggle", "pref_samples_toggle");

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            for (String k : MODE_OPTIONS) {
                findPreference(k).setOnPreferenceClickListener(mListener);
            }
        }

        protected Preference.OnPreferenceClickListener mListener = new Preference
            .OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference instanceof CheckBoxPreference) {
                    // Make sure at least one option is selected at all times
                    ((CheckBoxPreference) preference).setChecked(true);
                    // Disable the other options
                    for (String modeOption : MODE_OPTIONS) {
                        if (!preference.getKey().equals(modeOption)) {
                            ((CheckBoxPreference) findPreference(modeOption)).setChecked(false);
                        }
                    }

                    return true;
                }

                return false;
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SettingsFragment frag = new SettingsFragment();
        transaction.replace(android.R.id.content, frag);
        transaction.commit();
    }
}
