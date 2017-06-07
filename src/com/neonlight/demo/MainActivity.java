package com.neonlight.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends Activity {

    private RadioGroup mRadiogroup;
    private NeonLight mLightView;
    private RadioButton mR0;
    private RadioButton mR1;
    private RadioButton mR2;
    private RadioButton mR3;
    private RadioButton mR4;
    private RadioButton mR5;
    private CheckBox mCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLightView = (NeonLight) findViewById(R.id.light);
        mCheckBox = (CheckBox) findViewById(R.id.cb);
        mRadiogroup = (RadioGroup) findViewById(R.id.rg);
        mR0 = (RadioButton) findViewById(R.id.r0);
        mR1 = (RadioButton) findViewById(R.id.r1);
        mR2 = (RadioButton) findViewById(R.id.r2);
        mR3 = (RadioButton) findViewById(R.id.r3);
        mR4 = (RadioButton) findViewById(R.id.r4);
        mR5 = (RadioButton) findViewById(R.id.r5);

        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLightView.setPrivacy(isChecked);
            }
        });

        mRadiogroup.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mR0.getId() == checkedId) {
                    mLightView.setState(State.IDLE);
                } else if (mR1.getId() == checkedId) {
                    mLightView.setAnimationCallback(new NeonLight.AnimationCallback() {
                        @Override
                        public void onStartAnimationEnd() {
                            mR2.setChecked(true);
                        }
                    });
                    mLightView.setState(State.START);
                } else if (mR2.getId() == checkedId) {
                    mLightView.setState(State.LISTENING);
                } else if (mR3.getId() == checkedId) {
                    mLightView.setState(State.THINKING);
                } else if (mR4.getId() == checkedId) {
                    mLightView.setState(State.SPEAKING);
                } else if (mR5.getId() == checkedId) {
                    mLightView.setState(State.ERROR);
                }
            }
        });
    }
}
