package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.JavaBeansUtil;

import java.util.List;

public class ConstructorPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }


    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        introspectedTable.getAllColumns().forEach(column -> {
            sb.append(String.format("\n//%s %s,  //base的", column.getFullyQualifiedJavaType(), column.getJavaProperty()));
            sb2.append(String.format("\n//this.%s(%s); //base的", JavaBeansUtil.getSetterMethodName(column.getJavaProperty()), column.getJavaProperty()));
        });

        topLevelClass.addFileCommentLine(sb.toString());
        topLevelClass.addFileCommentLine(sb2.toString());
        System.out.println(sb);
        System.out.println(sb2);

        return true;
    }
}