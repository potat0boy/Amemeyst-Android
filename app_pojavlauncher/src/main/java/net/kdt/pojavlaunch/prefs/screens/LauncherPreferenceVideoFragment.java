package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    private static final int PICK_DRIVER_REQUEST = 1001;

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        setupBasicPrefs();
        reloadTurnipDrivers();

        Preference importBtn = findPreference("importTurnipDriver");
        if (importBtn != null) {
            importBtn.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/octet-stream");
                startActivityForResult(intent, PICK_DRIVER_REQUEST);
                return true;
            });
        }
        computeVisibility();
    }

    private void setupBasicPrefs() {
        requirePreference("ignoreNotch").setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && LauncherPreferences.PREF_NOTCH_SIZE > 0);
        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio", CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);
        if (resolution < 25) resolutionSeekbar.setValue(100);
        else resolutionSeekbar.setValue(resolution);
        
        SwitchPreference sustainedPerfSwitch = requirePreference("sustainedPerformance", SwitchPreference.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);
        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);
    }

    private void reloadTurnipDrivers() {
        ListPreference turnipPref = findPreference("chooseTurnipDriver");
        if (turnipPref == null) return;

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();
        entries.add("Default Driver");
        entryValues.add("default");

        File turnipDir = new File(requireContext().getFilesDir(), "turnip");
        if (turnipDir.exists()) {
            File[] files = turnipDir.listFiles((dir, name) -> name.endsWith(".so"));
            if (files != null) {
                for (File file : files) {
                    // Show the name without the .so extension in the UI for a cleaner look
                    entries.add(file.getName().replace(".so", ""));
                    entryValues.add(file.getAbsolutePath());
                }
            }
        }

        turnipPref.setEntries(entries.toArray(new CharSequence[0]));
        turnipPref.setEntryValues(entryValues.toArray(new CharSequence[0]));
        if (turnipPref.getValue() == null) turnipPref.setValue("default");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DRIVER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) showNamingDialog(uri);
        }
    }

    private void showNamingDialog(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Name your driver");
        
        final EditText input = new EditText(getContext());
        input.setHint("e.g. Turnip-v24.1");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "custom_driver_" + System.currentTimeMillis();
            saveDriverFile(uri, name + ".so");
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveDriverFile(Uri uri, String fileName) {
        try {
            File turnipDir = new File(requireContext().getFilesDir(), "turnip");
            if (!turnipDir.exists()) turnipDir.mkdirs();

            File destFile = new File(turnipDir, fileName);
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(destFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

            in.close();
            out.close();

            Toast.makeText(getContext(), "Added: " + fileName, Toast.LENGTH_SHORT).show();
            reloadTurnipDrivers(); 
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving driver", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
    }
}
