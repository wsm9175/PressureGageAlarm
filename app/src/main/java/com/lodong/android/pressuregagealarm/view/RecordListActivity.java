package com.lodong.android.pressuregagealarm.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.adapter.RecordAdapter;
import com.lodong.android.pressuregagealarm.databinding.ActivityRecordListBinding;
import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.roomDB.EventListInterface;

import java.util.List;

public class RecordListActivity extends AppCompatActivity {
    private ActivityRecordListBinding binding;
    private RecordAdapter adapter;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.none, R.anim.horizon_exit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.horizon_enter, R.anim.none);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_record_list);
        binding.setActivity(this);

        init();
    }

    private void init(){
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        adapter = new RecordAdapter();
        binding.recyclerview.setAdapter(adapter);
        getRecordList();
    }

    private void getRecordList(){
        EventListInterface eventListInterface = new EventListInterface(getApplication());
        eventListInterface.getEventList().observe(this, eventEntities -> {
            if(eventEntities != null){
                if(eventEntities.size() != 0){
                    adapter.setmList(eventEntities);
                }
            }
        });
    }
}