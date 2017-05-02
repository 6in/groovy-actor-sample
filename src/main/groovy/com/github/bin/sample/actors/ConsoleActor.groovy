package com.github.bin.sample.actors

import groovyx.gpars.actor.DefaultActor
import jdk.nashorn.internal.ir.annotations.Immutable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Immutable
class ConsoleActorParam {
    String level
    String message
}

/**
 * Created by ohya on 2017/04/29.
 */
@Component
class ConsoleActor extends DefaultActor {

    def logger = LoggerFactory.getLogger(ConsoleActor.class)

    ConsoleActor() {
    }

    @PostConstruct
    void init() {
        this.start()
    }

    @Override
    protected void act() {
        logger.info("start ConsoleActor start")
        loop {
            react { message ->
                switch (message) {
                    case ConsoleActorParam :
                        println("${message.level}:${message.message}")
                        break
                    default:
                        logger.info("logger stop")
                        stop()
                }
            }
        }
    }

}
