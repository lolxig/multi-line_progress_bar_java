package com.nullpo.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase


/**
 * created with IntelliJ IDEA 2019.3
 * author: nullpo
 * date: 2021/1/21 14:15
 * version: 1.0
 * description: to do
 */
class LogbackColorful extends ForegroundCompositeConverterBase<ILoggingEvent> {
    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        switch (event.getLevel().toInt()) {
            case Level.ERROR_INT:
                return ANSIConstants.RED_FG
            case Level.WARN_INT:
                return ANSIConstants.YELLOW_FG
            case Level.INFO_INT:
                return ANSIConstants.BLUE_FG
            case Level.DEBUG_INT:
                return ANSIConstants.GREEN_FG
            default:
                return ANSIConstants.DEFAULT_FG
        }
    }
}
