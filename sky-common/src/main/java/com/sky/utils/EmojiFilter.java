package com.sky.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmojiFilter {

    /**
     * 过滤掉emoji表情符号和其他特殊Unicode字符
     * 只保留中文、英文、数字和常见标点符号
     */
    public static String filterEmoji(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            
            // 保留基本ASCII字符（字母、数字、常见标点）
            if (c <= 0x7F) {
                result.append(c);
            }
            // 保留中文汉字范围
            else if (c >= 0x4E00 && c <= 0x9FFF) {
                result.append(c);
            }
            // 保留中文标点符号范围
            else if (c >= 0x3000 && c <= 0x303F) {
                result.append(c);
            }
            // 保留全角ASCII、全角标点
            else if (c >= 0xFF00 && c <= 0xFFEF) {
                result.append(c);
            }
            // 其他字符（包括emoji）跳过
            else {
                log.debug("过滤特殊字符: U+{:04X}", (int)c);
            }
        }
        
        return result.toString();
    }
}
