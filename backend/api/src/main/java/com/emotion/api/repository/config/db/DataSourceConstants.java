package com.emotion.api.repository.config.db;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AbstractDruidConfiguration
 *
 */
public class DataSourceConstants {

    public static final String MYBATIS_CONFIG_LOCATION = "classpath:mybatis/mybatis-config.xml";

    public final static String RDS_MYBATIS_MAPPER_PACKAGE = "com.emotion.api.repository.dao.rds";

    public final static String PG_MYBATIS_MAPPER_PACKAGE = "com.iote.stalin.mapper.pg";

    public final static String STATS_MYBATIS_MAPPER_PACKAGE = "com.iote.stalin.mapper.stats";

    @Getter
    @AllArgsConstructor
    public enum DataSourceEnum {

        RDS("主数据库", "classpath:mybatis/mapper/rds/*.xml", "spring.datasource.rds."),
//        PG("read库", "classpath:mybatis/mapper/pg/*.xml", "spring.shardingsphere.datasource.sd."),
//        STATS("stats数据库", "classpath:mybatis/mapper/stats/*.xml", "spring.datasource.stats.")
        ;

        private String desc;
        private String xmlPath;
        private String prefix;
    }
}
