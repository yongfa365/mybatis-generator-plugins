package yongfa365.mybaits.generator.plugins;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;

public class AddTransientFieldPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        return true;
    }


    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String baseRecordType = introspectedTable.getBaseRecordType();
        final String className = baseRecordType.substring(baseRecordType.lastIndexOf(".") + 1);

        properties.entrySet().stream().filter(p -> p.getKey().toString().startsWith(className + " ")).forEach(p -> {
            String[] items = p.getKey().toString().split(("\\s+"));
            Field field = new Field(items[2], new FullyQualifiedJavaType(items[1]));
            field.addAnnotation("@Transient");
            field.addJavaDocLine("/**");
            if (!p.getValue().toString().isEmpty()) {
                field.addJavaDocLine("* " + p.getValue()+"<br/>");
            }
            field.addJavaDocLine("* 数据库没有此Column，通过配置生成的");
            field.addJavaDocLine("*/");
            topLevelClass.addField(field);
        });

        return true;
    }


}

