package com.github.catvod.api;

import com.github.catvod.bean.tianyi.ShareData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TianyiApiTest {

//    @Test
//    public void getShareData() {
//
//
//        ShareData shareData = QuarkApi.get().getShareData("https://pan.quark.cn/s/1e386295b8ca");
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//
//        System.out.println("getShareData--" + gson.toJson(shareData));
//    }

    @Test
    public void getShareData() throws Exception {

        com.github.catvod.bean.tianyi.ShareData shareData = TianyiApi.get().getShareData("https://cloud.189.cn/web/share?code=ZvEjUvq6FNr2", "");
        // TianyiApi.get().getVod(shareData);
        com.github.catvod.bean.tianyi.ShareData shareData1 = TianyiApi.get().getShareData("https://cloud.189.cn/web/share?code=2eyARfBzURZj（访问码：kz6y）", "");

        //  TianyiApi.get().getVod(shareData1);
        ShareData shareData2 = TianyiApi.get().getShareData("https://cloud.189.cn/t/ZvEjUvq6FNr2", "");
        // TianyiApi.get().getVod(shareData2);


    }


    @Test
    public void getVod() throws Exception {

        com.github.catvod.bean.tianyi.ShareData shareData1 = TianyiApi.get().getShareData("https://cloud.189.cn/web/share?code=qEVVjyqM7bY3（访问码：6iel）", "");
        TianyiApi api = TianyiApi.get();
        api.setCookie("apm_ct=20250326080123000;OPENINFO=33c28688ef52ce9e3a9ef87388047efbde5e3e2e4c7ef6ef267632468c7dfaf294ff59fa59d34801;apm_sid=02F59AEE89AF29D6420BBD8408003B99;apm_key=317D96407B91CFC7EDA9010FA963CB06;pageOp=8b7ecdae02246019e5b5c07d5775e568;apm_uid=CD70AFED168CA30CF75CDBF983D237C2;LT=358459209f24f17e;GUID=a72822a1f8574d2c97b8392c067e835c;SSON=dc466c8192e3109eaea837c1d136c1fd065253ce1c7d3a66ca1520d7d6d6307b10a1fe65c7becac73b95f24a6e681e654ec4f47c39533ebcc48bb78d6d6e63d1bbf3334e6e97eaa7092d34f87bf1209e791a623d703df58b667c93cf9745938a396cfcc4e795bb687b7e16255f08379edd4f03e64b2002aa915c3157b008d54ed80b1ad57bf6b7405d23e0763077999425d511e0ccc0e07a952221985bf9903d10b9f21c4d6c175b5e9fb20721ef5b2926290dda57af27ff65ad5df045c8a824bebb4dcec0cd08a68edfc462d5bcd7a180b80b072ca61aa87dd0ebe3946397f94f0bc28d24a56958;JSESSIONID=58737554E5FEB36C9AF67050CE292E38;COOKIE_LOGIN_USER=92F4CEE641F1363A0EA09AC7FA6B61FDB6DD036333EFCD7F28818C64A94CFACC7C6186180FCEE7A6BD5E2A597347DBE58BA1C72D1493EE0847FD4F5A;apm_ua=45747CD36C19E71509E38183EB8AAB8D");
        api.getVod(shareData1);


    }
}