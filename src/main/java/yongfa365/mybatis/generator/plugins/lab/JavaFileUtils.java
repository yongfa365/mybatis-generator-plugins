package yongfa365.mybatis.generator.plugins.lab;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.MergeConstants;
import yongfa365.mybatis.generator.util.ContextUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mybatis.generator.api.dom.OutputUtilities.newLine;

public class JavaFileUtils {

    public static void mergerFile(Context context, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        try {
            File oldFile = ContextUtils.getModelFile(context, introspectedTable);
            String oldFileString = ContextUtils.readAllString(oldFile.toPath());
            String newFileString = topLevelClass.getFormattedContent();
            if (oldFileString.equals(newFileString)) {
                return;
            }

            CompilationUnit newCompilationUnit = StaticJavaParser.parse(newFileString);
            CompilationUnit existingCompilationUnit = StaticJavaParser.parse(oldFileString);

            //旧的有而新的没有的imports，复制给新的
            NodeList<ImportDeclaration> imports = new NodeList<>();
            NodeList<ImportDeclaration> oldimports = existingCompilationUnit.getImports();
            imports.addAll(oldimports);
            imports.removeAll(existingCompilationUnit.getImports());
            for (ImportDeclaration item : imports) {
                newFileString = newFileString.replaceAll("package .*?\\r\\n", "$0\\r\\n" + item.toString());
            }


            //旧的有而新的没有的Field，复制给新的
            NodeList<TypeDeclaration<?>> newTypes = newCompilationUnit.getTypes();
            NodeList<TypeDeclaration<?>> oldTypes = existingCompilationUnit.getTypes();

            for (int i = 0; i < newTypes.size(); i++) {

                //合并fields
                List<FieldDeclaration> newFields = newTypes.get(i).getFields();
                List<FieldDeclaration> oldFields = oldTypes.get(i).getFields();
                List<FieldDeclaration> fields = new ArrayList<>();
                fields.addAll(oldFields);
                fields.removeAll(newFields);
                if (fields.size() == 0) {
                    continue;
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (FieldDeclaration item : fields) {
                    newLine(stringBuilder);
                    item.getAnnotationByName("Column").ifPresent(p -> {
                        item.getAnnotations().remove(p);
                    });
                    if (!item.isAnnotationPresent("Transient")) {
                        item.addAnnotation("Transient");
                    }
                    String tip = "数据库没有此Column";
                    if (item.hasJavaDocComment()) {
                        item.getJavadocComment().ifPresent(p -> {
                            if (!p.getContent().contains(tip)) {
                                String newstr = p.getContent() + "\r\n" + tip;
                                item.setJavadocComment(newstr);
                            }
                        });
                    } else {
                        item.setJavadocComment(tip);
                    }

                    stringBuilder.append(item.toString());//可以格式化
                    newLine(stringBuilder);
                }
                String fieldsString = stringBuilder.toString();
                fieldsString = fieldsString.replaceAll("(?m)(.+)", "    $1");

                newFileString = newFileString.substring(0, newFileString.lastIndexOf("}")) + fieldsString + "}";

            }

            Files.write(oldFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String mergerFile2(CompilationUnit newCompilationUnit, CompilationUnit existingCompilationUnit) {

        StringBuilder sb = new StringBuilder(newCompilationUnit.getPackageDeclaration().get().toString());
        newCompilationUnit.removePackageDeclaration();

        //合并imports
        NodeList<ImportDeclaration> imports = newCompilationUnit.getImports();
        imports.addAll(existingCompilationUnit.getImports());
        Set importSet = new HashSet<ImportDeclaration>();
        importSet.addAll(imports);

        NodeList<ImportDeclaration> newImports = new NodeList<>();
        newImports.addAll(importSet);
        newCompilationUnit.setImports(newImports);
        for (ImportDeclaration i : newCompilationUnit.getImports()) {
            sb.append(i.toString());
        }
        newLine(sb);
        NodeList<TypeDeclaration<?>> types = newCompilationUnit.getTypes();
        NodeList<TypeDeclaration<?>> oldTypes = existingCompilationUnit.getTypes();

        for (int i = 0; i < types.size(); i++) {
            //截取Class
            String classNameInfo = types.get(i).toString().substring(0, types.get(i).toString().indexOf("{") + 1);
            sb.append(classNameInfo);
            newLine(sb);
            newLine(sb);
            //合并fields
            List<FieldDeclaration> fields = types.get(i).getFields();
            List<FieldDeclaration> oldFields = oldTypes.get(i).getFields();
            List<FieldDeclaration> newFields = new ArrayList<>();
            HashSet<FieldDeclaration> fieldDeclarations = new HashSet<>();
            fieldDeclarations.addAll(fields);
            fieldDeclarations.addAll(oldFields);
            newFields.addAll(fieldDeclarations);
            for (FieldDeclaration f : newFields) {
                sb.append(f.toString());
                newLine(sb);
                newLine(sb);
            }

            //合并methods
            List<MethodDeclaration> methods = types.get(i).getMethods();
            List<MethodDeclaration> existingMethods = oldTypes.get(i).getMethods();
            for (MethodDeclaration f : methods) {
                sb.append(f.toString());
                newLine(sb);
                newLine(sb);
            }
            for (MethodDeclaration m : existingMethods) {
                boolean flag = true;
                for (String tag : MergeConstants.OLD_ELEMENT_TAGS) {
                    if (m.toString().contains(tag)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    sb.append(m.toString());
                    newLine(sb);
                    newLine(sb);
                }
            }

            //判断是否有内部类
            types.get(i).getChildNodes();
            for (Node n : types.get(i).getChildNodes()) {
                if (n.toString().contains("static class")) {
                    sb.append(n.toString());
                }
            }

        }

        return sb.append(System.getProperty("line.separator") + "}").toString();
    }


}
