package com.stockanalyzer;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.WebResourceRoot;

import java.io.File;

public class EmbeddedTomcat {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();

        String webappDir = new File("src/main/webapp").getAbsolutePath();
        Context ctx = tomcat.addWebapp("", webappDir);

        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(
                resources, "/WEB-INF/classes",
                new File("build/classes/java/main").getAbsolutePath(), "/"));
        resources.addPreResources(new DirResourceSet(
                resources, "/WEB-INF/classes",
                new File("build/resources/main").getAbsolutePath(), "/"));
        ctx.setResources(resources);

        tomcat.start();
        System.out.println("Tomcat started on http://localhost:8080");
        tomcat.getServer().await();
    }
}
