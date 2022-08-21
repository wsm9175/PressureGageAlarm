package com.lodong.android.pressuregagealarm.view;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.adapter.AddressAdapter;
import com.lodong.android.pressuregagealarm.databinding.ActivitySettingAddressBookBinding;

import java.util.ArrayList;
import java.util.List;

public class SettingAddressBookActivity extends AppCompatActivity {
    private ActivitySettingAddressBookBinding binding;
    private int position = 0;

    private ActivityResultLauncher<Intent> launcher;

    private List<String> phoneNumberList;
    private List<String> emailList;

    private AddressAdapter addressAdapter;

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra("phoneNumberList", (ArrayList<String>) this.phoneNumberList);
        intent.putStringArrayListExtra("emailList", (ArrayList<String>) this.emailList);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting_address_book);
        binding.setActivity(this);

        init();
        settingClickListener();
    }

    private void init() {
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Cursor cursor = null;
                        Intent data = result.getData();

                        if (data != null) {
                            cursor = getContentResolver().query(data.getData(),
                                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                            ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                        }
                        if (cursor != null) {
                            cursor.moveToFirst();

                            Log.e("cursor", "name : " + cursor.getString(0));
                            Log.e("cursor", "number : " + cursor.getString(1));

                            this.phoneNumberList.add(cursor.getString(1));
                            this.addressAdapter.setmList(this.phoneNumberList);
                            cursor.close();
                        }
                    }
                });

        List<String> phoneNumberList = getIntent().getStringArrayListExtra("phoneNumberList");
        List<String> emailList = getIntent().getStringArrayListExtra("emailList");

        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;

        if (this.phoneNumberList == null) {
            this.phoneNumberList = new ArrayList<>();
        }
        if (this.emailList == null) {
            this.emailList = new ArrayList<>();
        }

        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        addressAdapter = new AddressAdapter(getAddressLongClickListener());
        addressAdapter.setmList(this.phoneNumberList);
        binding.recyclerview.setAdapter(addressAdapter);

    }

    public void settingClickListener() {
        binding.tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int id = tab.getPosition();
                switch (id) {
                    case 0:
                        position = 0;
                        changeAdapter();
                        break;
                    case 1:
                        position = 1;
                        changeAdapter();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void changeAdapter() {
        if (position == 0) {
            binding.txtAddress.setVisibility(View.VISIBLE);
            addressAdapter.setmList(this.phoneNumberList);
        } else if (position == 1) {
            binding.txtAddress.setVisibility(View.INVISIBLE);
            addressAdapter.setmList(this.emailList);
        }
    }

    public void getAddressFromAddressBook() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        launcher.launch(intent);
    }

    public void showTypingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input_address, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        EditText edtAddress = dialogView.findViewById(R.id.edt_time);

        inputButton.setOnClickListener(view -> {
            String address = edtAddress.getText().toString();
            if (address == null) {
                Toast.makeText(this, "값을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            } else {
                inputAddress(address);
            }
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    private void inputAddress(String address) {
        if (position == 0) {
            this.phoneNumberList.add(address);
            addressAdapter.setmList(this.phoneNumberList);
        } else if (position == 1) {
            this.emailList.add(address);
            addressAdapter.setmList(this.emailList);
        }
    }

    private AddressLongClickListener getAddressLongClickListener() {
        return new AddressLongClickListener() {
            @Override
            public void onLongClick(int position) {
                if (SettingAddressBookActivity.this.position == 0) {
                    SettingAddressBookActivity.this.phoneNumberList.remove(position);
                    addressAdapter.setmList(SettingAddressBookActivity.this.phoneNumberList);
                } else if (SettingAddressBookActivity.this.position == 1) {
                    SettingAddressBookActivity.this.emailList.remove(position);
                    addressAdapter.setmList(SettingAddressBookActivity.this.emailList);
                }
            }
        };
    }

    public interface AddressLongClickListener {
        public void onLongClick(int position);
    }
}