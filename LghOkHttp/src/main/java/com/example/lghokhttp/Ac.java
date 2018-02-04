package com.example.lghokhttp;

import android.app.Activity;
import android.os.Bundle;

import java.io.File;

import okhttp3.Call;
import okhttp3.Request;

/**
 * 作者：林冠宏
 * <p>
 * author: LinGuanHong,lzq is my dear wife.
 * <p>
 * My GitHub : https://github.com/af913337456/
 * <p>
 * My Blog   : http://www.cnblogs.com/linguanh/
 * <p>
 * on 2018/1/1.
 */

public class Ac extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        LghOkHttpUtil
                .http()
                .url("")
                .uiThreadHandle()
                .async()
                .readTimeOut(3)
                .formData(null,null)
                .writeTimeOut(5)
                .customRequestBody(null)
                .cookies(null)
                .files(null)
                .headers(null,null)
                .method(LghOkHttpUtil.Method.POST)
                .downLoad()
                .blockSize(1234)
                .toFile(new File(""))
                .doDownLoad(
                        new LghOkDownloadCallBack() {
                            @Override
                            public void onStartInUiThread() {

                            }

                            @Override
                            public void onProgress(int p) {

                            }

                            @Override
                            public File toFile() {
                                return null;
                            }

                            @Override
                            public void onSuccess(Call call, File targetFile) {

                            }

                            @Override
                            public void onFailed(Request request, Exception e) {

                            }
                        }
                );


    }
}
