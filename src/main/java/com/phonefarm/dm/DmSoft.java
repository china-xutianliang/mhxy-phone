package com.phonefarm.dm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 大漠插件 Java 封装（预留接口）。
 * 后续实现时需要：
 * - dm.dll 通过 regsvr32 注册
 * - jacob-1.18-x64.dll 在 java.library.path 中
 */
public class DmSoft {

    private static final Logger log = LoggerFactory.getLogger(DmSoft.class);
}
