package com.linkedin.android.shaky;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ShakySettingDialog extends DialogFragment {

    public static final String UPDATE_SHAKY_SENSITIVITY = "UpdateShakySensitivity";
    public static final String SHAKY_SETTING_DIALOG = "ShakySettingDialog";
    public static final String SHAKY_NEW_SENSITIVITY = "ShakySensitivityLevel";
    public static final String SHAKY_CURRENT_SENSITIVITY = "ShakyCurrentSensitivity";

    private String[] sensitivityOptions;
    private @ShakeDelegate.SensitivityLevel int currentSensitivityLevel;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensitivityOptions = new String[] {getResources().getString(R.string.shaky_sensitivity_high),
                getResources().getString(R.string.shaky_sensitivity_medium),
                getResources().getString(R.string.shaky_sensitivity_low)};

        currentSensitivityLevel = getArguments().getInt(SHAKY_CURRENT_SENSITIVITY);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.AppCompatAlertDialog);

        builder.setTitle(getResources().getString(R.string.shaky_sensor_sensitivity));
        final Intent intent = new Intent(UPDATE_SHAKY_SENSITIVITY);
        builder.setSingleChoiceItems(sensitivityOptions, getIndexFromCurrentSensitivityLevel(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        intent.putExtra(SHAKY_NEW_SENSITIVITY, ShakeDelegate.SENSITIVITY_HARD);
                        break;
                    case 2:
                        intent.putExtra(SHAKY_NEW_SENSITIVITY, ShakeDelegate.SENSITIVITY_LIGHT);
                        break;
                    default:
                        intent.putExtra(SHAKY_NEW_SENSITIVITY, ShakeDelegate.SENSITIVITY_MEDIUM);
                        break;
                }
            }
        });

        builder.setPositiveButton(getResources().getString(R.string.shaky_general_ok_string), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    private int getIndexFromCurrentSensitivityLevel() {
        if (currentSensitivityLevel == ShakeDelegate.SENSITIVITY_HARD) {
            return 0;
        } else if (currentSensitivityLevel == ShakeDelegate.SENSITIVITY_LIGHT) {
            return 2;
        } else {
            return 1;
        }
    }
}
