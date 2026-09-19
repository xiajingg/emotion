package com.emotion.api;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.controller.UserController;
import com.emotion.api.dto.*;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.po.UserTextInteraction;
import com.emotion.api.service.IDrinkService;
import com.emotion.api.service.IUserService;
import com.emotion.api.service.WechatArticleService;
import com.emotion.api.service.WechatUserService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
class EmotionApplicationTests {

    @Autowired
    private IUserService userService;

    @Test
    public void contextLoads() {
//        User user = new User();
//        user.setUsername("xiajing");
//        user.setPassword("123456");
//        userMapper.insert(user);
        System.out.println(userService.count());
    }

    @Autowired
    private WechatArticleService wechatArticleService;

    @Test
    public void test2() {
        System.out.println(JSONUtil.toJsonStr(wechatArticleService.getLastArticle()));
    }


    @Test
    public void test3() {
        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setUserOpenId("ofG8O7fqvDJfA4tCNtVGgDvWiW88");
        PageResult<HistoryEmotion> userHistorySubmit = userService.getUserHistorySubmit(userPrincipal, 2, 5, 0);
        System.out.println(JSONUtil.toJsonStr(userHistorySubmit));
    }

    @Autowired
    private WechatUserService wechatUserService;

    @Test
    public void test4() {
        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setUserOpenId("ofG8O7fqvDJfA4tCNtVGgDvWiW88");
        boolean b = wechatUserService.handleShare("ofG8O7Xbf7NbW1qrNYh4Pg0CFU8g", 1L);
        System.out.println(b);
    }

    @Autowired
    private IDrinkService drinkService;

    @Test
    public void drinkTest4() {
        List<DrinkRecordDTO> drinkRecordByUserId = drinkService.getDrinkRecordByUserId(7L);
        System.out.println(JSONUtil.toJsonStr(drinkRecordByUserId));
    }

    @Autowired
    private UserController userController;

    @Test
    public void test5() {
        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setUserId(7L);
        userPrincipal.setUserOpenId("ofG8O7fqvDJfA4tCNtVGgDvWiW88");
        SubmitTextDTO text = new SubmitTextDTO();
        text.setText("我现在每天都在努力学习，争取早日找到一份工作，加油！努力！");
        BaseResult<Map> objectBaseResult = userController.submitText(userPrincipal, text);
        System.out.println(objectBaseResult);
        BaseResult<PageResult<HistoryEmotion>> userHistorySubmit
                = userController.getUserHistorySubmit(userPrincipal, 1, 10, 0);
        System.out.println(userHistorySubmit);
    }

    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;

    @Test
    public void checkUserTextInteractionData() {
        LambdaQueryWrapper<UserTextInteraction> wrapper = new LambdaQueryWrapper<>();
        List<UserTextInteraction> userTextInteractions = userTextInteractionMapper.selectList(wrapper);
        for (UserTextInteraction userTextInteraction : userTextInteractions) {
            String aiResponseStr = userTextInteraction.getAiResponse();
            AiResponse aiResponse = null;
            try {
                aiResponse = JSONUtil.toBean(aiResponseStr, AiResponse.class);
            } catch (Exception e) {
                System.out.println(userTextInteraction.getId());
            }
//            System.out.println(JSONUtil.toJsonStr(aiResponse));
        }
    }

    @Test
    public void test6() {
        String inputText = "今天我去海边户外烧烤, 超开心!";
        // 将text.getText()用英文逗号句号感叹号和中文句号逗号感叹号切割为多个string
        inputText = inputText.replace("。", ".");
        String[] splits = inputText.split("[,.!，。！、；;]");
        int length = splits.length;
        int totalScore = 0;
        // 遍历切割后的文字分别传给情绪AI接口
        for (int i = 0; i < splits.length; i++) {
            String s = splits[i];
            if (s.length() <= 4 && splits.length > 1 && i != splits.length - 1) {
                s += ",";
                s += splits[i + 1];
                i++;
                length = length - 1;
            }
        }
    }


}
