//package com.emotion.api.repository.config.db;
//
//import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
//import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
//import org.apache.ibatis.plugin.Interceptor;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
//import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
//import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
//import org.mybatis.spring.SqlSessionTemplate;
//import org.mybatis.spring.annotation.MapperScan;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import javax.annotation.Resource;
//import javax.sql.DataSource;
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//
///**
// * AbstractDruidConfiguration
// */
//@Configuration
//@MapperScan(basePackages = DataSourceConstants.PG_MYBATIS_MAPPER_PACKAGE, sqlSessionTemplateRef = "pgSqlSessionTemplate")
//@EnableTransactionManagement
//public class PgDataSourceConfiguration {
//    @Resource
//    private PaginationInterceptor paginationInterceptor;
//
//    @Bean(name = "pgDataSource")
//    public DataSource pgDataSource(Environment env) throws SQLException {
//        DataSource dataSource = AbstractDruidConfiguration
//                .initDruidDataSource(env, DataSourceConstants.DataSourceEnum.PG.getPrefix());
//        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
//        // 配置分表规则
//        shardingRuleConfig.getTableRuleConfigs().add(getAccountGroupFundBalanceConfiguration());
//        shardingRuleConfig.getTableRuleConfigs().add(getPgAccountGroupFundBalanceConfiguration());
//        //TODO 先把所有的filterOrderProfit 请求整理好
////        shardingRuleConfig.getTableRuleConfigs().add(getFilterConfiguration());
//
//        Properties props = new Properties();
//        // 配置shardingsphere是否打印日志
////        if (EnvUtil.isDev()) {
////            props.setProperty("sql.show", "true");
////        }
//        Map<String, DataSource> dataSourceMap = new HashMap<>();
//        dataSourceMap.put("sd", dataSource);
//        return ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, props);
//    }
//
//    // 配置分表规则
//    // 返回account_group_fund_balance表的分表规则配置
//    private TableRuleConfiguration getAccountGroupFundBalanceConfiguration() {
//        // 指定数据库及表配置规则 最后面, 表名拼接$-> 代表分表的分隔符, 2022..2024代表分表的年份, ${1..12}代表分表的月份
//        TableRuleConfiguration result = new TableRuleConfiguration("account_group_fund_balance", "sd.account_group_fund_balance,sd.account_group_fund_balance_$->{2022..2024}_${1..12}");
//
//        // 指定分表字段及分表规则类  TaTable2Algorithm为自定义分表规则类, 下面的配置可以指定字段, 目前先不指定
////        result.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("uav_id", new UavPreciseShardingAlgorithm()));
//        return result;
//    }
//
//    private TableRuleConfiguration getFilterConfiguration() {
//        // 指定数据库及表配置规则 最后面, 表名拼接$-> 代表分表的分隔符, 2022..2024代表分表的年份, ${1..12}代表分表的月份
//        TableRuleConfiguration result = new TableRuleConfiguration("filter_order_profit", "sd.filter_order_profit,sd.filter_order_profit_$->{2022..2024}_${1..12}");
//
//        // 指定分表字段及分表规则类  TaTable2Algorithm为自定义分表规则类, 下面的配置可以指定字段, 目前先不指定
////        result.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("uav_id", new UavPreciseShardingAlgorithm()));
//        return result;
//    }
//    // 这个也是规则, 加这个是因为99%的情况只需要查主表, 就配置了一个单独主表的查询规则
//    private TableRuleConfiguration getPgAccountGroupFundBalanceConfiguration() {
//        // 指定数据库及表配置规则
//        TableRuleConfiguration result = new TableRuleConfiguration("pg_account_group_fund_balance", "sd.account_group_fund_balance");
//
//        // 指定分表字段及分表规则类  TaTable2Algorithm为自定义分表规则类
////        result.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("uav_id", new UavPreciseShardingAlgorithm()));
//        return result;
//    }
//
////    private TableRuleConfiguration getAccountGroupFundMasterConfiguration() {
////        // 指定数据库及表配置规则
////        TableRuleConfiguration result = new TableRuleConfiguration("account_group_fund_master");
////
////        // 指定分表字段及分表规则类  TaTable2Algorithm为自定义分表规则类
//////        result.setTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("uav_id", new UavPreciseShardingAlgorithm()));
////        return result;
////    }
//
//
//    @Bean(name = "pgSqlSessionFactory")
//    public SqlSessionFactory sqlSessionFactory(
//            @Qualifier("pgDataSource") DataSource dataSource, Environment env) throws Exception {
//        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        // 集成mybatis plus
//        MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
//        mybatisSqlSessionFactoryBean.setMapperLocations(resolver.getResources(DataSourceConstants.DataSourceEnum.PG.getXmlPath()));
//        mybatisSqlSessionFactoryBean.setConfigurationProperties(new Properties());
//        mybatisSqlSessionFactoryBean.setConfigLocation(resolver.getResource(DataSourceConstants.MYBATIS_CONFIG_LOCATION));
//        mybatisSqlSessionFactoryBean.setDataSource(dataSource);
//        Interceptor[] plugins = new Interceptor[1];
//        plugins[0] = paginationInterceptor;
//        mybatisSqlSessionFactoryBean.setPlugins(plugins);
//        return mybatisSqlSessionFactoryBean.getObject();
//    }
//
//    @Bean(name = "pgSqlSessionTemplate")
//    public SqlSessionTemplate sqlSessionTemplate(
//            @Qualifier("pgSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
//        return new SqlSessionTemplate(sqlSessionFactory);
//    }
//}
