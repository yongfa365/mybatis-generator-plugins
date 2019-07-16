package yongfa365.mybaits.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.List;

public class JpaAnnotationPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        this.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        this.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        this.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        topLevelClass.addImportedType("javax.persistence.*");
        topLevelClass.addAnnotation("@Entity");
        topLevelClass.addAnnotation(String.format("@Table(name = \"%s\")", tableName));
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        String columnName = introspectedColumn.getActualColumnName();
        boolean isPrimaryKey = introspectedTable.getPrimaryKeyColumns().stream().anyMatch(c -> c.getActualColumnName().equals(columnName));
        if (isPrimaryKey) {
            field.addAnnotation("@Id");
            field.addAnnotation("@GeneratedValue(strategy=GenerationType.IDENTITY)");
        }
        field.addAnnotation(String.format("@Column(name = \"%s\")", columnName));
        return true;
    }
}

//参考了:
//https://github.com/thinking-github/nbone/blob/master/nbone/nbone-toolbox/src/main/java/org/nbone/mybatis/generator/plugins/JpaAnnotationPlugin.java
//https://github.com/liuyuyu/mbg-plus/blob/master/src/main/java/io/github/liuyuyu/mbg/plus/JPAAnnotationPlugin.java