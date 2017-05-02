package com.github.bin.sample

import com.github.bin.sample.interfaces.MainProc
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Created by ohya on 2017/04/29.
 */
@Component
class MyMainProc implements MainProc{
    def logger = LoggerFactory.getLogger(MyMainProc.class)

    @Override
    void init() {
        logger.info("init")
    }

    @Override
    void main() {
        logger.info("main")
    }

    @Override
    void term() {
        logger.info("term")
    }

}

