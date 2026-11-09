package com.project.stockwork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean
    public Docket stockworkApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("双基地仓储库存与生产报工管理系统 API")
                        .description("Web 管理后台与 H5 移动端共用的一套 REST 接口")
                        .version("1.0.0")
                        .build())
                .select()
                .paths(PathSelectors.any())
                .build();
    }
}
