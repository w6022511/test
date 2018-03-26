package org.eking.tourism.module.autologin.controller;

import net.sf.json.JSONObject;
import org.eking.tourism.common.constant.WeChatAPIConstant;
import org.eking.tourism.common.utils.HttpUtil;
import org.eking.tourism.module.user.service.UserService;
import org.eking.tourism.repository.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
public class AutoLogin {

    @Value("${wechat.AppID}")
    private String APPID;

    @Value("${wechat.AppSecret}")
    String APPSECRET;

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public void getCode(HttpServletResponse response){
        //System.err.println(APPID);
        String url = WeChatAPIConstant.USER_AUTHORIZE_URL;

        String backUrl = "http://wjxqvx.natappfree.cc/wechat/callBack";

        String scope = "snsapi_userinfo";

        String newUrl = url.replace("APPID",APPID)
                .replace("REDIRECT_URI", URLEncoder.encode(backUrl))//回调地址，必须能在公网访问
                .replace("SCOPE",scope);
        try {
            System.err.println(newUrl);
            response.sendRedirect(newUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    *
    *获取授权code后回调,获取accessToken和openId,再拉取用户信息
    *@param
    *@return
    *
    */
    @GetMapping("/callBack")
    public JSONObject callBack(HttpServletRequest request,HttpServletResponse response){

        String code = request.getParameter("code");
        //获取access_token和openid
        String url = WeChatAPIConstant.LOGIN_ACCESS_TOKEN_URL.replace("APPID",APPID)
                .replace("SECRET",APPSECRET)
                .replace("CODE",code);

        JSONObject jsonObject = HttpUtil.doGet(url);

        String openid = jsonObject.getString("openid");
        String access_token = jsonObject.getString("access_token");

        //拉取用户信息
        String infoUrl = WeChatAPIConstant.GET_USER_INFO_URL.replace("ACCESS_TOKEN",access_token)
                .replace("OPENID",openid);

        JSONObject userInfo = HttpUtil.doGet(infoUrl);

        //解决返回的用户信息中文乱码
        String result = null;
        try {
             result = new String(userInfo.toString().getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject userInfoResult = JSONObject.fromObject(result);

        //判断数据有无此openID，没有则新建
        if(userService.getUserByOpenId(openid) == null){
            User user = new User();
            user.setOpenId(userInfoResult.getString("openid"));
            user.setNickName(userInfoResult.getString("nickname"));
            user.setSex(userInfoResult.getString("sex"));
            user.setHeadImage(userInfoResult.getString("headimgurl"));
            userService.createUser(user);
        }
        return userInfoResult;

    }
}
