package com.iteima.mysong.config;

import com.google.code.kaptcha.text.TextProducer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Random;


public class MathExpressionCreator implements TextProducer {

    @Override
    public String getText() {
        Random random = new Random();
        int a = random.nextInt(10);
        int b = random.nextInt(10);
        int operator = random.nextInt(3); // 0:+, 1:-, 2:*

        String expression;
        int result;

        switch(operator) {
            case 0:
                expression = a + "+" + b + "=?";
                result = a + b;
                break;
            case 1:
                expression = a + "-" + b + "=?";
                result = a - b;
                break;
            case 2:
                expression = a + "×" + b + "=?";
                result = a * b;
                break;
            default:
                expression = a + "+" + b + "=?";
                result = a + b;
        }

        // 将结果存储在session中
//        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
//        attr.getRequest().getSession().setAttribute("captcha", String.valueOf(result));
//        System.out.println(expression+"--------"+result);
        return expression+"@"+result;
    }
}
