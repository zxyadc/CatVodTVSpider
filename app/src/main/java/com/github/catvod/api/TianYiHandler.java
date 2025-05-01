package com.github.catvod.api;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.catvod.bean.tianyi.Cache;
import com.github.catvod.bean.tianyi.User;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.net.OkResult;
import com.github.catvod.spider.Init;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Notify;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.QRCode;
import com.github.catvod.utils.ResUtil;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class TianYiHandler {

    public static final String API_URL = "https://open.e.189.cn";
    private ScheduledExecutorService service;
    private AlertDialog dialog;
    private Cache cache = null;

    public File getCache() {
        return Path.tv("tianyi");
    }

    public File geteCache() {
        return Path.tv("tianyie");
    }

    private String indexUrl = "";

    private String reqId;
    private String lt;

    private SimpleCookieJar cookieJar;

    public SimpleCookieJar getCookieJar() {
        return cookieJar;
    }

    public TianYiHandler() {

        cookieJar = new SimpleCookieJar();
        cache = Cache.objectFrom(Path.read(getCache()));

    }

    public void cleanCookie() {

        cache.setTianyiUser(new User(""));
    }

    private Map<String, String> getHeader(String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Cookie", cookieJar.loadForRequest(url));
        return headers;
    }

    public void refreshCookie() throws IOException {

        String url = "https://cloud.189.cn/api/portal/loginUrl.action?redirectURL=https%3A%2F%2Fcloud.189.cn%2Fweb%2Fredirect.html&defaultSaveName=3&defaultSaveNameCheck=uncheck&browserId=16322f24d9405fb83331c3f6ce971b53";
        String index = OkHttp.getLocation(url, getHeader(url));
        SpiderDebug.log("unifyAccountLogin：" + index);

        Map<String, List<String>> resHeaderMap = OkHttp.getLocationHeader(index, getHeader(index));
        saveCookie(resHeaderMap.get("Set-Cookie"), index);
        indexUrl = resHeaderMap.get("Location").get(0);
        SpiderDebug.log("callbackUnify: " + indexUrl);

        Map<String, List<String>> callbackUnify = OkHttp.getLocationHeader(indexUrl, getHeader(indexUrl));
        saveCookie(callbackUnify.get("Set-Cookie"), indexUrl);
        SpiderDebug.log("refreshCookie header：" + Json.toJson(callbackUnify));

    }

    /*
     * 保存cookie
     *
     * @param cookie
     * @param url
     */
    private void saveCookie(List<String> cookie, String url) {
        if (cookie != null && cookie.size() > 0) {
            cookieJar.saveFromResponse(url, cookie);
        }
    }

    public byte[] startScan() throws Exception {


        SpiderDebug.log("index ori: " + "https://cloud.189.cn/api/portal/loginUrl.action?redirectURL=https%3A%2F%2Fcloud.189.cn%2Fweb%2Fredirect.html&defaultSaveName=3&defaultSaveNameCheck=uncheck&browserId=dff95dced0b03d9d972d920f03ddd05e");
        String index = OkHttp.getLocation("https://cloud.189.cn/api/portal/loginUrl.action?redirectURL=https://cloud.189.cn/web/redirect.html&defaultSaveName=3&defaultSaveNameCheck=uncheck&browserId=8d38da4344fba4699d13d6e6854319d7", Map.of("Cookie", ""));
        SpiderDebug.log("index red: " + index);
        Map<String, List<String>> resHeaderMap = OkHttp.getLocationHeader(index, getHeader(index));

        saveCookie(resHeaderMap.get("Set-Cookie"), index);

        indexUrl = resHeaderMap.get("Location").get(0);
        SpiderDebug.log("indexUrl red: " + indexUrl);

        HttpUrl httpParams = HttpUrl.parse(indexUrl);
        reqId = httpParams.queryParameter("reqId");
        lt = httpParams.queryParameter("lt");

        Result result = appConf();

        // Step 1: Get UUID
        JsonObject uuidInfo = getUUID();
        String uuid = uuidInfo.get("uuid").getAsString();
        String encryuuid = uuidInfo.get("encryuuid").getAsString();
        String encodeuuid = uuidInfo.get("encodeuuid").getAsString();

        // Step 2: Get QR Code
        byte[] byteStr = downloadQRCode(encodeuuid, reqId);

        Init.run(() -> showQRCode(byteStr));
        // Step 3: Check login status
        // return
        Init.execute(() -> startService(uuid, encryuuid, reqId, lt, result.paramId, result.returnUrl));
        /*Map<String, Object> result = new HashMap<>();
        result.put("qrcode", "data:image/png;base64," + qrCode);
        result.put("status", "NEW");*/
        return byteStr;

    }


    private String api(String url, Map<String, String> params, Map<String, String> headers, Integer retry, String method) throws InterruptedException {


        int leftRetry = retry != null ? retry : 3;

        OkResult okResult;
        if ("GET".equals(method)) {
            okResult = OkHttp.get(this.API_URL + url, params, headers);
        } else {
            okResult = OkHttp.post(this.API_URL + url, params, headers);
        }
        if (okResult.getResp().get("Set-Cookie") != null) {
            saveCookie(okResult.getResp().get("Set-Cookie"), this.API_URL);
        }

        if (okResult.getCode() != 200 && leftRetry > 0) {
            SpiderDebug.log("请求" + url + " failed;");
            Thread.sleep(1000);
            return api(url, params, headers, leftRetry - 1, method);
        }
        SpiderDebug.log("请求" + url + " 成功;" + "返回结果:" + okResult.getBody());
        return okResult.getBody();
    }

    /**
     * 获取appConf
     *
     * @param
     * @return
     */

    private @NotNull Result appConf() throws Exception {
        Map<String, String> tHeaders = getHeader(API_URL);
        tHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        tHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/76.0");
        tHeaders.put("Referer", indexUrl);
        tHeaders.put("origin", API_URL);
        tHeaders.put("Lt", lt);
        tHeaders.put("Reqid", reqId);

        Map<String, String> param = new HashMap<>();

        param.put("version", "2.0");
        param.put("appKey", "cloud");
        String paramId;
        String returnUrl;
        String body = api("/api/logbox/oauth2/appConf.do", param, tHeaders, 3, "POST");

        paramId = Json.safeObject(body).get("data").getAsJsonObject().get("paramId").getAsString();
        returnUrl = Json.safeObject(body).get("data").getAsJsonObject().get("returnUrl").getAsString();

        SpiderDebug.log("paramId: " + paramId);
        SpiderDebug.log("returnUrl: " + returnUrl);
        return new Result(paramId, returnUrl);
    }

    public void setCookie(JsonObject obj) {
        cookieJar.setGlobalCookie(obj);
    }

    private static class Result {
        public final String paramId;
        public final String returnUrl;

        public Result(String paramId, String returnUrl) {
            this.paramId = paramId;
            this.returnUrl = returnUrl;
        }
    }


    public JsonObject getUUID() throws InterruptedException {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "cloud");
        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51 Safari/537.36");
        headers.put("lt", lt);
        headers.put("reqId", reqId);
        headers.put("referer", indexUrl);


        String body = api("/api/logbox/oauth2/getUUID.do", params, headers, 3, "POST");
        return Json.safeObject(body);

    }

    public byte[] downloadQRCode(String uuid, String reqId) throws IOException {


        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51 Safari/537.36");

        headers.put("referer", indexUrl);

        //  OkResult okResult = OkHttp.get("https://open.e.189.cn/api/logbox/oauth2/image.do", params, headers);
//.addQueryParameter("uuid", uuid).addQueryParameter("REQID", reqId)
        HttpUrl url = HttpUrl.parse(API_URL + "/api/logbox/oauth2/image.do?uuid=" + uuid + "&REQID=" + reqId).newBuilder().build();

        Request request = new Request.Builder().url(url).headers(Headers.of(headers)).build();
        Response response = OkHttp.newCall(request);
        if (response.code() == 200) {
            return response.body().bytes();
        }
        return null;
    }


    private Map<String, Object> checkLoginStatus(String uuid, String encryuuid, String reqId, String lt, String paramId, String returnUrl) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("appId", "cloud");
        params.put("encryuuid", encryuuid);
        params.put("uuid", uuid);
        params.put("date", DateFormatUtils.format(new Date(), "yyyy-MM-ddHH:mm:ss") + new Random().nextInt(24));
        params.put("returnUrl", URLEncoder.encode(returnUrl, "UTF-8"));
        params.put("clientType", "1");
        params.put("timeStamp", (System.currentTimeMillis() / 1000 + 1) + "000");
        params.put("cb_SaveName", "0");
        params.put("isOauth2", "false");
        params.put("state", "");
        params.put("paramId", paramId);
        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51 Safari/537.36");
        headers.put("referer", indexUrl);
        headers.put("Reqid", reqId);

        String body = api("/api/logbox/oauth2/qrcodeLoginState.do", params, headers, 3, "POST");
        //  OkResult okResult = OkHttp.post(API_URL + "/api/logbox/oauth2/qrcodeLoginState.do", params, headers);
        SpiderDebug.log("qrcodeLoginState result------" + body);

        JsonObject obj = Json.safeObject(body).getAsJsonObject();
        if (Objects.nonNull(obj.get("status")) && obj.get("status").getAsInt() == 0) {

            SpiderDebug.log("扫码成功------" + obj.get("redirectUrl").getAsString());
            String redirectUrl = obj.get("redirectUrl").getAsString();


            fetchUserInfo(redirectUrl);


        } else {
            SpiderDebug.log("扫码失败------" + body);
        }


        return null;
    }

    private void fetchUserInfo(String redirectUrl) throws IOException {


        Map<String, List<String>> okResult = OkHttp.getLocationHeader(redirectUrl, getHeader(redirectUrl));
        saveCookie(okResult.get("Set-Cookie"), redirectUrl);
        SpiderDebug.log("扫码返回数据：" + Json.toJson(okResult));
        if (okResult.containsKey("Set-Cookie")) {

            //停止检验线程，关闭弹窗
            stopService();
        }


       /* if (okResult.getCode() == 200) {
            okResult.getBody();
        }*/
        return;

    }


    /**
     * 显示qrcode
     *
     * @param bytes
     */
    public void showQRCode(byte[] bytes) {
        try {
            int size = ResUtil.dp2px(240);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            ImageView image = new ImageView(Init.context());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(QRCode.Bytes2Bimap(bytes));
            FrameLayout frame = new FrameLayout(Init.context());
            params.gravity = Gravity.CENTER;
            frame.addView(image, params);
            dialog = new AlertDialog.Builder(Init.getActivity()).setView(frame).setOnCancelListener(this::dismiss).setOnDismissListener(this::dismiss).show();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Notify.show("请使用天翼网盘App扫描二维码");
        } catch (Exception ignored) {
        }
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private void dismiss(DialogInterface dialog) {
        stopService();
    }

    private void stopService() {
        if (service != null) service.shutdownNow();
        Init.run(this::dismiss);
    }

    public void startService(String uuid, String encryuuid, String reqId, String lt, String paramId, String returnUrl) {
        SpiderDebug.log("----start  checkLoginStatus  service");

        service = Executors.newScheduledThreadPool(1);

        service.scheduleWithFixedDelay(() -> {
            SpiderDebug.log("----checkLoginStatus ing....");
            try {
                checkLoginStatus(uuid, encryuuid, reqId, lt, paramId, returnUrl);
            } catch (Exception e) {
                SpiderDebug.log("----checkLoginStatus error" + e.getMessage());
                throw new RuntimeException(e);
            }

        }, 1, 3, TimeUnit.SECONDS);
    }

}