package yongfa365.mybatis.generator.plugins;


import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;
import java.util.stream.Collectors;

public class AddTransientFieldPlugin extends PluginAdapter {

    private static List<FieldConfig> FIELD_CONFIG;

    @Override
    public boolean validate(List<String> list) {

        if (FIELD_CONFIG == null) {
            FIELD_CONFIG = properties.entrySet().stream()
                    .map(p -> {
                        String[] items = p.getKey().toString().split("\\s+");
                        return new FieldConfig(items[0], items[1], items[2], p.getValue().toString());
                    })
                    .sorted((x, y) -> String.CASE_INSENSITIVE_ORDER.compare(x.name, y.name))
                    .collect(Collectors.toList());
        }
        return true;
    }


    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        String baseRecordType = introspectedTable.getBaseRecordType();
        final String className = baseRecordType.substring(baseRecordType.lastIndexOf(".") + 1);

        //生成自定义属性时需要排序，不然增加个字段后就可能导致顺序乱掉，增加review成本
        FIELD_CONFIG.stream().filter(p -> p.className.equals(className)).forEach(p -> {
            Field field = new Field(p.name, new FullyQualifiedJavaType(p.type));
            field.addAnnotation("@Transient");
            field.addJavaDocLine("/**");
            if (!p.comment.isEmpty()) {
                field.addJavaDocLine("* " + p.comment + "<br/>");
            }
            field.addJavaDocLine("* 数据库没有此Column，通过配置生成的");
            field.addJavaDocLine("*/");
            topLevelClass.addField(field);
        });

        return true;
    }

    class FieldConfig {

        FieldConfig(String className, String type, String name, String comment) {
            this.className = className;
            this.type = type;
            this.name = name;
            this.comment = comment;
        }

        final String className;
        final String type;
        final String name;
        final String comment;
    }

}

