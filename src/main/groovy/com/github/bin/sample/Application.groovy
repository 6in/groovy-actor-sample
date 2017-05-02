package com.github.bin.sample

import com.github.bin.sample.actors.ConsoleActorParam
import com.github.bin.sample.actors.DbActorExecParam
import com.github.bin.sample.actors.DbActorInitParam
import com.github.bin.sample.actors.DbActorQueryParam
import com.github.bin.sample.interfaces.MainProc
import groovyx.gpars.actor.Actor
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.slf4j.LoggerFactory
import static com.github.bin.sample.utils.ActorUtil.sendMessage
import static com.github.bin.sample.utils.ActorUtil.sendRequest
import static groovyx.gpars.actor.Actors.actor

@SpringBootApplication
class Application {
    static def logger = LoggerFactory.getLogger(Application.class)

    static void main(String[] args) {
        logger.info("start")
        ApplicationContext ctx = SpringApplication.run(Application.class, args)

        // アクターは生成と同時に開始
        def actors = []
        Actor conActor = ctx.getBean("consoleActor") as Actor
        Actor dbActor  = ctx.getBean("dbActor") as Actor

        if (conActor.isActive() ) {
            sendMessage conActor , new ConsoleActorParam(level:"info",message:"hello from main")
        }

        // DbActorに初期化コマンドを送る
        if (dbActor.isActive()) {
            println(sendRequest(dbActor,new DbActorInitParam()))
        }
        println(sendRequest(dbActor,new DbActorQueryParam(sqlName:"select_test",params:[ID:1])))

        def worker = actor {
            new File(".").eachFileRecurse {
                sendMessage dbActor,new DbActorExecParam(rowId:0,sqlName: "insert_rec",params:[name:it.name])
            }
        }
        worker.join()
        println(sendRequest(dbActor,new DbActorQueryParam(sqlName:"select_test",params:[ID:1])))

        // メイン処理クラスを取得し、実行
        def mainProc = ctx.getBean("myMainProc") as MainProc
        try {
            mainProc.init()
            mainProc.main()
            mainProc.term()
        } catch (Exception e) {
            logger.error(e.localizedMessage)
        } finally {
            conActor.send( false )
            dbActor.send( false )
            [conActor,dbActor]*.join()
        }


    }


}