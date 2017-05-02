package com.github.bin.sample.utils

import jp.co.future.uroborosql.SqlAgent
import jp.co.future.uroborosql.config.DefaultSqlConfig
import jp.co.future.uroborosql.config.SqlConfig
import jp.co.future.uroborosql.fluent.SqlQuery

/**
 * Created by ohya on 2017/05/01.
 */
class DbUtil {

    def test() {

        SqlConfig config = DefaultSqlConfig.getConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

        //SqlContextFactory sqlContextFactory = config.getSqlContextFactory()
        //SqlContext sqlContext = sqlContextFactory.createSqlContext()

        SqlAgent agent = config.createAgent()

        agent.updateWith("create table temp(id bigint)").count()
        agent.updateWith("insert into temp(id) values(1)").count()
        agent.updateWith("insert into temp(id) values(2)").count()

        agent.queryWith("select * from temp where id = /*ID*/").param("ID", 1)
        .collect().each { row ->
            println(row.ID)
        }


        agent.close()
    }

    static void main(String[] args) {
        new DbUtil().test()
    }

}
