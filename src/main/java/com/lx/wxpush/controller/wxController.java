package com.lx.wxpush.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lx.wxpush.utils.DateUtil;
import com.lx.wxpush.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.*;

@RestController
@RequestMapping("/wx")
public class wxController {

    @Value("${wx.config.appId}")
    private String appId;
    @Value("${wx.config.appSecret}")
    private String appSecret;
    @Value("${wx.config.templateId}")
    private String templateId;
    @Value("${wx.config.openid}")
    private String openid;
    @Value("${weather.config.appid}")
    private String weatherAppId;
    @Value("${weather.config.appSecret}")
    private String weatherAppSecret;
    @Value("${message.config.togetherDate}")
    private String togetherDate;
    @Value("${message.config.birthday}")
    private String birthday;
    @Value("${message.config.xiaoshibirthday}")
    private String vast_s_birthday;
    @Value("${message.config.message}")
    private String message;

    private String accessToken = "";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * 获取Token
     * 每天早上7：30执行推送
     * @return
     */
    @Scheduled(cron = "0 30 7 ? * *")
    @RequestMapping("/getAccessToken")
    public String getAccessToken() {
        //这里直接写死就可以，不用改，用法可以去看api
        String grant_type = "client_credential";
        //封装请求数据
        String params = "grant_type=" + grant_type + "&secret=" + appSecret + "&appid=" + appId+"&city=亳州";
        //发送GET请求
        String sendGet = HttpUtil.sendGet("https://api.weixin.qq.com/cgi-bin/token", params);
        // 解析相应内容（转换成json对象）
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(sendGet);
        logger.info("微信token响应结果=" + jsonObject1);
        //拿到accesstoken
        accessToken = (String) jsonObject1.get("access_token");
        return sendWeChatMsg(accessToken);
    }


    /**
     * 发送微信消息
     *
     * @return
     */
    public String sendWeChatMsg(String accessToken) {

        String[] openIds = openid.split(",");
        List<JSONObject> errorList = new ArrayList();
        for (String opedId : openIds) {
            JSONObject templateMsg = new JSONObject(new LinkedHashMap<>());

            templateMsg.put("touser", opedId);
            templateMsg.put("template_id", templateId);


            JSONObject first = new JSONObject();
            String date = DateUtil.formatDate(new Date(), "yyyy-MM-dd");
            String week = DateUtil.getWeekOfDate(new Date());
            String day = date + " " + week;
            first.put("value", day);
            first.put("color", "#EED016");


            String TemperatureUrl = "https://www.yiketianqi.com/free/day?appid=" + weatherAppId + "&appsecret=" + weatherAppSecret + "&city=亳州&unescape=1";
            String sendGet = HttpUtil.sendGet(TemperatureUrl, null);
            JSONObject temperature = JSONObject.parseObject(sendGet);
            String address = "无法识别";
            String tem_day = "无法识别"; //最高温度
            String tem_night = "无法识别"; //最低温度
            String weatherStatus = "";
            if (temperature.getString("city") != null) {
                tem_day = temperature.getString("tem_day") + "°";
                tem_night = temperature.getString("tem_night") + "°";
                address = temperature.getString("city");
                weatherStatus = temperature.getString("wea");
            }

            JSONObject city = new JSONObject();
            city.put("value", address);
            city.put("color", "#60AEF2");

            String weather = weatherStatus + ", 温度：" + tem_night + " ~ " + tem_day;


            JSONObject temperatures = new JSONObject();
            temperatures.put("value", weather);
            temperatures.put("color", "#44B549");

            JSONObject birthDate = new JSONObject();
            JSONObject vast_birthDate = new JSONObject();
            String birthDay = "无法识别";
            String birthDay2 = "无法识别";
            try {
                Calendar calendar = Calendar.getInstance();
                String newD = calendar.get(Calendar.YEAR) + "-" + birthday;
                String newD2 = calendar.get(Calendar.YEAR) + "-" + vast_s_birthday;
                birthDay = DateUtil.daysBetween(date, newD);
                birthDay2 = DateUtil.daysBetween(date, newD2);
                if (Integer.parseInt(birthDay) < 0) {
                    Integer newBirthDay = Integer.parseInt(birthDay) + 365;
                    birthDay = newBirthDay + "天";
                } else {
                    birthDay = birthDay + "天";
                }
                if (Integer.parseInt(birthDay2) < 0) {
                    Integer newBirthDay2 = Integer.parseInt(birthDay2) + 365;
                    birthDay2 = newBirthDay2 + "天";
                } else {
                    birthDay2 = birthDay2 + "天";
                }
            } catch (ParseException e) {
                logger.error("togetherDate获取失败" + e.getMessage());
            }
            birthDate.put("value", birthDay);
            birthDate.put("color", "#6EEDE2");
            vast_birthDate.put("value",birthDay2);
            vast_birthDate.put("color","#6EEDE2");


            JSONObject togetherDateObj = new JSONObject();
            String togetherDay = "";
            try {
                togetherDay = "第" + DateUtil.daysBetween(togetherDate, date) + "天";
            } catch (ParseException e) {
                logger.error("togetherDate获取失败" + e.getMessage());
            }
            togetherDateObj.put("value", togetherDay);
            togetherDateObj.put("color", "#FEABB5");

            JSONObject messageObj = new JSONObject();
            messageObj.put("value", message);
            messageObj.put("color", "#C79AD0");


            JSONObject data = new JSONObject(new LinkedHashMap<>());
            data.put("first", first);
            data.put("city", city);
            data.put("temperature", temperatures);
            data.put("togetherDate", togetherDateObj);
            data.put("birthDate", birthDate);
            data.put("vast_birthDate",vast_birthDate);
            data.put("message", messageObj);


            templateMsg.put("data", data);
            String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;

            String sendPost = HttpUtil.sendPost(url, templateMsg.toJSONString());
            JSONObject WeChatMsgResult = JSONObject.parseObject(sendPost);
            if (!"0".equals(WeChatMsgResult.getString("errcode"))) {
                JSONObject error = new JSONObject();
                error.put("openid", opedId);
                error.put("errorMessage", WeChatMsgResult.getString("errmsg"));
                errorList.add(error);
            }
            logger.info("sendPost=" + sendPost);
        }
        JSONObject result = new JSONObject();
        result.put("result", "success");
        result.put("errorData", errorList);
        return result.toJSONString();

    }

}
