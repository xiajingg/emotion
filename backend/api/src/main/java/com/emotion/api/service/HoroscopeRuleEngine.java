package com.emotion.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 占星规则引擎 - 负责原始天文数据计算与专业规则解读
 */
@Slf4j
@Service
public class HoroscopeRuleEngine {

    /**
     * 生成三层结构的占星数据：原始分析、规则解读
     *
     * @param positions 天体黄经数据
     * @return 包含 astroAnalysis(原始数据) 和 content(规则解读) 的 Map
     */
    public Map<String, String> generateHoroscopeData(Map<String, Double> positions) {
        return generateHoroscopeData(positions, null);
    }

    /**
     * 生成三层结构的占星数据：原始分析、规则解读（支持指定星座）
     *
     * @param positions 天体黄经数据
     * @param zodiacSign 星座名称（英文），如果为 null 则生成通用解读
     * @return 包含 astroAnalysis(原始数据) 和 content(规则解读) 的 Map
     */
    public Map<String, String> generateHoroscopeData(Map<String, Double> positions, String zodiacSign) {
        StringBuilder rawAnalysis = new StringBuilder();
        StringBuilder ruleContent = new StringBuilder();
        
        // 🔑 提取全部 10 个天体的黄经度数
        double sunLon = positions.getOrDefault("Sun", 0.0);
        double moonLon = positions.getOrDefault("Moon", 0.0);
        double mercuryLon = positions.getOrDefault("Mercury", 0.0);
        double venusLon = positions.getOrDefault("Venus", 0.0);
        double marsLon = positions.getOrDefault("Mars", 0.0);
        double jupiterLon = positions.getOrDefault("Jupiter", 0.0);
        double saturnLon = positions.getOrDefault("Saturn", 0.0);
        double uranusLon = positions.getOrDefault("Uranus", 0.0);
        double neptuneLon = positions.getOrDefault("Neptune", 0.0);
        double plutoLon = positions.getOrDefault("Pluto", 0.0);
        
        // 计算关键相位
        double moonVenusAngle = calculateAspectAngle(moonLon, venusLon);
        double sunMercuryAngle = calculateAspectAngle(sunLon, mercuryLon);
        double marsJupiterAngle = calculateAspectAngle(marsLon, jupiterLon);

        // 1. 面向用户展示的星象解码：只保留可理解的关键词，不展示完整黄经数据
        rawAnalysis.append(String.format(
            "今日重点：太阳在%s，月亮在%s，金星在%s。\n",
            getZodiacSignByDegree(sunLon),
            getZodiacSignByDegree(moonLon),
            getZodiacSignByDegree(venusLon)
        ));
        rawAnalysis.append(String.format(
            "情绪提示：月金%s，适合把感受说清楚；日水%s，先确认信息再回应。",
            getPhaseName(moonVenusAngle),
            getPhaseName(sunMercuryAngle)
        ));
        if (marsJupiterAngle < 65) {
            rawAnalysis.append(String.format("行动节奏：火木%s，适合推进一件小事。", getPhaseName(marsJupiterAngle)));
        }
        
        // 2. 规则引擎解析 (content) - 根据星座生成个性化解读
        int sunSignIndex = (int) (sunLon / 30);
        String[] signs = {"白羊", "金牛", "双子", "巨蟹", "狮子", "处女", "天秤", "天蝎", "射手", "摩羯", "水瓶", "双鱼"};
        
        // 🔑 修复：根据传入的 zodiacSign 参数确定当前分析的星座，而不是使用太阳位置
        String currentSignChinese = "";
        int currentSignIndex = -1;
        
        if (zodiacSign != null && !zodiacSign.isEmpty()) {
            // 映射英文星座名到中文
            java.util.Map<String, String> signMapping = new java.util.HashMap<>();
            signMapping.put("Aries", "白羊");
            signMapping.put("Taurus", "金牛");
            signMapping.put("Gemini", "双子");
            signMapping.put("Cancer", "巨蟹");
            signMapping.put("Leo", "狮子");
            signMapping.put("Virgo", "处女");
            signMapping.put("Libra", "天秤");
            signMapping.put("Scorpio", "天蝎");
            signMapping.put("Sagittarius", "射手");
            signMapping.put("Capricorn", "摩羯");
            signMapping.put("Aquarius", "水瓶");
            signMapping.put("Pisces", "双鱼");
            currentSignChinese = signMapping.getOrDefault(zodiacSign, "");
            
            // 找到当前星座的索引
            String[] englishSigns = {"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", 
                                    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"};
            for (int i = 0; i < englishSigns.length; i++) {
                if (englishSigns[i].equals(zodiacSign)) {
                    currentSignIndex = i;
                    break;
                }
            }
        }
        
        // 🔑 修复：以用户星座为中心生成个性化内容，而非太阳所在星座
        if (!currentSignChinese.isEmpty() && currentSignIndex >= 0) {
            ruleContent.append(String.format("【%s座专属运势】\n", currentSignChinese));
            ruleContent.append(String.format("今日太阳位于%s座区域，对你的影响如下：\n", signs[sunSignIndex]));
        } else {
            // 如果没有指定星座，使用原来的逻辑
            ruleContent.append(String.format("今日太阳位于%s座区域。", signs[sunSignIndex]));
        }
        
        // 根据月金相位生成情感解读
        if (moonVenusAngle < 30) {
            ruleContent.append("由于月亮与金星呈和谐相位，今日情感能量流动顺畅，人际互动将带来愉悦感。");
        } else if (moonVenusAngle > 150) {
            ruleContent.append("受月金对冲相位影响，情绪波动可能较大，建议在决策前多一分冷静。");
        }
        
        // 🔑 修复：根据用户星座与各行星的关系生成个性化建议
        if (currentSignIndex >= 0) {
            // 火星影响：行动力、竞争、勇气
            int marsSignIndex = (int) (marsLon / 30);
            if (marsSignIndex == currentSignIndex) {
                ruleContent.append("火星位于你的星座，赋予你强大的行动力和决断力，适合开启新计划或迎接挑战。");
            } else if (Math.abs(marsSignIndex - currentSignIndex) == 6) {
                ruleContent.append("火星与你的星座呈对冲相位，能量强烈但需注意平衡，避免过度冲动或与他人冲突。");
            } else if (Math.abs(marsSignIndex - currentSignIndex) == 4 || Math.abs(marsSignIndex - currentSignIndex) == 8) {
                ruleContent.append("火星与你形成三分相，行动力充沛且方向明确，适合推进重要事务。");
            }
            
            // 木星影响：幸运、扩张、机遇
            int jupiterSignIndex = (int) (jupiterLon / 30);
            if (jupiterSignIndex == currentSignIndex) {
                ruleContent.append("木星守护你的星座，今日运势格外顺遂，容易获得意外机遇和贵人相助。");
            } else if (Math.abs(jupiterSignIndex - currentSignIndex) == 4 || Math.abs(jupiterSignIndex - currentSignIndex) == 8) {
                ruleContent.append("木星与你形成和谐相位，带来好运和成长机会，适合学习拓展或旅行探索。");
            }
            
            // 土星影响：责任、限制、成熟
            int saturnSignIndex = (int) (saturnLon / 30);
            if (saturnSignIndex == currentSignIndex) {
                ruleContent.append("土星位于你的星座，带来责任感和压力，但也促使你建立稳固的基础和长期规划。");
            } else if (Math.abs(saturnSignIndex - currentSignIndex) == 6) {
                ruleContent.append("土星与你呈对冲相位，可能面临外界压力或考验，需要耐心和坚持来克服困难。");
            }
            
            // 金星影响：爱情、美学、人际关系
            int venusSignIndex = (int) (venusLon / 30);
            if (venusSignIndex == currentSignIndex) {
                ruleContent.append("金星守护你的星座，魅力值飙升，适合社交约会或艺术创作，人缘极佳。");
            } else if (Math.abs(venusSignIndex - currentSignIndex) == 4 || Math.abs(venusSignIndex - currentSignIndex) == 8) {
                ruleContent.append("金星与你形成吉相，人际关系和谐，适合表达爱意或享受美好时光。");
            }
            
            // 水星影响：沟通、思维、学习
            int mercurySignIndex = (int) (mercuryLon / 30);
            if (mercurySignIndex == currentSignIndex) {
                ruleContent.append("水星位于你的星座，思维敏捷清晰，适合学习新知、沟通交流或处理文书工作。");
            }
        } else {
            // 原来的通用逻辑
            int marsSignIndex = (int) (marsLon / 30);
            int zodiacIndex = sunSignIndex; // 使用太阳位置作为默认
            if (marsSignIndex == zodiacIndex || Math.abs(marsSignIndex - zodiacIndex) == 6) {
                ruleContent.append("火星能量与你的星座形成特殊连接，今日行动力显著增强，适合主动出击。");
            }
            
            // 根据木星位置添加幸运提示
            int jupiterSignIndex = (int) (jupiterLon / 30);
            if (jupiterSignIndex == zodiacIndex) {
                ruleContent.append("木星守护你的星座，今日运势格外顺遂，容易获得意外机遇。");
            }
        }
        
        // 根据星座添加个性化建议
        if (zodiacSign != null) {
            String personalizedAdvice = getZodiacSpecificAdvice(zodiacSign, moonVenusAngle, sunLon, moonLon, marsLon, jupiterLon);
            if (personalizedAdvice != null && !personalizedAdvice.isEmpty()) {
                ruleContent.append(personalizedAdvice);
            }
        }
        
        log.info("生成占星数据: 原始分析={}, 规则解读={}", rawAnalysis, ruleContent);
        return java.util.Map.of(
            "astro_analysis", rawAnalysis.toString(),
            "content", ruleContent.toString()
        );
    }

    /**
     * 计算两个天体之间的相位角度（取最小夹角）
     */
    private double calculateAspectAngle(double lon1, double lon2) {
        double angle = Math.abs(lon1 - lon2) % 360;
        if (angle > 180) angle = 360 - angle;
        return angle;
    }
    
    /**
     * 根据黄经度数获取星座名称
     */
    private String getZodiacSignByDegree(double degree) {
        String[] signs = {"白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", 
                          "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"};
        int index = (int) (degree / 30) % 12;
        return signs[index];
    }

    private String getPhaseName(double angle) {
        if (angle < 10) return "合相";
        if (angle < 30) return "半合相";
        if (angle < 60) return "六合相";
        if (angle < 90) return "刑相";
        if (angle < 120) return "拱相";
        if (angle < 180) return "冲相";
        return "合相";
    }

    /**
     * 根据星座和天体位置生成个性化建议（增强版：使用更多行星数据）
     */
    private String getZodiacSpecificAdvice(String sign, double moonVenusAngle, double sunLon, double moonLon, double marsLon, double jupiterLon) {
        // 计算星座索引
        int sunSignIndex = (int) (sunLon / 30);
        String[] signs = {"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", 
                          "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"};
        
        // 查找当前星座的索引
        int currentIndex = -1;
        for (int i = 0; i < signs.length; i++) {
            if (signs[i].equals(sign)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex == -1) return "";
        
        //  关键修复：加入月亮影响，月亮每天移动约13°，是每日差异的主要来源
        int moonSignIndex = (int) (moonLon / 30);
        StringBuilder advice = new StringBuilder();
        
        // 月亮与当前星座的关系
        if (moonSignIndex == currentIndex) {
            advice.append("月亮位于你的星座，情绪敏感度高，直觉敏锐，适合倾听内心声音。");
        } else if (Math.abs(moonSignIndex - currentIndex) == 6) {
            advice.append("月亮与你的星座呈对冲相位，情绪容易波动，建议保持稳定心态，避免冲动决策。");
        } else if (Math.abs(moonSignIndex - currentIndex) == 4 || Math.abs(moonSignIndex - currentIndex) == 8) {
            advice.append("月亮与你形成三分相，内心平静和谐，情绪稳定，适合做出重要决定。");
        } else if (Math.abs(moonSignIndex - currentIndex) == 3 || Math.abs(moonSignIndex - currentIndex) == 9) {
            advice.append("月亮与你呈刑相位，内心可能有些焦虑或不安，建议多做放松活动来调整状态。");
        }
        
        // 根据月金相位补充情感建议
        if (moonVenusAngle < 30) {
            advice.append("月金和谐相位带来温暖的人际氛围，适合约会或与亲友相聚。");
        } else if (moonVenusAngle > 150) {
            advice.append("月金对冲相位可能带来情感上的小摩擦，多一份包容会让关系更融洽。");
        }
        
        // 结合星座特性和月亮位置给出建议
        String baseAdvice = switch (sign) {
            case "Aries" -> "白羊座今日活力充沛，适合开启新计划";
            case "Taurus" -> "金牛座今日财运稳定，适合处理财务事务";
            case "Gemini" -> "双子座今日思维活跃，适合沟通交流";
            case "Cancer" -> "巨蟹座今日情感细腻，适合陪伴家人";
            case "Leo" -> "狮子座今日魅力四射，适合展示才华";
            case "Virgo" -> "处女座今日注重细节，适合整理规划";
            case "Libra" -> "天秤座今日人际关系和谐，适合合作协商";
            case "Scorpio" -> "天蝎座今日洞察力强，适合深度思考";
            case "Sagittarius" -> "射手座今日向往自由，适合学习探索";
            case "Capricorn" -> "摩羯座今日务实稳重，适合推进工作";
            case "Aquarius" -> "水瓶座今日创意迸发，适合创新尝试";
            case "Pisces" -> "双鱼座今日感性丰富，适合艺术创作";
            default -> "";
        };
        
        if (!baseAdvice.isEmpty()) {
            // 如果有月亮影响，追加到基础建议之后
            if (advice.length() > 0) {
                return baseAdvice + "，" + advice.toString();
            } else {
                // 没有月亮特殊相位时，根据月金相位补充
                if (moonVenusAngle < 60) {
                    return baseAdvice + "，月金和谐带来好心情，保持积极心态会有意外收获。";
                } else if (moonVenusAngle < 120) {
                    return baseAdvice + "，情绪平稳，按部就班推进计划即可。";
                } else {
                    return baseAdvice + "，注意调节情绪，避免因小事影响心情。";
                }
            }
        }
        
        return advice.toString();
    }
}
