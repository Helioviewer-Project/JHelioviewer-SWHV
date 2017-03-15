package org.helioviewer.jhv.platform;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.helioviewer.jhv.base.logging.Log;

class OSXAdapter implements InvocationHandler {

    static final boolean JAVA9 = Double.parseDouble(System.getProperty("java.specification.version")) >= 1.9;

    protected final Object targetObject;
    protected final Method targetMethod;
    protected final String proxySignature;

    static Object macOSXApplication;

    // Pass this method an Object and Method equipped to perform application shutdown logic
    // The method passed should return a boolean stating whether or not the quit should occur
    public static void setQuitHandler(Object target, Method quitHandler) {
        setHandler(new OSXAdapter("handleQuit", target, quitHandler));
    }

    // Pass this method an Object and Method equipped to display application info
    // They will be called when the About menu item is selected from the application menu
    public static void setAboutHandler(Object target, Method aboutHandler) {
        boolean enableAboutMenu = (target != null && aboutHandler != null);
        if (enableAboutMenu) {
            setHandler(new OSXAdapter("handleAbout", target, aboutHandler));
        }
        // If we're setting a handler, enable the About menu item by calling
        // com.apple.eawt.Application reflectively
        try {
            Method enableAboutMethod = macOSXApplication.getClass().getDeclaredMethod("setEnabledAboutMenu", boolean.class);
            enableAboutMethod.invoke(macOSXApplication, enableAboutMenu);
        } catch (Exception e) {
            Log.error("OSXAdapter could not access the About Menu", e);
        }
    }

    // Pass this method an Object and a Method equipped to display application options
    // They will be called when the Preferences menu item is selected from the application menu
    public static void setPreferencesHandler(Object target, Method prefsHandler) {
        boolean enablePrefsMenu = (target != null && prefsHandler != null);
        if (enablePrefsMenu) {
            setHandler(new OSXAdapter("handlePreferences", target, prefsHandler));
        }
        // If we're setting a handler, enable the Preferences menu item by calling
        // com.apple.eawt.Application reflectively
        try {
            Method enablePrefsMethod = macOSXApplication.getClass().getDeclaredMethod("setEnabledPreferencesMenu", boolean.class);
            enablePrefsMethod.invoke(macOSXApplication, enablePrefsMenu);
        } catch (Exception e) {
            Log.error("OSXAdapter could not access the About Menu", e);
        }
    }

    // Pass this method an Object and a Method equipped to handle document events from the Finder
    // Documents are registered with the Finder via the CFBundleDocumentTypes dictionary in the
    // application bundle's Info.plist
    public static void setFileHandler(Object target, Method fileHandler) {
        setHandler(new OSXAdapter("handleOpenFile", target, fileHandler) {
            // Override OSXAdapter.callTarget to send information on the
            // file to be opened
            @Override
            public boolean callTarget(Object appleEvent) {
                if (appleEvent != null) {
                    try {
                        Method getFilenameMethod = appleEvent.getClass().getDeclaredMethod("getFilename", (Class[]) null);
                        String filename = (String) getFilenameMethod.invoke(appleEvent, (Object[]) null);
                        targetMethod.invoke(targetObject, filename);
                    } catch (Exception ignore) {
                    }
                }
                return true;
            }
        });
    }

    // setHandler creates a Proxy object from the passed OSXAdapter and adds it as an ApplicationListener
    public static void setHandler(OSXAdapter adapter) {
        try {
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            if (macOSXApplication == null) {
                macOSXApplication = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
            }
            Class<?> applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");
            Method addListenerMethod = applicationClass.getDeclaredMethod("addApplicationListener", applicationListenerClass);
            // Create a proxy object around this handler that can be reflectively added as an Apple ApplicationListener
            ClassLoader classLoader = OSXAdapter.class.getClassLoader();
            if (classLoader != null) { // impossible
                Object osxAdapterProxy = Proxy.newProxyInstance(classLoader, new Class<?>[] { applicationListenerClass }, adapter);
                addListenerMethod.invoke(macOSXApplication, osxAdapterProxy);
            }
        } catch (ClassNotFoundException e) {
            Log.error("This version of Mac OS X does not support the Apple EAWT. ApplicationEvent handling has been disabled", e);
        } catch (Exception e) { // Likely a NoSuchMethodException or an IllegalAccessException loading/invoking eawt.Application methods
            Log.error("Mac OS X Adapter could not talk to EAWT", e);
        }
    }

    // Each OSXAdapter has the name of the EAWT method it intends to listen for (handleAbout, for example),
    // the Object that will ultimately perform the task, and the Method to be called on that Object
    protected OSXAdapter(String _proxySignature, Object _targetObject, Method _targetMethod) {
        proxySignature = _proxySignature;
        targetObject = _targetObject;
        targetMethod = _targetMethod;
    }

    // Override this method to perform any operations on the event
    // that comes with the various callbacks
    // See setFileHandler above for an example
    public boolean callTarget(Object appleEvent) throws InvocationTargetException, IllegalAccessException {
        Object result = targetMethod.invoke(targetObject, (Object[]) null);
        return result == null || Boolean.parseBoolean(result.toString());
    }

    public boolean callTarget(Object appleEvent, Object response) throws InvocationTargetException, IllegalAccessException {
        Object result = targetMethod.invoke(targetObject, (Object[]) null);
        return result == null || Boolean.parseBoolean(result.toString());
    }

    // InvocationHandler implementation
    // This is the entry point for our proxy object; it is called every time an ApplicationListener method is invoked
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isCorrectMethod(method, args)) {
            boolean handled = args.length == 1 ? callTarget(args[0]) : callTarget(args[0], args[1]);
            if (!JAVA9)
                setApplicationEventHandled(args[0], handled);
        }
        // All of the ApplicationListener methods are void; return null regardless of what happens
        return null;
    }

    // Compare the method that was called to the intended method when the OSXAdapter instance was created
    // (e.g. handleAbout, handleQuit, handleOpenFile, etc.)
    protected boolean isCorrectMethod(Method method, Object[] args) {
        return targetMethod != null && args.length > 0 && proxySignature.equals(method.getName());
    }

    // It is important to mark the ApplicationEvent as handled and cancel the default behavior
    // This method checks for a boolean result from the proxy method and sets the event accordingly
    protected static void setApplicationEventHandled(Object event, boolean handled) {
        if (event != null) {
            try {
                Method setHandledMethod = event.getClass().getDeclaredMethod("setHandled", boolean.class);
                // If the target method returns a boolean, use that as a hint
                setHandledMethod.invoke(event, handled);
            } catch (Exception e) {
                Log.error("OSXAdapter was unable to handle an ApplicationEvent: " + event, e);
            }
        }
    }

}
