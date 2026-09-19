package com.emotion.api;

import com.emotion.api.config.BaseResult;
import com.emotion.api.dto.SubmitTextDTO;
import com.emotion.api.service.IUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class ConvertImageTest {
    @Autowired
    private IUserService iUserService;

    @Test
    public void testConvertImage() {
        SubmitTextDTO submitTextDTO = new SubmitTextDTO();
        submitTextDTO.setText("帮我转换一下");
        BaseResult<String> stringBaseResult = iUserService.covertImage(1L, submitTextDTO);
        System.out.println(stringBaseResult);
    }
}
