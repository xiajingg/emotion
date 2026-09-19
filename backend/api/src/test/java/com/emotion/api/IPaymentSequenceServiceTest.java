package com.emotion.api;

import com.emotion.api.service.IPaymentSequenceService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class IPaymentSequenceServiceTest {

    @Autowired
    private IPaymentSequenceService paymentSequenceService;

    @Test
    public void testGetPrePaySequence() {
        System.out.println(paymentSequenceService.getPrePaySequence());
    }
}
