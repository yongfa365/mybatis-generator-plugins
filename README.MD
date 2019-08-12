# 官方信息
id|Url
--|--
1|[MyBatis Generator官方地址](http://www.mybatis.org/generator/)
2|[MyBatis Generator Plugin Lifecycle](http://www.mybatis.org/generator/reference/pluggingIn.html)
3|[官方plug例子](https://github.com/mybatis/generator/tree/master/core/mybatis-generator-core/src/main/java/org/mybatis/generator/plugins)

写在前面的plug先执行。

整个执行过程都是在处理一些内存对象，最终所有plug执行完才写入。

```xml
 <context id="yongfa365.mybatis.generator" targetRuntime="MyBatis3" defaultModelType="flat">
        <property name="javaFileEncoding" value="UTF-8"/>
        <!-- 可以用这种跳过删除某些目录-->
        <!-- <plugin type="yongfa365.mybatis.generator.plugins.DeleteGeneratedFilesPlugin">-->
        <!--     <property name="deleteJavaModel" value="false"/>-->
        <!--     <property name="deleteSqlMap" value="false"/>-->
        <!--     <property name="deleteJavaClient" value="false"/>-->
        <!-- </plugin>-->
        <!--删除所有自动生成的目标目录里的文件，默认是全删除，一般要放在最前面，如果使用融合方案在应注释掉这个-->
        <plugin type="yongfa365.mybatis.generator.plugins.DeleteGeneratedFilesPlugin"/>
        
        <!--MyBatis Generator 1.3.7还没有实现生成MSSQL  SqlServer备注，用插件填充上，之后再使用就有了，应该放的尽可能靠前-->
        <plugin type="yongfa365.mybatis.generator.plugins.FixSqlServerCommentPlugin"/>
        
        <!--为Model加上Swagger的注解-->
        <plugin type="yongfa365.mybatis.generator.plugins.lab.SwaggerAnnotationPlugin"/>
        
        <!--为Model加上Swagger2的注解-->
        <plugin type="yongfa365.mybatis.generator.plugins.Swagger2AnnotationPlugin"/>
        
        <!--为Model加上lombok的import及自己想在类上加的注解，都是以|分割-->
        <plugin type="yongfa365.mybatis.generator.plugins.LombokAnnotationPlugin">
            <property name="imports" value="lombok.*|lombok.experimental.*"/>
            <property name="annotations" value="@Data|@Builder|@AllArgsConstructor|@NoArgsConstructor"/>
        </plugin>
        
        <!--为Model加上Jpa注解@Entity,@Id,@Table,@Column等-->
        <plugin type="yongfa365.mybatis.generator.plugins.JpaAnnotationPlugin"/>
        
        <!--name要唯一，格式：类名 类型 字段名；value：备注-->
        <plugin type="yongfa365.mybatis.generator.plugins.lab.AddTransientFieldPlugin">
            <property name="表一 Integer isApproved" value="审批状态"/>
            <property name="表二 Integer isApproved" value="审批状态"/>
        </plugin>

        <!--集成tk 通用mapper，Model，SqlMap，DAO都是融合的：同一文件上面自动，下面手动-->
        <plugin type="yongfa365.mybatis.generator.plugins.TkMapperPlugin">
            <!--所有生成的Mapper都继承自这个设定，多个可以“,”隔开-->
            <property name="mappers" value="hk.com.wot.dao.MyMapper"/>
            <!-- Field加类似@ColumnType(jdbcType = JdbcType.NVARCHAR)，最终运行时效果：#{resourceID,jdbcType=BIGINT}-->
            <property name="isGenColumnTypeWithJdbcType" value="false"/>
            <!--是否融合Model新旧imports-->
            <property name="isMixModelImports" value="true"/>
        </plugin>

        
        <!--为Model加上javadoc，内容是数据库备注-->
        <commentGenerator type="yongfa365.mybatis.generator.plugins.DatabaseCommentGenerator">
            <property name="suppressDate" value="true"/>
            <!--yongfa365生成实体备注的插件不关注此设置-->
            <property name="suppressAllComments" value="false"/>
        </commentGenerator>


        <jdbcConnection driverClass="com.microsoft.sqlserver.jdbc.SQLServerDriver"
                        connectionURL="jdbc:sqlserver://127.0.0.1;DatabaseName=XXXXDB"
                        userId="sa"
                        password="********">
        </jdbcConnection>
        
         <!--tinyint默认转为Byte了，精度不够，改成Integer-->
        <javaTypeResolver type="yongfa365.mybatis.generator.plugins.JavaTypeResolverDefaultImpl">
            <property name="forceBigDecimals" value="false"/>
            <!-- 简单说就是生成 LocalDateTime而不是Date -->
            <property name="useJSR310Types" value="true"/>
        </javaTypeResolver>
</context>
```
