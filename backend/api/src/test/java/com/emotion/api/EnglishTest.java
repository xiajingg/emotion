package com.emotion.api;

import com.emotion.api.service.IEnglishMainTextService;
import com.emotion.api.service.impl.EnglishMainTextServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class EnglishTest {

    @Autowired
    private IEnglishMainTextService englishMainTextService;

    @Test
    public void test1() {
//        String text = "People often says that a dog is man's best friend. Over thousands of years, man has taught his dogs to do many kinds of work besides guarding the home. For example, sheepdogs are famous for their ability to control a flock of hundreds of sheep.";
//        englishMainTextService.(text);
        EnglishMainTextServiceImpl.extracted("People often says that a dog is man's best friend. Over thousands of years, man has taught his dogs to do many kinds of work besides guarding the home. For example, sheepdogs are famous for their ability to control a flock of hundreds of sheep.");
    }
}
