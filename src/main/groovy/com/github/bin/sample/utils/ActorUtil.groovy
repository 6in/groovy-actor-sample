package com.github.bin.sample.utils

import groovyx.gpars.actor.Actor

import static groovyx.gpars.actor.Actors.actor

/**
 * Created by ohya on 2017/05/02.
 */
class ActorUtil {
    static def sendRequest(Actor target, Object msg) {
        def replyMsg = null
        def recv = actor {
            target.send msg
            react {
                replyMsg = it
            }
        }
        recv.join()
        return replyMsg
    }

    static void sendMessage(Actor target, Object msg) {
        def replyMsg = null
        actor {
            target.send msg
        }
    }


}
