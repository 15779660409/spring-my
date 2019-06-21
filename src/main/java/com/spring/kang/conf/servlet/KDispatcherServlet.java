package com.spring.kang.conf.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spring.kang.conf.anno.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
public class KDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    private final Properties properties = new Properties();

    private final List<String> classNames = new ArrayList<>();

    private final Map<String, Object> ioc = new HashMap<>();

    private final Map<String, Method> handlerMapping = new HashMap<>();

    private Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        Method method = handlerMapping.get(url);
        PrintWriter writer = resp.getWriter();
        if(null == method){
            writer.write("404 NOT FOUND");
            return;
        }
        try {
            String result = doDispatcher(method,req,resp);
            writer.write(result);
        } catch (Exception e) {
            e.printStackTrace();
            writer.write("500 NOT FOUND");
        }


    }

    private String doDispatcher(Method method,HttpServletRequest req,HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz = method.getDeclaringClass();
        List<Object> params = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType().equals(HttpServletRequest.class)) {//处理request对象
                params.add(req);
            } else if (parameter.getType().equals(HttpServletResponse.class)) {//处理reponse对象
                params.add(resp);
            } else {
                String paramsName = null;
                Object paramsValue = null;
                if (parameter.isAnnotationPresent(KRequestParam.class)) {
                    KRequestParam annotation = parameter.getAnnotation(KRequestParam.class);
                    paramsName = annotation.value();
                    if(parameter.getType().isAssignableFrom(Integer.class)){
                        paramsValue = Integer.valueOf(req.getParameter(paramsName));
                    }else {
                        paramsValue = req.getParameter(paramsName);
                    }

                }
                params.add(paramsValue);
            }
        }
        Object[] objects = params.toArray();
        Object invoke = method.invoke(ioc.get(toFirstLowerCase(clazz.getSimpleName())), objects);
        String result = gson.toJson(invoke);
        return result;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.加载配置文件
        doLocalConfig(config.getInitParameter(LOCATION));

        // 2.扫描所有相关的类
        doScanner(properties.getProperty("scanPackage"));

        // 3.初始化所有相关类的实例，保存到ioc容器中
        doInstance();

        // 4.依赖注入
        doAutowrited();

        // 5.handlerMapping关系映射
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(KRequestMapping.class)) {
                continue;
            }
            KRequestMapping kRequestMapping = clazz.getAnnotation(KRequestMapping.class);
            String baseUrl = kRequestMapping.value();
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(KRequestMapping.class)) {
                    continue;
                }
                kRequestMapping = method.getAnnotation(KRequestMapping.class);
                String url = kRequestMapping.value();
                url = (baseUrl + "/" + url).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapping is: " + url + "   " + method);
            }

        }
    }

    private void doAutowrited() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(KAutowrited.class)) {
                    continue;
                }
                KAutowrited kAutowrited = field.getAnnotation(KAutowrited.class);
                String beanName = kAutowrited.value().trim();
                if ("".equals(beanName)) {
                    beanName = toFirstLowerCase(field.getType().getSimpleName());
                }
                try {
                    field.setAccessible(true);//暴力反射
                    field.set(entry.getValue(), ioc.get(beanName));
                    System.out.println(beanName + "注入成功");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }

        }

    }

    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(KController.class)) {
                    String beanName = toFirstLowerCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(KService.class)) {
                    KService kService = clazz.getAnnotation(KService.class);
                    String beanName = kService.value();
                    //如果用户设置了名字，就用用户自己设置的
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //如果没设，就按类名第一个字母小写
                    for (Class<?> i : clazz.getInterfaces()) {
                        ioc.put(toFirstLowerCase(i.getSimpleName()), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String toFirstLowerCase(String name) {
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "/" + file.getName());
            } else {
                classNames.add(packageName.replaceAll("/", "\\.") + "." + file.getName().replaceAll(".class", ""));
            }
        }

    }

    private void doLocalConfig(String location) {
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
