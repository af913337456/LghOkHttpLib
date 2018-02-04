package com.example.lghokhttp;

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

interface LghOkCallBack {
    void onFailed(Request request, Exception e);
}
