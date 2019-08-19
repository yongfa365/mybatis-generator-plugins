package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.internal.util.StringUtility;
import yongfa365.mybatis.generator.util.RemarkUtils;

import java.util.ArrayList;
import java.util.List;

public class QueryDslPlugin extends PluginAdapter {
    private String targetPackage;
    private String targetProject;


    @Override
    public boolean validate(List<String> warnings) {
        targetPackage = properties.getProperty("targetPackage");
        targetProject = properties.getProperty("targetProject");
        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {

        List<GeneratedJavaFile> list = new ArrayList<>();

        CompilationUnit unit = generateUnit(introspectedTable);
        GeneratedJavaFile genFile = new GeneratedJavaFile(unit, targetProject, this.context.getProperty("javaFileEncoding"), this.context.getJavaFormatter());
        list.add(genFile);

        return list;
    }

    private CompilationUnit generateUnit(IntrospectedTable introspectedTable) {
        String entityClazzType = introspectedTable.getBaseRecordType();

        String domainObjectName = introspectedTable.getFullyQualifiedTable().getDomainObjectName();

        StringBuilder builder = new StringBuilder();

        FullyQualifiedJavaType superClassType = new FullyQualifiedJavaType(
                builder.append("EntityPathBase<")
                        .append(entityClazzType)
                        .append(">").toString()
        );

        TopLevelClass topLevelClass = new TopLevelClass(
                builder.delete(0, builder.length())
                        .append(targetPackage)
                        .append(".Q")
                        .append(domainObjectName)
                        .toString()
        );

        topLevelClass.setSuperClass(superClassType);
        topLevelClass.setVisibility(JavaVisibility.PUBLIC);

        FullyQualifiedJavaType modelJavaType = new FullyQualifiedJavaType(entityClazzType);
        topLevelClass.addImportedType(modelJavaType);
        topLevelClass.addImportedType("com.querydsl.core.types.dsl.*");
        topLevelClass.addImportedType("com.querydsl.core.types.PathMetadata");
        topLevelClass.addImportedType("com.querydsl.core.types.Path");
        topLevelClass.addStaticImport("com.querydsl.core.types.PathMetadataFactory.*");
        topLevelClass.addImportedType("javax.annotation.Generated");
        topLevelClass.addJavaDocLine("/**\n" +
                "* " + getDomainName(introspectedTable) + "\n" +
                "*/");

        topLevelClass.addAnnotation(String.format("@Generated(\"%s\")", "com.querydsl.codegen.EntitySerializer"));

        //添加serialVersionUID字段
        Field field = new Field();
        field.setFinal(true);
        field.setInitializationString("233287413L");
        field.setName("serialVersionUID");
        field.setStatic(true);
        field.setType(new FullyQualifiedJavaType("long"));
        field.setVisibility(JavaVisibility.PRIVATE);
        context.getCommentGenerator().addFieldComment(field, introspectedTable);
        topLevelClass.addField(field);

        Field qfield = new Field();
        qfield.setFinal(true);
        qfield.setStatic(true);
        qfield.setInitializationString("new Q" + domainObjectName + "(\"" + toCamelCase(domainObjectName) + "\")");
        qfield.setName(toCamelCase(domainObjectName));
        qfield.setType(new FullyQualifiedJavaType("Q" + domainObjectName));
        qfield.setVisibility(JavaVisibility.PUBLIC);
        context.getCommentGenerator().addFieldComment(qfield, introspectedTable);
        topLevelClass.addField(qfield);

        Method constructor = new Method("Q" + domainObjectName);
        constructor.setVisibility(JavaVisibility.PUBLIC);
        constructor.setConstructor(true);
        constructor.addParameter(new Parameter(FullyQualifiedJavaType.getStringInstance(), "variable"));
        constructor.addBodyLine("super(" + domainObjectName + ".class, forVariable(variable));");
        context.getCommentGenerator().addGeneralMethodComment(constructor, introspectedTable);
        topLevelClass.addMethod(constructor);

        Method constructor2 = new Method("Q" + domainObjectName);
        constructor2.setVisibility(JavaVisibility.PUBLIC);
        constructor2.setConstructor(true);
        constructor2.addParameter(new Parameter(new FullyQualifiedJavaType("Path<? extends " + domainObjectName + ">"), "path"));
        constructor2.addBodyLine("super(path.getType(), path.getMetadata());");
        context.getCommentGenerator().addGeneralMethodComment(constructor2, introspectedTable);
        topLevelClass.addMethod(constructor2);

        Method constructor3 = new Method("Q" + domainObjectName);
        constructor3.setVisibility(JavaVisibility.PUBLIC);
        constructor3.setConstructor(true);
        constructor3.addParameter(new Parameter(new FullyQualifiedJavaType("PathMetadata"), "metadata"));
        constructor3.addBodyLine("super(" + domainObjectName + ".class, metadata);");
        context.getCommentGenerator().addGeneralMethodComment(constructor3, introspectedTable);
        topLevelClass.addMethod(constructor3);

        addField(introspectedTable, topLevelClass);
        return topLevelClass;
    }

    private void addField(IntrospectedTable introspectedTable, TopLevelClass topLevelClass) {
        introspectedTable.getAllColumns().forEach(column -> {
            String fieldName = column.getJavaProperty();
            Field field = new Field();
            field.setFinal(true);
            field.setName(fieldName);
            field.setVisibility(JavaVisibility.PUBLIC);

            String typeName = column.getFullyQualifiedJavaType().getFullyQualifiedNameWithoutTypeParameters();
            field = setType(field, fieldName, typeName);

            String remarks = RemarkUtils.getRemark(introspectedTable, column);
            if (StringUtility.stringHasValue(remarks)) {
                field.addJavaDocLine("/**");
                field.addJavaDocLine(" * " + remarks);
                field.addJavaDocLine(" */");
            }
            context.getCommentGenerator().addFieldComment(field, introspectedTable);
            topLevelClass.addField(field);
        });
    }

    private Field setType(Field field, String fieldName, String typeName) {
        typeName = typeName.replace("java.lang.", "");
        String value = "";
        String type = typeName;
        switch (typeName) {
            case "String":
                value = "createString(\"" + fieldName + "\")";
                type = "StringPath";
                break;

            case "Boolean":
                value = "createBoolean(\"" + fieldName + "\")";
                type = "BooleanPath";
                break;

            case "java.time.LocalDate":
                value = "createDate(\"" + fieldName + "\", " + typeName + ".class)";
                type = "DatePath<" + typeName + ">";
                break;

            case "java.time.LocalDateTime":
                value = "createDateTime(\"" + fieldName + "\", " + typeName + ".class)";
                type = "DateTimePath<" + typeName + ">";
                break;

            case "Time":
                value = "createTime(\"" + fieldName + "\", " + typeName + ".class)";
                type = "TimePath<" + typeName + ">";
                break;

            case "Integer":
            case "Long":
            case "Double":
            case "java.math.BigDecimal":
                value = "createNumber(\"" + fieldName + "\", " + typeName + ".class)";
                type = "NumberPath<" + typeName + ">";
                break;
        }

        field.setInitializationString(value);
        field.setType(new FullyQualifiedJavaType(type));
        return field;
    }

    private String getDomainName(IntrospectedTable introspectedTable) {
        return introspectedTable.getRemarks() == null ? introspectedTable.getFullyQualifiedTable().getDomainObjectName() : introspectedTable.getRemarks();
    }

    private String toCamelCase(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }
}
