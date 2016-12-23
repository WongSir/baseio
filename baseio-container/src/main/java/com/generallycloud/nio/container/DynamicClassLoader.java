/*
 * Copyright 2015 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.nio.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.generallycloud.nio.common.FileUtil;
import com.generallycloud.nio.common.Logger;
import com.generallycloud.nio.common.LoggerFactory;
import com.generallycloud.nio.common.LoggerUtil;

public class DynamicClassLoader extends ClassLoader {

	private Logger					logger		= LoggerFactory.getLogger(DynamicClassLoader.class);
	private Map<String, ClassEntry>	clazzEntries	= new HashMap<String, ClassEntry>();
	private ClassLoader				parent;
	private ClassLoader				systemClassLoader;

	public DynamicClassLoader() {
		
		ClassLoader parent = getParent();

		if (parent == null) {
			parent = getSystemClassLoader();
		}

		this.parent = parent;

		this.systemClassLoader = this.getClass().getClassLoader();
	}

	private Class<?> findLoadedClass0(String name) throws ClassNotFoundException {

		ClassEntry entry = clazzEntries.get(name);

		if (entry == null) {
			return null;
		}

		return entry.loadedClass;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {

		Class<?> clazz = findLoadedClass0(name);

		if (clazz == null) {
			return loadClass(name);
		}

		return clazz;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		
		Class<?> clazz = findLoadedClass0(name);

		if (clazz != null) {

			return clazz;
		}
		
		clazz = defineClass(name);

		if (clazz != null) {

			return clazz;
		}
		
		try {
			return parent.loadClass(name);
		} catch (Throwable e) {
			return systemClassLoader.loadClass(name);
		}
	}

	public void scan(String file) throws IOException {
		this.scan(new File(file));
	}

	public void scan(File file) throws IOException {
		this.scan0(file);
		LoggerUtil.prettyNIOServerLog(logger, "预加载 class 字节码到缓存[ {} ]个 ", clazzEntries.size());
	}

	private void scan0(File file) throws IOException {
		
		if (!file.exists()) {
			LoggerUtil.prettyNIOServerLog(logger, "文件/文件夹 [ {} ] 不存在", file.getAbsoluteFile());
			return;
		} 
		
		if (file.isDirectory()) {
			
			File[] files = file.listFiles();
			
			for (File _file : files) {
				scan0(_file);
			}
			
			return;
		}
		
		String fileName = file.getName();
		
		if (fileName.endsWith(".jar")) {
			scanZip(new JarFile(file));
		}
		
	}

	private void scanZip(JarFile file) throws IOException {

		try {
			
			LoggerUtil.prettyNIOServerLog(logger, "加载文件 [ {} ]", file.getName());

			Enumeration<JarEntry> entries = file.entries();
			
			for (; entries.hasMoreElements();) {
				
				JarEntry entry = entries.nextElement();

				if (entry.isDirectory()) {
					continue;
				}
				
				String name = entry.getName();
				
				if (name.endsWith(".class") && !matchSystem(name)) {
					storeClass(file, name, entry);
				}
			}
		} finally {

			file.close();
		}
	}

	public boolean matchSystem(String name) {

		return name.startsWith("java") || name.startsWith("sun") || name.startsWith("com/sun") || matchExtend(name);

	}

	public boolean matchExtend(String name) {
		return false;
	}

	private void storeClass(JarFile file, String name, JarEntry entry) throws IOException {
		
		String className = name.replace('/', '.').replace(".class", "");
		
		if (clazzEntries.containsKey(className)) {
			throw new DuplicateClassException(className);
		}
		
		try {
			
			parent.loadClass(className);
			
			throw new DuplicateClassException(className);
		} catch (ClassNotFoundException e) {
		}

		InputStream inputStream = file.getInputStream(entry);
		
		byte[] binaryContent = FileUtil.toByteArray(inputStream, entry.getSize());

		ClassEntry classEntry = new ClassEntry();

		classEntry.binaryContent = binaryContent;

		classEntry.className = className;

		clazzEntries.put(className, classEntry);

	}

	private Class<?> defineClass(String name) throws ClassNotFoundException {
		
		ClassEntry entry = clazzEntries.get(name);

		if (entry == null) {
			return null;
		}

		return defineClass(entry);
	}

	private Class<?> defineClass(ClassEntry entry) {

		String name = entry.className;
		
		entry.loadedClass = defineClass(name, entry.binaryContent, 0, entry.binaryContent.length);

		LoggerUtil.prettyNIOServerLog(logger, "define class [ {} ]", name);

		return entry.loadedClass;
	}

	public Class<?> forName(String name) throws ClassNotFoundException {
		return this.findClass(name);
	}

	class ClassEntry {

		private String		className;

		private byte[]	binaryContent;

		private Class<?>	loadedClass;

	}

	public void unloadClassLoader() {
		
		Collection<ClassEntry> es = clazzEntries.values();
		
		for(ClassEntry e : es){
			unloadClass(e.loadedClass);
		}
	}

	private void unloadClass(Class<?> clazz) {
		
		Field[] fields = clazz.getDeclaredFields();
		
		for (Field field : fields) {
			
			if (!Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			
			try {
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				field.set(null, null);
			} catch (Throwable e) {
				logger.debug(e);
			}
		}
	}

}