package com.lodong.android.pressuregagealarm.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.OnReadMessageInterface;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.databinding.ActivitySettingBinding;
import com.lodong.android.pressuregagealarm.viewmodel.SettingViewModel;

public class SettingActivity extends AppCompatActivity implements OnReadMessageInterface {
    private final String TAG = SettingActivity.class.getSimpleName();
    private ActivitySettingBinding binding;
    private SettingViewModel viewModel;
    private String nowType;
    private String nowValue;

    private String[] timeItems;
    private final double criTime = 3600000;
    private final double[] timeValue = {criTime * 0.5, criTime * 1, criTime * 4, criTime * 8, criTime * 12, criTime * 24};
    private final String[] ITEM_PSI = {"1.422339psi", "4.267018psi", "7.111696psi"};
    private final double[] VALUE_PSI = {1.422339, 4.267018, 7.111696};
    private final String[] ITEM_BAR = {"0.098067bar", "0.294199bar", "0.490332bar"};
    private final double[] VALUE_BAR = {0.098067, 0.294199,0.490332};
    private final String[] ITEM_KGF = {"0.1kgf/cm2", "0.3kgf/cm2", "0.5kgf/cm2"};
    private final double[] VALUE_KGF = {0.1,0.3,0.5};

    private boolean isSpinnerSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        binding.setActivity(this);
        viewModel = new ViewModelProvider(this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(SettingViewModel.class);
        viewModel.setParent(this);

        init();

        settingClickListener();
    }

    private void init() {
        viewModel.setHandler(new BluetoothResponseHandler(this));
        viewModel.checkConnect();

        timeItems = getResources().getStringArray(R.array.array_time);

        binding.spinnerTime.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeItems));
    }

    private void settingClickListener(){
        binding.spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if(position == timeItems.length-1){
                    //시간 직접 입력
                }else{
                    //시간 설정
                    double time = timeValue[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.spinnerDeviation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                SettingActivity.this.nowType = nowType.trim();
                double deviation;
                if(nowType.equals("psi")){
                    deviation = VALUE_PSI[position];
                }else if(nowType.equals("bar")){
                    deviation = VALUE_BAR[position];
                }else if(nowType.equals("Kgf/Cm2")){
                    deviation = VALUE_KGF[position];
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void settingSpinnerDeviation(){
        this.nowType = nowType.trim();
        if(nowType.equals("psi")){
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
        }else if(nowType.equals("bar")){
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
        }else if(nowType.equals("Kgf/Cm2")){
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
        }
    }

    @Override
    public void onReadMessage(String message) {
        String value;
        StringBuilder msg = new StringBuilder();
        msg.append(message);
        Log.d("MSG", msg.toString() + " , " + msg.toString().length());
        if (msg.toString().length() >= 12 && msg.toString().length() < 23 && !msg.toString().contains("UNIT") && !msg.toString().contains("DATA")) {
            String[] strArrTmp = msg.toString().split(" ");
            if (strArrTmp.length == 3) {
                Log.d("ERROR", "********ERROR**********");
                return;
            } else {
                String press = strArrTmp[2];
                String type = strArrTmp[3];
                this.nowType = type;
                this.nowValue = press;
                Log.d(TAG, "press : " + press);
                Log.d(TAG, "type : " + type);

                binding.txtPressureValue.setText(press);
                binding.txtType.setText(type);
                if (binding.imgSignal.getVisibility() == View.INVISIBLE) {
                    binding.imgSignal.setVisibility(View.VISIBLE);
                } else {
                    binding.imgSignal.setVisibility(View.INVISIBLE);
                }
                if(!isSpinnerSetting){
                    settingSpinnerDeviation();
                }
            }
        }
    }
}