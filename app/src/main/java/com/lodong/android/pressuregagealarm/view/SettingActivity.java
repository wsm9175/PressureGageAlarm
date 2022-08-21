package com.lodong.android.pressuregagealarm.view;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.OnReadMessageInterface;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.databinding.ActivitySettingBinding;
import com.lodong.android.pressuregagealarm.viewmodel.SettingViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity implements OnReadMessageInterface {
    private final String TAG = SettingActivity.class.getSimpleName();
    private ActivitySettingBinding binding;
    private SettingViewModel viewModel;
    private String nowType;
    private String nowValue;

    private ArrayList<String> timeItems;
    private final double criTime = 3600000;
    private ArrayList<Double> timeValue;
    private String[] timeList;
    private final Double[] timeVList = {criTime * 0.5, criTime * 1, criTime * 4, criTime * 8, criTime * 12, criTime * 24};
    private final String[] ITEM_PSI = {"1.422339psi", "4.267018psi", "7.111696psi"};
    private final double[] VALUE_PSI = {1.422339, 4.267018, 7.111696};
    private final String[] ITEM_BAR = {"0.098067bar", "0.294199bar", "0.490332bar"};
    private final double[] VALUE_BAR = {0.098067, 0.294199, 0.490332};
    private final String[] ITEM_KGF = {"0.1kgf/cm2", "0.3kgf/cm2", "0.5kgf/cm2"};
    private final double[] VALUE_KGF = {0.1, 0.3, 0.5};

    private boolean isSpinnerSetting;

    private long nowSettingTime = -99;
    private double nowDeviation = -99;
    private List<String> nowPhoneNumberList;
    private List<String> nowEmailList;

    ActivityResultLauncher<Intent> launcher;

    private boolean isSetting;
    private boolean isSettingSuccess;

    private MutableLiveData<Double> nowValueML = new MutableLiveData<>();
    private boolean isPressZeroAlarm;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

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

        getSettingInfo();
    }

    private void init() {
        binding.imgSignal.setVisibility(View.INVISIBLE);
        viewModel.setHandler(new BluetoothResponseHandler(this, Looper.getMainLooper()));
        viewModel.checkConnect();
        this.timeList = getResources().getStringArray(R.array.array_time);
        this.timeItems = new ArrayList<>();
        this.timeValue = new ArrayList<>();

        this.timeItems.addAll(Arrays.asList(timeList));
        this.timeValue.addAll(Arrays.asList(timeVList));

        binding.spinnerTime.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeItems));

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        List<String> phoneNumberList = intent.getStringArrayListExtra("phoneNumberList");
                        List<String> emailList = intent.getStringArrayListExtra("emailList");

                        if (phoneNumberList != null) {
                            SettingActivity.this.nowPhoneNumberList = phoneNumberList;
                        }
                        if (emailList != null) {
                            SettingActivity.this.nowEmailList = emailList;
                        }

                        settingAddressBook();
                    }
                });
        double timeValue = this.timeValue.get(binding.spinnerTime.getSelectedItemPosition());
        this.nowSettingTime = (long) timeValue;

    }

    private void settingClickListener() {
        binding.spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (position == timeItems.size() - 1) {
                    //시간 직접 입력
                    showTimeInputDialog();
                } else {
                    //시간 설정
                    double time = SettingActivity.this.timeValue.get(position);
                    nowSettingTime = (long) time;
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
                double deviation = 0;
                String type = "";
                if (nowType.equals("psi")) {
                    deviation = VALUE_PSI[position];
                    type = "psi";
                } else if (nowType.equals("bar")) {
                    deviation = VALUE_BAR[position];
                    type = "bar";
                } else if (nowType.equals("Kgf/Cm2")) {
                    deviation = VALUE_KGF[position];
                    type = "Kgf/Cm2";
                }
                changeDeviation(deviation, type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        binding.txtAddressBook.setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingAddressBookActivity.class);
            if (this.nowPhoneNumberList != null) {
                intent.putStringArrayListExtra("phoneNumberList", (ArrayList<String>) this.nowPhoneNumberList);
            }
            if (this.nowEmailList != null) {
                intent.putStringArrayListExtra("emailList", (ArrayList<String>) this.nowEmailList);
            }

            launcher.launch(intent);
        });

        this.nowValueML.observe(this, new Observer<Double>() {
            @Override
            public void onChanged(Double press) {
                if (press <= 0.0) {
                    if (!isPressZeroAlarm) {
                        isPressZeroAlarm = true;
                        showAlarmPressZero();
                    }
                }
            }
        });
    }

    private void settingSpinnerDeviation() {
        this.nowType = nowType.trim();
        int position;
        if (nowType.equals("psi")) {
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
            position = binding.spinnerDeviation.getSelectedItemPosition();
            this.nowDeviation = VALUE_PSI[position];
        } else if (nowType.equals("bar")) {
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
            position = binding.spinnerDeviation.getSelectedItemPosition();
            this.nowDeviation = VALUE_BAR[position];
        } else if (nowType.equals("Kgf/Cm2")) {
            binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
            position = binding.spinnerDeviation.getSelectedItemPosition();
            this.nowDeviation = VALUE_KGF[position];
        }
    }

    private void changeTime(String changeTime) {
        this.timeItems.add(this.timeItems.size() - 1, changeTime + "기입 시간");
        this.timeValue.add(this.timeValue.size(), Double.parseDouble(changeTime) * criTime);
        binding.spinnerTime.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, this.timeItems));
        nowSettingTime = (long) ((double) this.timeValue.get(timeValue.size() - 1));
        binding.spinnerTime.setSelection(this.timeItems.size() - 2);
        Log.d(TAG, "바뀐 시간 : " + binding.spinnerTime.getSelectedItem().toString());
        Log.d(TAG, "바뀐 시간 : " + this.timeValue);
    }

    private void changeDeviation(Double deviation, String type) {
        this.nowDeviation = deviation;
        this.nowType = type;
        Log.d(TAG, "바뀐 편차 : " + binding.spinnerDeviation.getSelectedItem().toString());
    }

    private void showTimeInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_setting, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        EditText edtTime = dialogView.findViewById(R.id.edt_time);

        inputButton.setOnClickListener(view -> {
            String inputTime = edtTime.getText().toString();
            if (inputTime.equals("")) {
                Toast.makeText(this, "시간이 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            } else {
                changeTime(inputTime);
                alertDialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    private void settingAddressBook() {
        StringBuilder displayAddress = new StringBuilder();

        if (this.nowPhoneNumberList != null) {
            for (String address : this.nowPhoneNumberList) {
                displayAddress.append(address + ",");
            }
        }

        if (this.nowEmailList != null) {
            for (String address : this.nowEmailList) {
                displayAddress.append(address + ",");
            }
        }

        if (displayAddress.length() != 0) {
            String display = displayAddress.toString();
            binding.txtAddressBook.setText(display);
        } else {
            binding.txtAddressBook.setText("설정되지 않음");
        }
    }

    public void insertSettingInfo() {
        long time = this.nowSettingTime;
        double deviation = this.nowDeviation;
        List<String> phoneNumberList = this.nowPhoneNumberList;
        List<String> emailList = this.nowEmailList;

        if (this.nowPhoneNumberList == null && this.nowEmailList == null) {
            showNoAddressDialog();
            return;
        }
        if(this.nowPhoneNumberList.size() == 0 && this.nowEmailList.size() == 0){
            showNoAddressDialog();
            return;
        }
        viewModel.insertSettingInfo(time, deviation, phoneNumberList, emailList, nowType);
    }

    public void getSettingInfo() {
        viewModel.getIsSetting().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSetting) {
                SettingActivity.this.isSetting = true;
                isSettingSuccess = false;
            }
        });
        viewModel.getSettingInfo();
    }

    private void changeSettingDisplay() {
        Log.d(TAG, "changeSettingDisplay");
        if (this.nowType != null) {
            double deviation = viewModel.getSettingDeviation();
            this.nowDeviation = deviation;

            long time = viewModel.getSettingTime();
            String changeTime = String.valueOf(time / criTime);
            changeTime(changeTime);
            this.nowSettingTime = time;

            String deviationType = viewModel.getSettingDeviationType();
            if (deviationType.equals("psi")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    if (Double.compare(VALUE_PSI[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                if (this.nowType.equals("psi")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
                    binding.spinnerDeviation.setSelection(position);
                    /*this.nowDeviation = VALUE_BAR[position];*/
                } else if (this.nowType.equals("bar")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_BAR[position];
                } else if (this.nowType.equals("Kgf/Cm2")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_KGF[position];
                }
            } else if (deviationType.equals("bar")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    Log.d(TAG, VALUE_BAR[i] + "vs" + deviation);
                    if (Double.compare(VALUE_BAR[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                if (this.nowType.equals("psi")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_PSI[position];
                } else if (this.nowType.equals("bar")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
                    binding.spinnerDeviation.setSelection(position);
                    /*this.nowDeviation = VALUE_BAR[position];*/
                } else if (this.nowType.equals("Kgf/Cm2")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_KGF[position];
                }
            } else if (deviationType.equals("Kgf/Cm2")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    Log.d(TAG, VALUE_KGF[i] + "vs" + deviation);
                    if (Double.compare(VALUE_KGF[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                if (this.nowType.equals("psi")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_PSI[position];
                } else if (this.nowType.equals("bar")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
                    binding.spinnerDeviation.setSelection(position);
                    this.nowDeviation = VALUE_BAR[position];
                } else if (this.nowType.equals("Kgf/Cm2")) {
                    binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
                    binding.spinnerDeviation.setSelection(position);
                    /*this.nowDeviation = VALUE_BAR[position];*/
                }
            }

            List<String> phoneNumber = viewModel.getSettingPhoneNumber();
            this.nowPhoneNumberList = phoneNumber;
            List<String> emailNNumber = viewModel.getSettingEmailList();
            this.nowEmailList = emailNNumber;
            settingAddressBook();

            isSettingSuccess = true;
            binding.txtDeviation.setText("±" + this.nowDeviation);
        } else {
            double deviation = viewModel.getSettingDeviation();
            this.nowDeviation = deviation;

            long time = viewModel.getSettingTime();
            String changeTime = String.valueOf(time / criTime);
            changeTime(changeTime);
            this.nowSettingTime = time;

            String deviationType = viewModel.getSettingDeviationType();
            if (deviationType.equals("psi")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    if (Double.compare(VALUE_PSI[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_PSI));
                binding.spinnerDeviation.setSelection(position);
                this.nowDeviation = VALUE_PSI[position];
                this.nowType = "psi";
            } else if (deviationType.equals("bar")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    Log.d(TAG, VALUE_BAR[i] + "vs" + deviation);
                    if (Double.compare(VALUE_BAR[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_BAR));
                binding.spinnerDeviation.setSelection(position);
                this.nowDeviation = VALUE_BAR[position];
                this.nowType = "bar";
            } else if (deviationType.equals("Kgf/Cm2")) {
                int position = 0;
                for (int i = 0; i < 3; i++) {
                    Log.d(TAG, VALUE_KGF[i] + "vs" + deviation);
                    if (Double.compare(VALUE_KGF[i], deviation) == 0) {
                        position = i;
                        break;
                    }
                }
                binding.spinnerDeviation.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ITEM_KGF));
                binding.spinnerDeviation.setSelection(position);
                this.nowDeviation = VALUE_KGF[position];
                this.nowType = "Kgf/Cm2";
            }

            List<String> phoneNumber = viewModel.getSettingPhoneNumber();
            this.nowPhoneNumberList = phoneNumber;
            List<String> emailNNumber = viewModel.getSettingEmailList();
            this.nowEmailList = emailNNumber;
            settingAddressBook();

            isSettingSuccess = true;
            binding.txtDeviation.setText("±" + this.nowDeviation);
        }
    }

    private void showAlarmPressZero() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_press_zero, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);

        inputButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    private void showNoAddressDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_address, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);

        inputButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    @Override
    public void onReadMessage(String message) {
        String value;
        StringBuilder msg = new StringBuilder();
        msg.append(message);
        /*Log.d("MSG", msg.toString() + " , " + msg.toString().length());*/
        if (msg.toString().length() >= 12 && msg.toString().length() < 23 && !msg.toString().contains("UNIT") && !msg.toString().contains("DATA")) {
            String[] strArrTmp = msg.toString().split(" ");
            if (strArrTmp.length == 3) {
                Log.e("ERROR", "********ERROR**********");
                return;
            } else if (strArrTmp.length == 2) {
                Log.e("ERROR", "********ERROR**********");
                return;
            } else {
                String press = strArrTmp[2];
                String type = strArrTmp[3];
                this.nowType = type.trim();
                this.nowValue = press;
              /*  Log.d(TAG, "press : " + press);
                Log.d(TAG, "type : " + type);*/

                nowValueML.setValue(Double.valueOf(press.trim()));
                binding.txtPressureValue.setText(press);
                binding.txtType.setText(type);
                if (binding.imgSignal.getVisibility() == View.INVISIBLE) {
                    binding.imgSignal.setVisibility(View.VISIBLE);
                } else {
                    binding.imgSignal.setVisibility(View.INVISIBLE);
                }
                if (!isSpinnerSetting) {
                    settingSpinnerDeviation();
                    isSpinnerSetting = true;
                }

                if (this.isSetting && !isSettingSuccess) {
                    changeSettingDisplay();
                }
            }
        }
    }
}