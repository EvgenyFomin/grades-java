package ru.protei.processor;

import com.google.auto.service.AutoService;
import ru.protei.annotation.JdbcColumn;
import ru.protei.annotation.JdbcEntity;
import ru.protei.annotation.JdbcOneToMany;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes("ru.protei.annotation.JdbcEntity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MetaInfoProcessor extends AbstractProcessor {
    public MetaInfoProcessor() {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {

        Map<Element, List<Element>> clazzToFields = new HashMap<>();

        for (TypeElement annotation : annotations) {
            for (Element clazz : roundEnv.getElementsAnnotatedWith(annotation)) {
                clazzToFields.put(clazz, new ArrayList<>());
                for (Element el : clazz.getEnclosedElements()) {
                    if (el.getKind().isField() && (el.getAnnotation(JdbcColumn.class) != null || el.getAnnotation(JdbcOneToMany.class) != null)) {
                        clazzToFields.get(clazz).add(el);
                    }
                }
            }
        }

        clazzToFields.forEach((clazz, fields) -> {
            TypeElement classElement = (TypeElement) clazz;
            String className = classElement.getSimpleName() + "_";
            String tableName = classElement.getAnnotation(JdbcEntity.class).table();

            try {
                JavaFileObject builderFile = processingEnv.getFiler()
                        .createSourceFile(classElement.getQualifiedName() + "_");

                try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                    out.println("package " + getPackage(classElement) + ";");
                    out.println("public class " + className + " {");
                    out.println("    public static final String TABLE_NAME = \"" + tableName + "\";");

                    List<Element> jdbcColumnFields = fields.stream().filter(el -> el.getAnnotation(JdbcColumn.class) != null).toList();
                    List<Element> jdbcOneToManyFields = fields.stream().filter(el -> el.getAnnotation(JdbcOneToMany.class) != null).toList();

                    if (!jdbcColumnFields.isEmpty()) {
                        out.println("    public interface Columns {");
                        jdbcColumnFields.forEach(field -> {
                            String columnName = field.getAnnotation(JdbcColumn.class).value();
                            out.println("        public String " + columnName.toUpperCase() + " = \"" + columnName + "\";");
                        });
                        out.println("    }");

                        out.println("    public interface FullColumns {");
                        jdbcColumnFields.forEach(field -> {
                            String columnName = field.getAnnotation(JdbcColumn.class).value();
                            out.println("        public String " + columnName.toUpperCase() + " = TABLE_NAME + \".\" + Columns." + columnName.toUpperCase() + ";");
                        });
                        out.println("    }");
                    }

                    if (!jdbcOneToManyFields.isEmpty()) {
                        out.println("    public interface Fields {");
                        jdbcOneToManyFields.forEach(field -> {
                            out.println("        public String " + field.getSimpleName().toString().toUpperCase() + " = \"" + field.getSimpleName().toString() + "\";");
                        });
                        out.println("    }");
                    }

                    out.println("}");
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.NOTE,
                        "Failed to generate builder: " + e.getMessage()
                );
            }
        });

        return true;
    }

    private String getPackage(TypeElement type) {
        return processingEnv.getElementUtils()
                .getPackageOf(type).getQualifiedName().toString();
    }
}