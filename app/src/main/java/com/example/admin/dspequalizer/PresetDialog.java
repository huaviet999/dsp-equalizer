package com.example.admin.dspequalizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class PresetDialog extends AppCompatDialogFragment {
    public static int checkedPosition = 0;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_presets, null, false);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        builder.setView(view);

        onRadioButtonClick(radioGroup);
        return builder.create();
    }

    public void onRadioButtonClick(RadioGroup radioGroup) {

        RadioButton btn = (RadioButton) radioGroup.getChildAt(checkedPosition);
        btn.setChecked(true);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int childCount = group.getChildCount();
                for (int x = 0; x < childCount; x++) {

                    RadioButton btn = (RadioButton) group.getChildAt(x);
                    if (btn.getId() == checkedId) {
                        checkedPosition = x;
                        DialogListener activity = (DialogListener) getActivity();
                        activity.onReturnCheckedPosition(checkedPosition);
                        dismiss();
                    }
                }
            }
        });
    }
}
