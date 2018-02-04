package com.example.lghokhttp;

import java.io.File;

import okhttp3.Call;

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

public interface LghOkDownloadCallBack extends LghOkCallBack {
    void onStartInUiThread();
    void onProgress(int p);
    File toFile();
    void onSuccess(Call call,File targetFile);
}
