package com.github.bin.sample.actors

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import jdk.nashorn.internal.ir.annotations.Immutable
import jp.co.future.uroborosql.SqlAgent
import jp.co.future.uroborosql.config.DefaultSqlConfig
import jp.co.future.uroborosql.config.SqlConfig
import jp.co.future.uroborosql.fluent.SqlUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct


@Immutable
class DbActorInitParam {
}

@Immutable
class DbActorExecParam {
    int rowId
    String sqlName
    HashMap<String, Object> params
}

@Immutable
class DbActorQueryParam {
    String sqlName
    HashMap<String, Object> params
}


@Component
@ConfigurationProperties(prefix = "dbActor")
class DbActorConfig {
    int instances
    HashMap<String, String> jdbc
    HashMap<String, String> sqls
}

/**
 * Created by ohya on 2017/04/29.
 */
@Component
class DbActor extends DefaultActor {

    def logger = LoggerFactory.getLogger(DbActor.class)

    @Autowired
    DbActorConfig dbActorConfig

    @Value('${dbActor.instances}')
    int instances

    List<Actor> childActors = new ArrayList<>()

    DbActor() {
    }

    @PostConstruct
    void init() {
        logger.info("init")
        logger.info("instances1:${instances}")
        logger.info("jdbc:${dbActorConfig.jdbc.url}")

        assert dbActorConfig.instances > 0

        logger.info("connect db")
        SqlConfig config = DefaultSqlConfig.getConfig(
                dbActorConfig.jdbc["url"]
                , dbActorConfig.jdbc["usr"]
                , dbActorConfig.jdbc["psw"])

        SqlAgent sqlAgent = config.createAgent()

        // ここでテーブル作成
        logger.info("create tables")
        sqlAgent.updateWith(dbActorConfig.sqls["create_tables"]).count()
        //sqlAgent.updateWith(dbActorConfig.sqls["truncate_data"]).count()
        sqlAgent.commit()
        sqlAgent.close()

        // 子アクターを生成し、起動する
        (1..dbActorConfig.instances).each {
            childActors << new DbChildActor(it-1,dbActorConfig)
        }

        // アクター開始
        start()
    }

    @Override
    protected void act() {
        logger.info("start DbActor: instances=>" + dbActorConfig)
        int dbActorStatus = 0

        SqlConfig config = DefaultSqlConfig.getConfig(
                dbActorConfig.jdbc["url"]
                , dbActorConfig.jdbc["usr"],
                dbActorConfig.jdbc["psw"])

        SqlAgent sqlAgent = config.createAgent()

        loop {
            react { message ->
                switch (message) {
                    case DbActorInitParam :
                        reply "test"
                        break

                    case DbActorQueryParam:
                        def param = message as DbActorQueryParam
                        def rows = []

                        if (dbActorConfig.sqls.containsKey(param.sqlName)) {
                            String sql = dbActorConfig.sqls[param.sqlName]
                            def query = sqlAgent.queryWith(sql)
                            param.params.each {
                                query.param(it.key, it.value)
                            }
                            query.collect().each {
                                rows << it
                            }
                        }
                        reply rows
                        break
                    case DbActorExecParam:
                        // 担当する子アクターに、メッセージを転送
                        def param = message as DbActorExecParam
                        def idx = param.rowId % dbActorConfig.instances
                        logger.info("send to client[${idx}]")
                        childActors[idx].send(param)
                        break
                    default:
                        logger.info("DbActor stop")
                        // 子アクターを終了
                        childActors.each {
                            it.send(false)
                            it.join()
                        }
                        sqlAgent.close()
                        dbActorStatus = 0
                        stop()
                }
            }
        }
    }


}

class DbChildActor extends DefaultActor {
    def logger = LoggerFactory.getLogger(DbChildActor.class)

    DbActorConfig dbActorConfig
    int updateCount = 0
    int actorNo = 0

    DbChildActor(int idx,DbActorConfig dbActorConfig) {
        this.dbActorConfig = dbActorConfig
        this.actorNo = idx
        start()
    }

    @Override
    protected void act() {
        SqlConfig config = DefaultSqlConfig.getConfig(
                dbActorConfig.jdbc["url"]
                , dbActorConfig.jdbc["usr"]
                , dbActorConfig.jdbc["psw"])

        SqlAgent sqlAgent = config.createAgent()

        def sqlUpdates = [:].withDefault { key ->
            sqlAgent.updateWith(
                dbActorConfig.sqls[key]
                    .replaceAll(/__ACTOR_NO__/,sprintf("%02d",this.actorNo))
            )
        }

        loop {
            react { message ->
                switch (message) {
                    case DbActorExecParam :
                        DbActorExecParam param = message as DbActorExecParam
                        String sqlName = param.sqlName
                        SqlUpdate sqlUpdate = sqlUpdates[sqlName]

                        param.params.each { kv ->
                            sqlUpdate.param(kv.key,kv.value)
                        }

                        sqlUpdate.addBatch()
                        this.updateCount++

                        if (this.updateCount % 1000 == 0) {
                            sqlUpdate.batch()
                            sqlAgent.commit()
                        }

                        break
                    default :
                        logger.info("DbClient stop")
                        if (this.updateCount % 1000 != 0) {
                            sqlUpdates.each{ it.value.batch() }
                        }
                        sqlAgent.commit()
                        stop()
                }
            }
        }
    }



}