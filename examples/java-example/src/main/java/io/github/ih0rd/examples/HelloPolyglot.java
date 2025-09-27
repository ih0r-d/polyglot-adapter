package io.github.ih0rd.examples;

import io.github.ih0r.adapter.api.PolyglotAdapter;
import io.github.ih0r.adapter.api.context.Language;
import io.github.ih0r.adapter.api.context.PolyglotContextFactory;

import java.util.Map;

public class HelloPolyglot {
    void main(){
//        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
//            Map<String, Object> result = adapter.evaluate("add", MyApi.class, 3, 4);
//            System.out.println("Result: " + result.get("result"));
//        }

        PolyglotContextFactory.Builder builder =
                new PolyglotContextFactory.Builder(Language.PYTHON)
                        .pyResourcesPath(Paths.get("examples/java-example/src/main/resources/python"));



//                .resourcePath("examples/java-example/src/main/python");

//        try (PolyglotAdapter adapter = PolyglotAdapter.python(builder)) {
//            Map<String, Object> result = adapter.evaluate("add", MyApi.class, 1, 2);
//            System.out.println(result);
//        }

    }
}
