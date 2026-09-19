package com.emotion.api.repository.config.db;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * AbstractDruidConfiguration
 */
@Configuration
@MapperScan(basePackages = DataSourceConstants.RDS_MYBATIS_MAPPER_PACKAGE, sqlSessionTemplateRef = "rdsSqlSessionTemplate")
@EnableTransactionManagement
public class RdsDataSourceConfiguration {


    @Resource
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Resource
    private MetaObjectHandler metaObjectHandler;

    @Primary
    @Bean(name = "rdsDataSource")
    public DataSource rdsDataSource(Environment env) {
        return AbstractDruidConfiguration
                .initDruidDataSource(env, DataSourceConstants.DataSourceEnum.RDS.getPrefix());
    }

    @Primary
    @Bean(name = "rdsSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("rdsDataSource") DataSource dataSource, Environment env) throws Exception {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 集成mybatis plus
        MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        mybatisSqlSessionFactoryBean.setMapperLocations(resolver.getResources(DataSourceConstants.DataSourceEnum.RDS.getXmlPath()));
        mybatisSqlSessionFactoryBean.setConfigurationProperties(new Properties());
        mybatisSqlSessionFactoryBean.setConfigLocation(resolver.getResource(DataSourceConstants.MYBATIS_CONFIG_LOCATION));
        mybatisSqlSessionFactoryBean.setDataSource(dataSource);
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        mybatisSqlSessionFactoryBean.setGlobalConfig(globalConfig);
        Interceptor[] plugins = new Interceptor[1];
        plugins[0] = mybatisPlusInterceptor;
        mybatisSqlSessionFactoryBean.setPlugins(plugins);
        return mybatisSqlSessionFactoryBean.getObject();
    }

    @Primary
    @Bean(name = "rdsSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(
            @Qualifier("rdsSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
