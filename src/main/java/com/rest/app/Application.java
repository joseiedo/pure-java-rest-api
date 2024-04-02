package com.rest.app;

import com.rest.app.config.Injector;
import com.rest.app.utils.ApiUtils;
import com.rest.app.utils.Path;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.classes.ClassCriteria;
import org.burningwave.core.classes.ClassHunter;
import org.burningwave.core.classes.SearchConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Application {

    public static void setupEndpoints(HttpServer server) {
        String packageRelPath = Application.class.getPackage().getName().replace(".", "/");
        ClassHunter classHunter = ComponentContainer.getInstance().getClassHunter();
        ClassHunter.SearchResult endpoints = classHunter.findBy(
                SearchConfig.forResources(packageRelPath).by(
                        ClassCriteria.create().allThoseThatMatch(cls -> cls.isAnnotationPresent(Path.class))));

        endpoints.getClasses().forEach(cls -> {
            Arrays.stream(cls.getMethods()).forEach(method -> {
                Path path = method.getAnnotation(Path.class);

                if (path != null) {
                    Path classPath = cls.getAnnotation(Path.class);
                    String endpoint = classPath != null ? classPath.value() + path.value() : path.value();

                    server.createContext(endpoint, (exchange -> {
                        try {
                            Object controller = Injector.getService(cls);
                            method.invoke(controller, exchange);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Endpoint: " + path.value() + " Method: " +
                                method.getName());
                    }));

                }
            });
        });
    }

    public static void main(String[] args) throws IOException {
        int serverPort = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        Injector.startApplication(Application.class);

        setupEndpoints(server);

        HttpContext context = server.createContext("/api/hello", (exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, List<String>> params = ApiUtils.splitQuery(exchange.getRequestURI().getRawQuery());
                String noNameText = "Anonymous";
                String name = params.getOrDefault("name", List.of(noNameText)).stream().findFirst().orElse(noNameText);
                String respText = String.format("Hello %s!", name);
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));

        context.setAuthenticator(new BasicAuthenticator("myrealm") {
            @Override
            public boolean checkCredentials(String user, String pwd) {
                return user.equals("admin") && pwd.equals("admin");
            }
        });

        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
