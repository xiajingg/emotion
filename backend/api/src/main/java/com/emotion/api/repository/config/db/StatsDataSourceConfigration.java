//package com.emotion.api.repository.config.db;
//
//import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
//import org.apache.ibatis.session.SqlSessionFactory;
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
//import javax.sql.DataSource;
//import java.util.Properties;
//
//@Configuration
//@MapperScan(basePackages = DataSourceConstants.STATS_MYBATIS_MAPPER_PACKAGE, sqlSessionTemplateRef = "statsSqlSessionTemplate")
//@EnableTransactionManagement
//public class StatsDataSourceConfigration {
//
//    @Bean(name = "statsDataSource")
//    public DataSource statsDataSource(Environment env) {
//        return AbstractDruidConfiguration
//                .initDruidDataSource(env, DataSourceConstants.DataSourceEnum.STATS.getPrefix());
//    }
//
//    @Bean(name = "statsSqlSessionFactory")
//    public SqlSessionFactory sqlSessionFactory(
//            @Qualifier("statsDataSource") DataSource dataSource, Environment env) throws Exception {
//        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        // 集成mybatis plus
//        MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
//        mybatisSqlSessionFactoryBean.setMapperLocations(resolver.getResources(DataSourceConstants.DataSourceEnum.STATS.getXmlPath()));
//        mybatisSqlSessionFactoryBean.setConfigurationProperties(new Properties());
//        mybatisSqlSessionFactoryBean.setConfigLocation(resolver.getResource(DataSourceConstants.MYBATIS_CONFIG_LOCATION));
//        mybatisSqlSessionFactoryBean.setDataSource(dataSource);
//
//        return mybatisSqlSessionFactoryBean.getObject();
//    }
//
//    @Bean(name = "statsSqlSessionTemplate")
//    public SqlSessionTemplate sqlSessionTemplate(
//            @Qualifier("statsSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
//        return new SqlSessionTemplate(sqlSessionFactory);
//    }
//}
