package com.gpstracking;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.Button;

public class DialogFragment extends androidx.fragment.app.DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inflater.inflate(R.layout.dialog, null))
                .setPositiveButton(R.string.Settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DialogFragment.this.getDialog().cancel();
                    }
                });


        return builder.create();
    }
}
