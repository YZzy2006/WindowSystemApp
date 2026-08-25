package com.window.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class MybatisPlusConfig {

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataSource dataSource) throws SQLException {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType dbType = detectDbType(dataSource);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

    private DbType detectDbType(DataSource dataSource) {
        if (driverClassName.contains("h2")) return DbType.H2;
        try (var conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (productName.contains("h2")) return DbType.H2;
            if (productName.contains("mysql")) return DbType.MYSQL;
        } catch (Exception ignored) {}
        return DbType.MYSQL;
    }
}
