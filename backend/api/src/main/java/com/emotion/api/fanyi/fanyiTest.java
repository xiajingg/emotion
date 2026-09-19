package com.emotion.api.fanyi;


public class fanyiTest {

    // 在平台申请的APP_ID 详见 http://api.fanyi.baidu.com/api/trans/product/desktop?req=developer
    private static final String APP_ID = "";
    private static final String SECURITY_KEY = "";

    public static void main(String[] args) {
        String query = "高度600米";
        System.out.println(TransApi.getTransResult(query, "auto", "zh"));
    }

}
