package com.example.android.uamp.ui.dialogs;

import android.text.Editable;
import android.text.TextWatcher;
import android.support.v7.widget.AppCompatEditText;

public class TextWatcherAdapter implements TextWatcher {

    public interface TextWatcherListener {

        void onTextChanged(AppCompatEditText view, String text);

    }

    private final AppCompatEditText view;
    private final TextWatcherListener listener;

    public TextWatcherAdapter(AppCompatEditText editText, TextWatcherListener listener) {
        this.view = editText;
        this.listener = listener;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        listener.onTextChanged(view, s.toString());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // pass
    }

    @Override
    public void afterTextChanged(Editable s) {
        // pass
    }

}
