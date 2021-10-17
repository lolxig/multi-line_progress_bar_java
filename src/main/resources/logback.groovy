import com.nullpo.utils.LogbackColorful

conversionRule('color', LogbackColorful)

appender('console', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%d{yyyy-MM-dd HH:mm:ss.SSS} %magenta([%thread]) %color(%-5level) %cyan(%logger{15}) - %msg%n'
    }
}

appender('file', RollingFileAppender) {
    file = 'logs/process.log'
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = 'logs/process.%d{yyyy-MM-dd}.log'
        maxHistory = 10000
        totalSizeCap = '2GB'
    }
    encoder(PatternLayoutEncoder) {
        pattern = '%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }
}

root(INFO, ['console', 'file'])