package com.example.firebaseupload;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.firebaseupload.views.MyButton;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SettingsFragment extends Fragment implements ITabbedFragment {

    Switch mySwitch;
    @BindView(R.id.information_button)
    MyButton informationButton;
    @BindView(R.id.textViewCustom)
    TextView textViewCustom;
    @BindView(R.id.textViewOriginal)
    TextView textViewOriginal;

    private boolean switchState = false;

    public SettingsFragment() {
        // Required empty public constructor
    }

    private IAdapter mListener;

    public SettingsFragment(IAdapter listener) {
        // Required empty public constructor
        if (listener != null) {
            mListener = listener;
        }
    }

    public static SettingsFragment newInstance(IAdapter listener) {
        return new SettingsFragment(listener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        mySwitch = (Switch) view.findViewById(R.id.language_switch);


        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // do something when check is selected
                    MainActivity.setLanguage(false);
                    Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "Switch is true", Toast.LENGTH_LONG).show();

                } else {
                    //do something when unchecked
                    MainActivity.setLanguage(true);
                    Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "Switch is false", Toast.LENGTH_LONG).show();
                }
            }
        });
        return view;
    }


    @Override
    public void onReceive(Object o) {

    }


    @OnClick(R.id.information_button)
    public void onClick() {
        MainActivity.setCondition(true);

        Intent intent = new Intent(getActivity(), IntroActivity.class);
        startActivity(intent);

    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("switch", switchState);

    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        if (savedInstanceState != null) {
            switchState = savedInstanceState.getBoolean("switch");
        }
        if (savedInstanceState != null) {
            mySwitch.setChecked(savedInstanceState.getBoolean("switch"));
        }


    }
}
