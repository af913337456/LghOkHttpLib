package com.example.lghokhttp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by lgh on 17-11-25.
 */

// todo 添加拦截器
@SuppressWarnings("all")
public class LghOkHttpUtil {

    private final static String TAG = "LghOkHttpUtil";
    private static Builder builder;

    public enum Method {
        GET,
        POST,
        DELETE,
        PUT,
        HEAD
    }

    public enum FileType {
        IMAGE,
        VIDEO,
        FILE
    }

    private LghOkHttpUtil(){

    }

    public static Builder http(){
        if(builder == null){
            builder = Builder.creator.builder;
        }
        builder.reset();
        return builder;
    }

    public static Builder https(Context context,OkHttpsConfiguration configuration){
        if(builder == null){
            builder = Builder.creator.builder;
        }
        builder.reset();
        builder.setOkHttpsConfiguration(context,configuration);
        return builder;
    }

    public static class RequestFile{
        private String name;
        private String fileName;
        private FileType fileType;
        private File file;

        public RequestFile(String name,String fileName,File file){
            init(name,fileName, FileType.FILE,file);
        }

        public RequestFile(String name,String fileName,FileType fileType,File file){
            init(name,fileName,fileType,file);
        }

        private void init(String name,String fileName,FileType fileType,File file){
            this.fileType = fileType;
            this.name = name;
            this.fileName = fileName;
            this.file = file;
        }
    }

    // ok 的response 的读取依然处以 网络链接态
    public static class LghResponse{
        public String body;
        public String headers;
        public int code;
        public String message;

