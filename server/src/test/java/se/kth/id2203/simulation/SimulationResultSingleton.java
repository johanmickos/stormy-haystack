/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.simulation;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freely adapted from <http://surguy.net/articles/communication-across-classloaders.xml>.
 * 
 * @author Lars Kroll <lkroll@kth.se>
 */
public class SimulationResultSingleton implements SimulationResultMap {

    public static SimulationResultMap instance = null;

    public synchronized static SimulationResultMap getInstance() {
        ClassLoader myClassLoader = SimulationResultSingleton.class.getClassLoader();
        if (instance == null) {
            if (!myClassLoader.toString().startsWith("sun.")) {
                try {
                    ClassLoader parentClassLoader = SimulationResultSingleton.class.getClassLoader().getParent();
                    Class otherClassInstance = parentClassLoader.loadClass(SimulationResultSingleton.class.getName());
                    Method getInstanceMethod = otherClassInstance.getDeclaredMethod("getInstance", new Class[]{});
                    Object otherAbsoluteSingleton = getInstanceMethod.invoke(null, new Object[]{});
                    instance = (SimulationResultMap) Proxy.newProxyInstance(myClassLoader,
                            new Class[]{SimulationResultMap.class},
                            new PassThroughProxyHandler(otherAbsoluteSingleton));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                instance = new SimulationResultSingleton();
            }
        }

        return instance;
    }

    private SimulationResultSingleton() {
    }

    private ConcurrentHashMap<String, Object> entries = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object o) {
        entries.put(key, o);
    }

    @Override
    public <T> T get(String key, Class<T> tpe) {
        return (T) entries.get(key);
    }

}
