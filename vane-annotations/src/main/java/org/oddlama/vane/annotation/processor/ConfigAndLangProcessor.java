package org.oddlama.vane.annotation.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
    "org.oddlama.vane.annotation.config.ConfigBoolean",
    "org.oddlama.vane.annotation.config.ConfigDouble",
    "org.oddlama.vane.annotation.config.ConfigInt",
    "org.oddlama.vane.annotation.config.ConfigLong",
    "org.oddlama.vane.annotation.config.ConfigMaterialSet",
    "org.oddlama.vane.annotation.config.ConfigString",
    "org.oddlama.vane.annotation.config.ConfigVersion",
    "org.oddlama.vane.annotation.lang.LangMessage",
    "org.oddlama.vane.annotation.lang.LangString",
    "org.oddlama.vane.annotation.lang.LangVersion",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ConfigAndLangProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			round_env.getElementsAnnotatedWith(annotation)
			    .forEach(e -> verify_type(annotation, e));
		}

		return true;
	}

	private static final Map<String, String> field_type_mapping;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("org.oddlama.vane.annotation.config.ConfigBoolean", "boolean");
		map.put("org.oddlama.vane.annotation.config.ConfigInt", "int");
		map.put("org.oddlama.vane.annotation.config.ConfigMaterialSet", "java.util.Set<org.bukkit.Material>");
		map.put("org.oddlama.vane.annotation.config.ConfigDouble", "double");
		map.put("org.oddlama.vane.annotation.config.ConfigLong", "long");
		map.put("org.oddlama.vane.annotation.config.ConfigString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.config.ConfigLong", "long");
		map.put("org.oddlama.vane.annotation.config.ConfigString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.config.ConfigVersion", "long");
		map.put("org.oddlama.vane.annotation.lang.LangMessage", "org.oddlama.vane.util.Message");
		map.put("org.oddlama.vane.annotation.lang.LangString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.lang.LangVersion", "long");
		field_type_mapping = Collections.unmodifiableMap(map);
	}

	private void verify_type(TypeElement annotation, Element element) {
		var type = ((VariableElement)element).asType().toString();
		var required_type = field_type_mapping.get(annotation.asType().toString());
		if (!required_type.equals(type)) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element.asType().toString()
					+ ": @" + annotation.getSimpleName() + " requires a field of type " + required_type + " but got " + type);
		}
	}
}
