package com.emotion.api.service.impl;

import com.emotion.api.fanyi.TransApi;
import com.emotion.api.repository.po.EnglishMainText;
import com.emotion.api.repository.dao.rds.EnglishMainTextMapper;
import com.emotion.api.repository.po.EnglishWordQuestions;
import com.emotion.api.service.IEnglishMainTextService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.service.IEnglishWordQuestionsService;
import com.emotion.api.util.QianfanAI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 存储英文学习材料的主表 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishMainTextServiceImpl extends ServiceImpl<EnglishMainTextMapper, EnglishMainText> implements IEnglishMainTextService {

    @Autowired
    private IEnglishWordQuestionsService englishWordQuestionsService;

    @Override
    public boolean addQuestion(String text) {
        // 保存主表
        EnglishMainText mainText = new EnglishMainText();
        mainText.setTextContent(text);
        this.save(mainText);
        // 提取单词
        String[] words = extracted(text);
        for (int i = 0; i < words.length; i++) {
            EnglishWordQuestions englishWordQuestions = new EnglishWordQuestions();
            englishWordQuestions.setMainTextId(mainText.getId());
            englishWordQuestions.setWord(words[i]);
            String transResult = TransApi.getTransResult(words[i], "auto", "zh");
            englishWordQuestions.setChineseTranslation(transResult);
            englishWordQuestionsService.save(englishWordQuestions);
        }
        // 保存句子结构

        return true;
    }


    /**
     * 抽取单词,
     *
     * @param text
     * @return
     */
    public static String[] extracted(String text) {
        String systemContent = "你是一个英语机器人, 你擅长发现一段英语短文里面的相对较难的单词, 你要以json的格式把单词返回给我, 请把返回的10个单词写在此处{ }";
        String s = QianfanAI.executeChat(systemContent, text);
        // 如果s包含数组,则提取数组的数据, 然后去掉换行符
        // 判断字符串 s 是否包含 '['，表示可能存在键值对列表
        if (s.contains("[")) {
            // 根据 '[' 分割字符串，提取出键值对列表部分
            String[] split = s.split("\\[");
            // 进一步根据 ']' 分割，提取出键值对列表的内容
            String[] split1 = split[1].split("]");
            // 根据 ',' 分割键值对列表，获取每个键值对
            String[] split2 = split1[0].split(",");
            // 初始化一个字符串数组，用于存储处理后的键值对
            String[] result = new String[split2.length];
            // 遍历每个键值对进行处理
            for (int i = 0; i < split2.length; i++) {
                // 去除键值对中的换行、首尾的空格、引号、'{' 和 '}' 字符，确保键值对字符串的纯净性
                result[i] = split2[i].replaceAll("\\n", "").trim().replaceAll("\"", "").replaceAll("\\{", "").replaceAll("\\}", "");
            }
            // 返回处理后的键值对数组
            return result;
        }


        return new String[0];
    }

    public static void main(String[] args) {
        extracted("请你提取下面一段英文中相对较难的10个单词: A bank robber stole a lot of money. He was caught and sent to prison, but the money was never found. When he came out of prison, they watched him to see what he would do.Here is the detective, reporting to the inspector. 'Yes, sir, I found Johnny . I followed him all around the town, but frankly, I couldn't make anything out of what he bought. Here's the list.'");
    }
}
