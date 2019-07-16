# 官方信息
id|Url
--|--
1|[MyBatis Generator官方地址](http://www.mybatis.org/generator/)
2|[MyBatis Generator Plugin Lifecycle](http://www.mybatis.org/generator/reference/pluggingIn.html)
3|[官方plug例子](https://github.com/mybatis/generator/tree/master/core/mybatis-generator-core/src/main/java/org/mybatis/generator/plugins)

写在前面的plug先执行。

整个执行过程都是在处理一些内存对象，最终所有plug执行完才写入。

```xml
 <context id="xxxxxxx" targetRuntime="MyBatis3" defaultModelType="flat">
        <property name="javaFileEncoding" value="UTF-8"/>
        <!-- 可以用这种跳过删除某些目录-->
        <!-- <plugin type="yongfa365.mybaits.generator.plugins.DeleteGeneratedFilesPlugin">-->
        <!--     <property name="deleteJavaModel" value="false"/>-->
        <!--     <property name="deleteSqlMap" value="false"/>-->
        <!--     <property name="deleteJavaClient" value="false"/>-->
        <!-- </plugin>-->
        <!--删除所有自动生成的目标目录里的文件，默认是全删除，一般要放在最前面-->
        <plugin type="yongfa365.mybaits.generator.plugins.DeleteGeneratedFilesPlugin"/>
        
        <!--MyBaits Generator 1.3.7还没有实现生成MSSQL  SqlServer备注，用插件填充上，之后再使用就有了，应该放的尽可能靠前-->
        <plugin type="yongfa365.mybaits.generator.plugins.FixSqlServerCommentPlugin"/>
        
        <!--为Model加上Swagger的注解-->
        <plugin type="yongfa365.mybaits.generator.plugins.SwaggerPlugin"/>
        
        <!--为Model加上lombok的import及自己想在类上加的注解，都是以|分割-->
        <plugin type="yongfa365.mybaits.generator.plugins.LombokPlugin">
            <property name="imports" value="lombok.*|lombok.experimental.*"/>
            <property name="annotations" value="@Data|@Builder|@AllArgsConstructor|@NoArgsConstructor"/>
        </plugin>
        
        <!--为Model加上Jpa注解@Entity,@Id,@Table,@Column等-->
        <plugin type="yongfa365.mybaits.generator.plugins.JpaAnnotationPlugin"/>
        
        <!--为Model加上javadoc，内容是数据库备注-->
        <commentGenerator type="yongfa365.mybaits.generator.plugins.DatabaseCommentGenerator" />
</context>
```