        @Override
        public String toString() {
            return "LghResponse{" +
                    "body='" + body + '\'' +
                    ", headrs='" + headers + '\'' +
                    ", code=" + code +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    public static class Builder{

        private OkHttpClient client;
        private OkHttpClient.Builder okBuilder;
        private LghOkDownLoad downLoader;
        private int readTimeOut = 0;
        private int writeTimeOut = 0;

        private Handler handler;
        private boolean background = true;
        private boolean mainHandle = false;

        private RequestBody customRequestBody = null;
        private Method method = Method.GET;

        private MediaType multipartType;

        private String url;
        private String json;
        private String cookies;
        private String[] headersKey;
        private String[] headersValues;

        private String[] keys;
        private String[] values;
        private RequestFile[] files;

        private Builder(){
            okBuilder = new OkHttpClient.Builder();
            handler = new Handler(Looper.getMainLooper());
        }

        private static class creator{
            private static Builder builder = new Builder();
        }

        private void reset(){
            method = Method.GET;
            multipartType = null;
            readTimeOut  = 0;
            writeTimeOut = 0;
            url = null;
            json = null;
            cookies = null;
            keys = null;
            values = null;
            files = null;
            headersKey = null;
            headersValues = null;
            background = true;
            mainHandle = false;
        }

        private boolean check(){
            return url != null;
        }

        private abstract static class DownloadRunnable implements Runnable{

            public int progress;

            public abstract void update(int progress);

            @Override
            public void run() {
                update(progress);
            }
        }

        public LghOkDownLoad downLoad(){
            if(downLoader == null)
                downLoader = new LghOkDownLoad();
            return downLoader;
        }

        public class LghOkDownLoad{
            private int blockSize = 2048;
            private File toFile;

            public LghOkDownLoad blockSize(int blockSize){
                this.blockSize = blockSize;
                return this;
            }

            @Deprecated
            public LghOkDownLoad toFile(@NonNull File toFile){
                if(!toFile.exists())
                    throw new IllegalArgumentException("file is not exits!");
                this.toFile = toFile;
                return this;
            }

            public void doDownLoad(final LghOkDownloadCallBack callBack){
                executeDownload(false,callBack);
            }

            /**
             * doDownLoad
             * @param rebuild rebuild init okHttpClient
             * @param callBack
             */
            public void doDownLoad(boolean rebuild,final LghOkDownloadCallBack callBack){
                executeDownload(rebuild,callBack);
            }

            /**
             * real do ok download
             * @param reBuild
             * @param callBack
             */
            private void executeDownload(
                    boolean reBuild,
                    final LghOkDownloadCallBack callBack)
            {
                final Request request = buildRequest();
                if(reBuild || client == null)
                    client = okBuilder.build();
                callBack.onStartInUiThread();
                if(background){
                    // async
                    client.newCall(request).enqueue(
                            new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Log.e(TAG, "下载 onFailure " + e.toString());
                                    callBack.onFailed(request,e);
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    readDownloadnFile(call,request,response,callBack);
                                }
                            }
                    );
                    return;
                }
                // sync
                try {
                    final Response response = client.newCall(request).execute();
                    readDownloadnFile(null,request,response,callBack);
                } catch (IOException e) {
                    callBack.onFailed(request,e);
                }
            }

            private void readDownloadnFile(
                    final Call call,
                    final Request request,
                    final Response response,
                    final LghOkDownloadCallBack callBack
            ){
                InputStream is = null;
                byte[] buf = new byte[blockSize];
                int len = 0;
                FileOutputStream fos = null;
                final File toFile = callBack.toFile() == null?this.toFile:callBack.toFile();
                try {
                    long total = response.body().contentLength();
                    Log.e(TAG, "下载的总大小 " + total);
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(toFile);
                    if(mainHandle){
                        while ((len = is.read(buf)) != -1) {
                            current += len;
                            fos.write(buf, 0, len);
                            final int pr = (int) (((current*1.0)/ total)* 100);
                            Log.e(TAG, "mainHandle 当前进度 current------>" + pr);
                            handler.post(
                                    new DownloadRunnable() {
                                        @Override
                                        public void update(int progress) {
                                            callBack.onProgress(pr);
                                        }
                                    }
                            );
                        }
                    }else{
                        while ((len = is.read(buf)) != -1) {
                            current += len;
                            fos.write(buf, 0, len);
                            int pr = (int) (((current*1.0)/ total)* 100);
                            Log.e(TAG, "syncHandle 当前进度 current------>" + pr);
                            callBack.onProgress(pr);
                        }
                    }
                    fos.flush();
                    if(mainHandle) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                callBack.onSuccess(call, toFile);
                            }
                        };
                        handler.post(r);
                    }else
                        callBack.onSuccess(call, toFile);
                } catch (final IOException e) {
                    Log.e(TAG, "下载文件异常 " + e.toString());
                    if(mainHandle) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                callBack.onFailed(request,e);
                            }
                        };
                        handler.post(r);
                    }else
                        callBack.onFailed(request,e);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        if (fos != null)
                            fos.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "下载文件异常 " + e.toString());
                        if(mainHandle) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    callBack.onFailed(request,e);
                                }
                            };
                            handler.post(r);
                        }else
                            callBack.onFailed(request,e);
                    }
                }
            }
        }

        /**
         * doRequest
         * @param reBuild rebuild init okHttpClient
         * @param callback
         */
        public void doRequest(boolean rebuild,LghOkRequestCallBack callback){
            executeRequest(rebuild,callback);
        }

        /**
         * doRequest
         * @param callback
         */
        public void doRequest(LghOkRequestCallBack callback){
            executeRequest(false,callback);
        }

        /**
         * real do ok request
         * @param reBuild
         * @param callback
         */
        private void executeRequest(boolean reBuild,final LghOkRequestCallBack callback){
            final Request request = buildRequest();
            if(reBuild || client == null)
                client = okBuilder.build();
            if(background){
                // async
                client.newCall(request).enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                handleFailed(callback,request,e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                handleRespone(call,callback,response,request);
                            }
                        }
                );
                return;
            }
            // sync
            try {
                final Response response = client.newCall(request).execute();
                handleRespone(null,callback,response,request);
            } catch (IOException e) {
                handleFailed(callback,request,e);
            }
        }

        private Request buildRequest(){
            if(!check())
                throw new NullPointerException("url cant null !");

            if(readTimeOut != 0)
                okBuilder.readTimeout (readTimeOut, TimeUnit.SECONDS);

            if(writeTimeOut != 0)
                okBuilder.writeTimeout(writeTimeOut, TimeUnit.SECONDS);

            MediaType mediaType;
            RequestBody requestBody = null;
            // json
            if(json != null){
                mediaType = MediaType.parse("application/json; charset=utf-8");
                requestBody = RequestBody.create(mediaType, json);
            }else{
                // 自定义
                if(customRequestBody != null)
                    requestBody = customRequestBody;

                // 文件
                if(files != null && files.length > 0){
                    if(method != Method.POST && method != Method.PUT)
                        throw new RuntimeException("file upload must use POST !");

                    final MultipartBody.Builder builder;

                    if(multipartType != null)
                        builder = new MultipartBody.Builder().setType(multipartType);
                    else
                        // 默认使用表单
                        builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

                    for (RequestFile requestFile : files) {
                        switch (requestFile.fileType){
                            case IMAGE:
                                mediaType = MediaType.parse("image/jpeg");
                                break;
                            case VIDEO:
                            case FILE:
                            default:
                                mediaType = MediaType.parse("application/octet-stream");
                                break;
                        }
                        if(keys != null && values != null){
	                        int keyLength = keys.length;
	                        int valuesLen = values.length;
	                        if (keyLength != valuesLen)
	                            throw new RuntimeException("keys.length != values.length");
	                        for (int i = 0; i < keyLength; i++)
	                            builder.addFormDataPart(keys[i],values[i]);
	                    }
                        builder.addFormDataPart
                                (
                                        requestFile.name,
                                        requestFile.fileName,
                                        RequestBody.create
                                                (
                                                        mediaType,
                                                        requestFile.file
                                                )
                                );
                    }
                    requestBody = builder.build();
                }else{
                    // 表单
                    if(keys != null && values != null){
                        final FormBody.Builder builder = new FormBody.Builder();
                        int keyLength = keys.length;
                        int valuesLen = values.length;
                        if (keyLength != valuesLen)
                            throw new RuntimeException("keys.length != values.length");

                        for (int i = 0; i < keyLength; i++)
                            builder.add(keys[i], values[i]);

                        requestBody = builder.build();
                    }
                }
            }

            if(requestBody == null && (method == Method.POST || method == Method.PUT))
                throw new NullPointerException("there is no request type!");

            final Request.Builder requestBuilder = new Request.Builder();
            final Request request;
            if(cookies != null)
                requestBuilder.addHeader("Cookie", cookies);

            if(headersKey != null && headersValues != null) {
                int keyLength = headersKey.length;
                int valuesLen = headersValues.length;
                if (keyLength != valuesLen)
                    throw new RuntimeException("headersKey.length != headersValues.length");

                for (int i = 0; i < keyLength; i++)
                    requestBuilder.addHeader(headersKey[i], headersValues[i]);
            }

            switch (method){
                case GET:
                    request = requestBuilder.url(url).get().build();
                    break;
                case POST:
                    request = requestBuilder.url(url).post(requestBody).build();
                    break;
                case DELETE:
                    if(requestBody == null)
                        request = requestBuilder.url(url).delete().build();
                    else
                        request = requestBuilder.url(url).delete(requestBody).build();
                    break;
                case PUT:
                    request = requestBuilder.url(url).put(requestBody).build();
                    break;
                case HEAD:
                    request = requestBuilder.url(url).head().build();
                    break;
                default:
                    // default use get
                    request = requestBuilder.url(url).get().build();
            }
            return request;
        }

        private void handleFailed(
                final LghOkRequestCallBack callback,
                final Request request,
                final Exception e)
        {
            if(mainHandle){
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null)
                            callback.onFailed(request,e);
                    }
                };
                handler.post(r);
                return;
            }
            if(callback != null)
                callback.onFailed(request,e);
        }

        private void handleRespone(
                final Call call,
                final LghOkRequestCallBack callback,
                final Response response,
                final Request request
        ) throws IOException
        {
            final LghResponse lghResponse = new LghResponse();
            lghResponse.body = response.body().string();
            lghResponse.headers = response.headers().toString();
            lghResponse.code = response.code();
            lghResponse.message = response.message();

            if(mainHandle){
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null)
                            callback.onResponse(call,lghResponse);
                    }
                };
                handler.post(runnable);
                return;
            }
            if(callback != null)
                callback.onResponse(call,lghResponse);
        }

        /** 配置函数 */
        public Builder uiThreadHandle(){
            this.mainHandle = true;
            return this;
        }

        public Builder setMultipartType(MediaType multipartType) {
            this.multipartType = multipartType;
            return this;
        }

        public Builder readTimeOut(int seconds){
            this.readTimeOut = seconds;
            return this;
        }

        public Builder writeTimeOut(int seconds){
            this.writeTimeOut = seconds;
            return this;
        }

        public Builder customRequestBody(RequestBody requestBody){
            this.customRequestBody = requestBody;
            return this;
        }
        
        public Builder async(){
            this.background = true;
            return this;
        }

        public Builder sync(){
            this.background = false;
            return this;
        }

        public Builder url(String url){
            this.url = url;
            return this;
        }

        public Builder json(String json){
            this.json = json;
            return this;
        }

        public Builder cookies(String cookies){
            this.cookies = cookies;
            return this;
        }

        public Builder headers(String[] key,String[] values){
            this.headersKey    = key;
            this.headersValues = values;
            return this;
        }

        public Builder formData(String[] keys,String[] values){
            this.keys   = keys;
            this.values = values;
            return this;
        }

        public Builder files(RequestFile[] files){
            this.files = files;
            return this;
        }

        public Builder method(Method method){
            this.method = method;
            return this;
        }

        private void setOkHttpsConfiguration(Context context,final OkHttpsConfiguration configuration){
            if(context == null || configuration == null){
                return;
            }
            // 开启 https
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    Log.e("lghzzz",hostname);
                    return hostname.trim().equals(configuration.verifyHost);
                }
            };
            okBuilder.hostnameVerifier(hostnameVerifier);
            okBuilder.sslSocketFactory(
                    HttpsUtils.getSslSocketFactory(
                            new InputStream[]{
                                    context.getResources().openRawResource(configuration.serverFileCerId)
                            },
                            context.getResources().openRawResource(configuration.clientFileBksId),
                            configuration.password
                    ).sSLSocketFactory
            );
        }
    }

    private static class OkHttpsConfiguration {

        public String verifyHost;
        public String password;
        public int serverFileCerId;
        public int clientFileBksId;

        public OkHttpsConfiguration(String verifyHost, String password,int serverFileCerId,int clientFileBksId){
            this.verifyHost = verifyHost;
            this.password = password;
            this.serverFileCerId = serverFileCerId;
            this.clientFileBksId = clientFileBksId;
        }
    }

}
