package com.nextlabs.rms.hibernate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public Main() {
    }

    public void test() throws IOException, ClassNotFoundException {
        DbSession session = HibernateUtils.newSession();
        session.beginTransaction();
        try {
            List<String> list = getClassesForPackage("com.nextlabs.rms.hibernate.model");
            for (String className : list) {
                if ("com.nextlabs.rms.hibernate.model.Constants".equals(className)) {
                    continue;
                }
                Class<?> clazz = Class.forName(className);
                Criteria criteria = session.createCriteria(clazz);
                criteria.setMaxResults(1);
                criteria.list();
            }
        } finally {
            session.close();
        }
    }

    public static void main(String[] args) {
        try {
            new Main().test();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static List<String> getClassesForPackage(String packageName) throws IOException {
        String relPath = packageName.replace('.', '/');
        URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);

        ArrayList<String> list = new ArrayList<String>();
        if (resource.toString().startsWith("jar:")) {
            processJarfile(resource, packageName, list);
        } else {
            processDirectory(new File(resource.getPath()), packageName, list);
        }
        return list;
    }

    private static void processDirectory(File directory, String pkgname, List<String> list) {
        String[] files = directory.list();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            String className = null;
            if (fileName.endsWith(".class")) {
                className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
            }
            if (className != null) {
                list.add(className);
            }
            File subdir = new File(directory, fileName);
            if (subdir.isDirectory()) {
                processDirectory(subdir, pkgname + '.' + fileName, list);
            }
        }
    }

    private static void processJarfile(URL resource, String pkgname, List<String> list) throws IOException {
        String relPath = pkgname.replace('.', '/');
        String resPath = resource.getPath();
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                String className = null;
                if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                    className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                }
                if (className != null) {
                    list.add(className);
                }
            }
        }
    }
}
