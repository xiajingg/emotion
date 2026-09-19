package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.AddQuestionReq;
import com.emotion.api.service.IEnglishMainTextService;
import com.emotion.api.util.RoleUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/english")
public class EnglishController {

    @Autowired
    private IEnglishMainTextService englishMainTextService;

    /**
     * 增加题目
     */
    @PostMapping("/addQuestion")
    public BaseResult<Boolean> addQuestion(@CurrentUser UserPrincipal userPrincipal, @RequestBody AddQuestionReq addQuestionReq) {
        boolean b = RoleUtil.judgeAdmin(userPrincipal);
        if (!b) {
            return BaseResult.error("权限不足");
        }
        boolean t = englishMainTextService.addQuestion(addQuestionReq.getText());
        return BaseResult.success(t);
    }
}
