package com.example.android.uamp.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import com.example.android.uamp.R;

/**
 * Created by David on 30/11/2015.
 */
public class SetTimerDialog extends DialogFragment {

    private NumberPicker numberPicker;

    public interface OnSleepTimerChangedListener {
        void onSleepTimerChanged(int x);
    }

    private OnSleepTimerChangedListener listener = null;
    public void setOnSetSleepTimerListener(OnSleepTimerChangedListener l) {
        listener = l;
    }

    private void btnOKClicked() {
        int value = numberPicker.getValue();
        if (listener != null)
            listener.onSleepTimerChanged(value);

        dismiss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_settimer, container, false);

        getDialog().setTitle("Sleep timer");

        Button btnCancel = (Button) rootView.findViewById(R.id.btnSetTimerDialogCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Button btnOK = (Button) rootView.findViewById(R.id.btnSetTimerDialogOK);
        btnOK.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                btnOKClicked();
            }
        });

        numberPicker = (NumberPicker) rootView.findViewById(R.id.numberpickerSlideshowSpeed);
        numberPicker.setMaxValue(90);
        numberPicker.setMinValue(1);
        numberPicker.setValue(15);
        return rootView;
    }

}
