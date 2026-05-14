package com.hxh.apboa.core.mpatch.model;

/**
 * 描述：搜索替换项
 * 用于存储单个 SEARCH/REPLACE 操作的搜索内容和替换内容
 *
 * @param search  要搜索的原代码片段
 * @param replace 要替换成的新代码片段
 *
 * @author huxuehao
 **/
public record SearchReplaceItem(String search, String replace) {
}
