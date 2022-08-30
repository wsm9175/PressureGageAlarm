package com.lodong.android.pressuregagealarm.model;

import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TextFileMaker {
    private final String TAG = TextFileMaker.class.getSimpleName();
    public static String saveStorage = ""; //저장된 파일 경로
    private RandomAccessFile raf;

    public void fileSetting(String folderName, String fileName) {
        //폴더 생성
        String root = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + folderName + "/";
        File dir = new File(root);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File txt = new File(dir, fileName);
        saveStorage = txt.getAbsolutePath();

        try {
            if (!txt.exists()) {
                txt.createNewFile();
                raf = new RandomAccessFile(saveStorage, "rw");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeText(String data) {
        try {
            if (raf != null && data != null) {
                raf.seek(raf.length());
            }
            String writeData = new String(data.getBytes("KSC5601"), "8859_1");
            raf.writeBytes(writeData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeText() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
