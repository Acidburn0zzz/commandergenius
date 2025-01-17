/*
 * Copyright (C) 2008 The Android Open Source Project
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

/* This is GLSurfaceView class ripped out of Android 2.1 sources,
   fixed with a hammer to work with libSDL port */

package net.sourceforge.clonekeenplus;

import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.app.KeyguardManager;

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying OpenGL rendering.
 * <p>
 * A GLSurfaceView provides the following features:
 * <p>
 * <ul>
 * <li>Manages a surface, which is a special piece of memory that can be
 * composited into the Android view system.
 * <li>Manages an EGL display, which enables OpenGL to render into a surface.
 * <li>Accepts a user-provided Renderer object that does the actual rendering.
 * <li>Renders on a dedicated thread to decouple rendering performance from the
 * UI thread.
 * <li>Supports both on-demand and continuous rendering.
 * <li>Optionally wraps, traces, and/or error-checks the renderer's OpenGL calls.
 * </ul>
 *
 * <h3>Using GLSurfaceView</h3>
 * <p>
 * Typically you use GLSurfaceView by subclassing it and overriding one or more of the
 * View system input event methods. If your application does not need to override event
 * methods then GLSurfaceView can be used as-is. For the most part
 * GLSurfaceView behavior is customized by calling "set" methods rather than by subclassing.
 * For example, unlike a regular View, drawing is delegated to a separate Renderer object which
 * is registered with the GLSurfaceView
 * using the {@link #setRenderer(Renderer)} call.
 * <p>
 * <h3>Initializing GLSurfaceView</h3>
 * All you have to do to initialize a GLSurfaceView is call {@link #setRenderer(Renderer)}.
 * However, if desired, you can modify the default behavior of GLSurfaceView by calling one or
 * more of these methods before calling setRenderer:
 * <ul>
 * <li>{@link #setDebugFlags(int)}
 * <li>{@link #setEGLConfigChooser(boolean)}
 * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
 * <li>{@link #setEGLConfigChooser(int, int, int, int, int, int)}
 * <li>{@link #setGLWrapper(GLWrapper)}
 * </ul>
 * <p>
 * <h4>Choosing an EGL Configuration</h4>
 * A given Android device may support multiple possible types of drawing surfaces.
 * The available surfaces may differ in how may channels of data are present, as
 * well as how many bits are allocated to each channel. Therefore, the first thing
 * GLSurfaceView has to do when starting to render is choose what type of surface to use.
 * <p>
 * By default GLSurfaceView chooses an available surface that's closest to a 16-bit R5G6B5 surface
 * with a 16-bit depth buffer and no stencil. If you would prefer a different surface (for example,
 * if you do not need a depth buffer) you can override the default behavior by calling one of the
 * setEGLConfigChooser methods.
 * <p>
 * <h4>Debug Behavior</h4>
 * You can optionally modify the behavior of GLSurfaceView by calling
 * one or more of the debugging methods {@link #setDebugFlags(int)},
 * and {@link #setGLWrapper}. These methods may be called before and/or after setRenderer, but
 * typically they are called before setRenderer so that they take effect immediately.
 * <p>
 * <h4>Setting a Renderer</h4>
 * Finally, you must call {@link #setRenderer} to register a {@link Renderer}.
 * The renderer is
 * responsible for doing the actual OpenGL rendering.
 * <p>
 * <h3>Rendering Mode</h3>
 * Once the renderer is set, you can control whether the renderer draws
 * continuously or on-demand by calling
 * {@link #setRenderMode}. The default is continuous rendering.
 * <p>
 * <h3>Activity Life-cycle</h3>
 * A GLSurfaceView must be notified when the activity is paused and resumed. GLSurfaceView clients
 * are required to call {@link #onPause()} when the activity pauses and
 * {@link #onResume()} when the activity resumes. These calls allow GLSurfaceView to
 * pause and resume the rendering thread, and also allow GLSurfaceView to release and recreate
 * the OpenGL display.
 * <p>
 * <h3>Handling events</h3>
 * <p>
 * To handle an event you will typically subclass GLSurfaceView and override the
 * appropriate method, just as you would with any other View. However, when handling
 * the event, you may need to communicate with the Renderer object
 * that's running in the rendering thread. You can do this using any
 * standard Java cross-thread communication mechanism. In addition,
 * one relatively easy way to communicate with your renderer is
 * to call
 * {@link #queueEvent(Runnable)}. For example:
 * <pre class="prettyprint">
 * class MyGLSurfaceView extends GLSurfaceView {
 *
 *     private MyRenderer mMyRenderer;
 *
 *     public void start() {
 *         mMyRenderer = ...;
 *         setRenderer(mMyRenderer);
 *     }
 *
 *     public boolean onKeyDown(int keyCode, KeyEvent event) {
 *         if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
 *             queueEvent(new Runnable() {
 *                 // This method will be called on the rendering
 *                 // thread:
 *                 public void run() {
 *                     mMyRenderer.handleDpadCenter();
 *                 }});
 *             return true;
 *         }
 *         return super.onKeyDown(keyCode, event);
 *     }
 * }
 * </pre>
 *
 */
public class GLSurfaceView_SDL extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * The renderer only renders
     * when the surface is created, or when {@link #requestRender} is called.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     */
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    /**
     * The renderer is called
     * continuously to re-render the scene.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     * @see #requestRender()
     */
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    /**
     * Check glError() after every GL call and throw an exception if glError indicates
     * that an error has occurred. This can be used to help track down which OpenGL ES call
     * is causing an error.
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    public final static int DEBUG_CHECK_GL_ERROR = 1;

    /**
     * Log GL calls to the system log at "verbose" level with tag "GLSurfaceView".
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    public final static int DEBUG_LOG_GL_CALLS = 2;

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public GLSurfaceView_SDL(Context context) {
        super(context);
        init();
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public GLSurfaceView_SDL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        mKeyguardManager = ((KeyguardManager)getContext().getSystemService(Context.KEYGUARD_SERVICE));
    }

    /**
     * Set the glWrapper. If the glWrapper is not null, its
     * {@link GLWrapper#wrap(GL)} method is called
     * whenever a surface is created. A GLWrapper can be used to wrap
     * the GL object that's passed to the renderer. Wrapping a GL
     * object enables examining and modifying the behavior of the
     * GL calls made by the renderer.
     * <p>
     * Wrapping is typically used for debugging purposes.
     * <p>
     * The default value is null.
     * @param glWrapper the new GLWrapper
     */
    public void setGLWrapper(GLWrapper glWrapper) {
        mGLWrapper = glWrapper;
    }

    /**
     * Set the debug flags to a new value. The value is
     * constructed by OR-together zero or more
     * of the DEBUG_CHECK_* constants. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     * @param debugFlags the new debug flags
     * @see #DEBUG_CHECK_GL_ERROR
     * @see #DEBUG_LOG_GL_CALLS
     */
    public void setDebugFlags(int debugFlags) {
        mDebugFlags = debugFlags;
    }

    /**
     * Get the current value of the debug flags.
     * @return the current value of the debug flags.
     */
    public int getDebugFlags() {
        return mDebugFlags;
    }

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a GLSurfaceView.
     * <p>The following GLSurfaceView methods can only be called <em>before</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #setEGLConfigChooser(boolean)}
     * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
     * <li>{@link #setEGLConfigChooser(int, int, int, int, int, int)}
     * </ul>
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    public void setRenderer(Renderer renderer) {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = getEglConfigChooser(16, false, false, false);
        }
        mGLThread = new GLThread(renderer);
        mGLThread.start();
    }

    /**
     * Install a custom EGLConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     * @param configChooser
     */
    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
        mEGLConfigChooser = configChooser;
    }

    /**
     * Install a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
      * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     *
     * @param needDepth
     */
    public void setEGLConfigChooser(int bpp, boolean needDepth, boolean stencil, boolean gles2) {
        setEGLConfigChooser(getEglConfigChooser(bpp, needDepth, stencil, gles2));
    }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified component sizes, and as close
     * to the specified component sizes as possible.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16 bits as possible.
     *
     */
    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize,
            int alphaSize, int depthSize, int stencilSize, boolean gles2) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize, gles2));
    }
    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RENDERMODE_CONTINUOUSLY.
     * <p>
     * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(Renderer)}
     *
     * @param renderMode one of the RENDERMODE_X constants
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public void setRenderMode(int renderMode) {
        mGLThread.setRenderMode(renderMode);
    }

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     * @return the current rendering mode.
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public int getRenderMode() {
        return mGLThread.getRenderMode();
    }

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link #RENDERMODE_WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    public void requestRender() {
        mGLThread.requestRender();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        mGLThread.surfaceCreated();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mGLThread.surfaceDestroyed();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mGLThread.onWindowResize(w, h);
    }

    /**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     * Must not be called before a renderer has been set.
     */
    public void onPause() {
        mGLThread.onPause();
    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed. Calling this method will
     * recreate the OpenGL display and resume the rendering
     * thread.
     * Must not be called before a renderer has been set.
     */
    public void onResume() {
        mGLThread.onResume();
    }

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceView.
     * Must not be called before a renderer has been set.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGLThread.requestExitAndWait();
    }

    // ----------------------------------------------------------------------

    /**
     * An interface used to wrap a GL interface.
     * <p>Typically
     * used for implementing debugging and tracing on top of the default
     * GL interface. You would typically use this by creating your own class
     * that implemented all the GL methods by delegating to another GL instance.
     * Then you could add your own behavior before or after calling the
     * delegate. All the GLWrapper would do was instantiate and return the
     * wrapper GL instance:
     * <pre class="prettyprint">
     * class MyGLWrapper implements GLWrapper {
     *     GL wrap(GL gl) {
     *         return new MyGLImplementation(gl);
     *     }
     *     static class MyGLImplementation implements GL,GL10,GL11,... {
     *         ...
     *     }
     * }
     * </pre>
     * @see #setGLWrapper(GLWrapper)
     */
    public interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        GL wrap(GL gl);
    }

    /**
     * A generic renderer interface.
     * <p>
     * The renderer is responsible for making OpenGL calls to render a frame.
     * <p>
     * GLSurfaceView clients typically create their own classes that implement
     * this interface, and then call {@link GLSurfaceView#setRenderer} to
     * register the renderer with the GLSurfaceView.
     * <p>
     * <h3>Threading</h3>
     * The renderer will be called on a separate thread, so that rendering
     * performance is decoupled from the UI thread. Clients typically need to
     * communicate with the renderer from the UI thread, because that's where
     * input events are received. Clients can communicate using any of the
     * standard Java techniques for cross-thread communication, or they can
     * use the {@link GLSurfaceView#queueEvent(Runnable)} convenience method.
     * <p>
     * <h3>EGL Context Lost</h3>
     * There are situations where the EGL rendering context will be lost. This
     * typically happens when device wakes up after going to sleep. When
     * the EGL context is lost, all OpenGL resources (such as textures) that are
     * associated with that context will be automatically deleted. In order to
     * keep rendering correctly, a renderer must recreate any lost resources
     * that it still needs. The {@link #onSurfaceCreated(GL10, EGLConfig)} method
     * is a convenient place to do this.
     *
     *
     * @see #setRenderer(Renderer)
     */
    public static interface SwapBuffersCallback {
        public boolean SwapBuffers();
    }

    public static abstract class Renderer {
        /**
         * Called when the surface is created or recreated.
         * <p>
         * Called when the rendering thread
         * starts and whenever the EGL context is lost. The context will typically
         * be lost when the Android device awakes after going to sleep.
         * <p>
         * Since this method is called at the beginning of rendering, as well as
         * every time the EGL context is lost, this method is a convenient place to put
         * code to create resources that need to be created when the rendering
         * starts, and that need to be recreated when the EGL context is lost.
         * Textures are an example of a resource that you might want to create
         * here.
         * <p>
         * Note that when the EGL context is lost, all OpenGL resources associated
         * with that context will be automatically deleted. You do not need to call
         * the corresponding "glDelete" methods such as glDeleteTextures to
         * manually delete these lost resources.
         * <p>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param config the EGLConfig of the created surface. Can be used
         * to create matching pbuffers.
         */
        public abstract void onSurfaceCreated(GL10 gl, EGLConfig config);

        public abstract void onSurfaceDestroyed();

        /**
         * Called when the surface changed size.
         * <p>
         * Called after the surface is created and whenever
         * the OpenGL ES surface size changes.
         * <p>
         * Typically you will set your viewport here. If your camera
         * is fixed then you could also set your projection matrix here:
         * <pre class="prettyprint">
         * void onSurfaceChanged(GL10 gl, int width, int height) {
         *     gl.glViewport(0, 0, width, height);
         *     // for a fixed camera, set the projection too
         *     float ratio = (float) width / height;
         *     gl.glMatrixMode(GL10.GL_PROJECTION);
         *     gl.glLoadIdentity();
         *     gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
         * }
         * </pre>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param width
         * @param height
         */
        public abstract void onSurfaceChanged(GL10 gl, int width, int height);

        /**
         * Called to draw the current frame.
         * <p>
         * This method is responsible for drawing the current frame.
         * <p>
         * The implementation of this method typically looks like this:
         * <pre class="prettyprint">
         * void onDrawFrame(GL10 gl) {
         *     gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
         *     //... other gl calls to render the scene ...
         * }
         * </pre>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         */
        public abstract void onDrawFrame(GL10 gl);
        
        public boolean SwapBuffers() {
            if( mSwapBuffersCallback != null )
                return mSwapBuffersCallback.SwapBuffers();
            return false;
        }
        
        public void setSwapBuffersCallback( SwapBuffersCallback c ) {
            mSwapBuffersCallback = c;
        }

        private SwapBuffersCallback mSwapBuffersCallback = null;
    }

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link GLSurfaceView#setEGLConfigChooser(EGLConfigChooser)}
     */
    public interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL10#eglChooseConfig} and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         * @param egl the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
        public boolean isGles2Required();
    }

    private static abstract class BaseConfigChooser
            implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = configSpec;
        }
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config);

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config);
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs);

        protected int[] mConfigSpec;
    }

    private static class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize, boolean isGles2) {
            super(new int[] {EGL10.EGL_NONE}); // Get all possible configs
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
            mIsGles2 = isGles2;
       }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs) {
            EGLConfig closestConfig = null;
            int closestDistance = 1000;
            String cfglog = "";
            int idx = 0;
            int selectidx = -1;

            Log.v("SDL", "Desired GL config: " + "R" + mRedSize + "G" + mGreenSize + "B" + mBlueSize + "A" + mAlphaSize + " depth " + mDepthSize + " stencil " + mStencilSize + " type " + (mIsGles2 ? "GLES2" : "GLES"));
            for(EGLConfig config : configs) {
                if ( config == null )
                    continue;
                int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                         EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                          EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                int rendertype = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RENDERABLE_TYPE, 0);
                int desiredtype = mIsGles2 ? EGL_OPENGL_ES2_BIT : EGL_OPENGL_ES_BIT;
                int nativeRender = findConfigAttrib(egl, display, config,
                        EGL10.EGL_NATIVE_RENDERABLE, 0);
                int caveat = findConfigAttrib(egl, display, config,
                        EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_NONE);
                int distance = Math.abs(r - mRedSize) + Math.abs(g - mGreenSize) + Math.abs(b - mBlueSize);
                int dist1 = distance;
                if( mAlphaSize - a > 0 )
                    distance += mAlphaSize - a;
                else if( mAlphaSize - a < 0 )
                    distance += 1; // Small penalty if we don't need alpha channel but it is present
                int dist2 = distance;
                if( (d > 0) != (mDepthSize > 0) )
                    distance += (mDepthSize > 0) ? 5 : 1; // Small penalty if we don't need zbuffer but it is present
                int dist3 = distance;
                if( (s > 0) != (mStencilSize > 0) )
                    distance += (mStencilSize > 0) ? 5 : 1; // Small penalty if we don't need stencil buffer but it is present
                int dist4 = distance;
                if( (rendertype & desiredtype) == 0 )
                    distance += 5;
                int dist5 = distance;
                if( caveat == EGL10.EGL_SLOW_CONFIG )
                    distance += 4;
                if( caveat == EGL10.EGL_NON_CONFORMANT_CONFIG ) // dunno what that means, probably R and B channels swapped
                    distance += 1;

                String cfgcur = "R" + r + "G" + g + "B" + b + "A" + a + " depth " + d + " stencil " + s +
                    " type " + rendertype + " (";
                if((rendertype & EGL_OPENGL_ES_BIT) != 0)
                    cfgcur += "GLES";
                if((rendertype & EGL_OPENGL_ES2_BIT) != 0)
                    cfgcur += " GLES2";
                if((rendertype & EGL_OPENGL_BIT) != 0)
                    cfgcur += " OPENGL";
                if((rendertype & EGL_OPENVG_BIT) != 0)
                    cfgcur += " OPENVG";
                cfgcur += ")";
                cfgcur += " caveat " + (caveat == EGL10.EGL_NONE ? "none" :
                						(caveat == EGL10.EGL_SLOW_CONFIG ? "SLOW" :
                						caveat == EGL10.EGL_NON_CONFORMANT_CONFIG ? "non-conformant" :
                						String.valueOf(caveat)));
                cfgcur += " nr " + nativeRender;
                cfgcur += " pos " + distance + " (" + dist1 + "," +  dist2 + "," + dist3 + "," + dist4 + "," + dist5 + ")";
                Log.v("SDL", "GL config " + idx + ": " + cfgcur);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestConfig = config;
                    cfglog = new String(cfgcur);
                    selectidx = idx;
                }
                idx += 1;
            }
            Log.v("SDL", "GLSurfaceView_SDL::EGLConfigChooser::chooseConfig(): selected " + selectidx + ": " + cfglog );
            return closestConfig;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {
            mValue[0] = -1;
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            Log.w("SDL", "GLSurfaceView_SDL::EGLConfigChooser::findConfigAttrib(): attribute doesn't exist: " + attribute);
            return defaultValue;
        }

        public boolean isGles2Required()
        {
            return mIsGles2;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        protected boolean mIsGles2 = false;

        public static final int EGL_OPENGL_ES_BIT = 1;
        public static final int EGL_OPENVG_BIT = 2;
        public static final int EGL_OPENGL_ES2_BIT = 4;
        public static final int EGL_OPENGL_BIT = 8;
        }

    /**
     * This class will choose a supported surface as close to
     * RGB565 as possible, with or without a depth buffer.
     *
     */
    private static class SimpleEGLConfigChooser16 extends ComponentSizeChooser {
        public SimpleEGLConfigChooser16(boolean withDepthBuffer, boolean stencil, boolean gles2) {
            super(4, 4, 4, 0, withDepthBuffer ? 16 : 0, stencil ? 8 : 0, gles2);
            // Adjust target values. This way we'll accept a 4444 or
            // 555 buffer if there's no 565 buffer available.
            mRedSize = 5;
            mGreenSize = 6;
            mBlueSize = 5;
        }
    }

    private static class SimpleEGLConfigChooser24 extends ComponentSizeChooser {
        public SimpleEGLConfigChooser24(boolean withDepthBuffer, boolean stencil, boolean gles2) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, stencil ? 8 : 0, gles2);
            mRedSize = 8;
            mGreenSize = 8;
            mBlueSize = 8;
        }
    }

    private static class SimpleEGLConfigChooser32 extends ComponentSizeChooser {
        public SimpleEGLConfigChooser32(boolean withDepthBuffer, boolean stencil, boolean gles2) {
            super(8, 8, 8, 8, withDepthBuffer ? 16 : 0, stencil ? 8 : 0, gles2);
            mRedSize = 8;
            mGreenSize = 8;
            mBlueSize = 8;
            mAlphaSize = 8;
        }
    }
    private static ComponentSizeChooser getEglConfigChooser(int videoDepthBpp, boolean withDepthBuffer, boolean stencil, boolean gles2) {
        if(videoDepthBpp == 16)
            return new SimpleEGLConfigChooser16(withDepthBuffer, stencil, gles2);
        if(videoDepthBpp == 24)
            return new SimpleEGLConfigChooser24(withDepthBuffer, stencil, gles2);
        if(videoDepthBpp == 32)
            return new SimpleEGLConfigChooser32(withDepthBuffer, stencil, gles2);
        return null;
    };

    /**
     * An EGL helper class.
     */

    private class EglHelper {
        public EglHelper() {

        }

        /**
         * Initialize EGL for a given configuration spec.
         * @param configSpec
         */
        public void start(){

            Log.v("SDL", "GLSurfaceView_SDL::EglHelper::start(): creating GL context");
            /*
             * Get an EGL instance
             */
            mEgl = (EGL10) EGLContext.getEGL();

            /*
             * Get to the default display.
             */
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            /*
             * We can now initialize EGL for that display
             */
            int[] version = new int[2];
            mEgl.eglInitialize(mEglDisplay, version);
            mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
            if( mEglConfig == null )
                Log.e("SDL", "GLSurfaceView_SDL::EglHelper::start(): mEglConfig is NULL");

            /*
            * Create an OpenGL ES context. This must be done only once, an
            * OpenGL context is a somewhat heavy object.
            */
            final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            final int[] gles2_attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };

            mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig,
                    EGL10.EGL_NO_CONTEXT, mEGLConfigChooser.isGles2Required() ? gles2_attrib_list : null );

            if( mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT )
                Log.e("SDL", "GLSurfaceView_SDL::EglHelper::start(): mEglContext is EGL_NO_CONTEXT, error: " + mEgl.eglGetError());

            mEglSurface = null;
        }

        /*
         * React to the creation of a new surface by creating and returning an
         * OpenGL interface that renders to that surface.
         */
        public GL createSurface(SurfaceHolder holder) {
            Log.v("SDL", "GLSurfaceView_SDL::EglHelper::createSurface(): creating GL context");
            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            if (mEglSurface != null) {

                /*
                 * Unbind and destroy the old EGL surface, if
                 * there is one.
                 */
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            }

            /*
             * Create an EGL surface we can render into.
             */
            /*
            // This does not have any effect on Galaxy Note
            int [] attribList = new int[4];
            attribList[0] = mEgl.EGL_RENDER_BUFFER;
            attribList[1] = mEgl.EGL_SINGLE_BUFFER;
            attribList[2] = mEgl.EGL_NONE;
            attribList[3] = mEgl.EGL_NONE;
            */
            mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay,
                    mEglConfig, holder, null);

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
                    mEglContext);


            GL gl = mEglContext.getGL();
            if (mGLWrapper != null) {
                gl = mGLWrapper.wrap(gl);
            }

            return gl;
        }

        /**
         * Display the current render surface.
         * @return false if the context has been lost.
         */
        public boolean swap() {
            mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

            /*
             * Always check for EGL_CONTEXT_LOST, which means the context
             * and all associated data were lost (For instance because
             * the device went to sleep). We need to sleep until we
             * get a new surface.
             */
            return mEgl.eglGetError() != EGL11.EGL_CONTEXT_LOST;
        }

        public void finish() {
            Log.v("SDL", "GLSurfaceView_SDL::EglHelper::finish(): destroying GL context");
            if (mEglSurface != null) {
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
                mEglSurface = null;
            }
            if (mEglContext != null) {
                mEgl.eglDestroyContext(mEglDisplay, mEglContext);
                mEglContext = null;
            }
            if (mEglDisplay != null) {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     *
     */
    class GLThread extends Thread implements SwapBuffersCallback {
        GLThread(Renderer renderer) {
            super();
            mDone = false;
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RENDERMODE_CONTINUOUSLY;
            mRenderer = renderer;
            mRenderer.setSwapBuffersCallback(this);
            setName("GLThread");
        }

        @Override
        public void run() {
            /*
             * When the android framework launches a second instance of
             * an activity, the new instance's onCreate() method may be
             * called before the first instance returns from onDestroy().
             *
             * This semaphore ensures that only one instance at a time
             * accesses EGL.
             */
                try {
                sEglSemaphore.acquire();
                } catch (InterruptedException e) {
                    return;
                }

                mEglHelper = new EglHelper();
                // mEglHelper.start();
                mNeedStart = true;
                mSizeChanged = true;
                SwapBuffers();

                mRenderer.onDrawFrame(mGL);

                mEglHelper.finish();

                /*
                synchronized (sGLThreadManager) {
                    stopEglLocked();
                 }
                sGLThreadManager.threadExiting(this);
                */

                sEglSemaphore.release();
        }

        public boolean SwapBuffers() {

            boolean tellRendererSurfaceCreated = false;
            boolean tellRendererSurfaceChanged = false;

            /*
             * This is our main activity thread's loop, we go until
             * asked to quit.
             */

                /*
                 *  Update the asynchronous state (window size)
                 */
              while(true) { // Loop until we're re-created GL context and successfully called swap()

                int w, h;
                boolean changed = false;
                synchronized (this) {
                    /*
                    Runnable r;
                    while ((r = getEvent()) != null) {
                        r.run();
                    }
                    */
                    if (mPaused) {
                        mRenderer.onSurfaceDestroyed();
                        mEglHelper.finish();
                        mNeedStart = true;
                        if( Globals.NonBlockingSwapBuffers )
                            return false;
                    }
                }
                while (needToWait()) {
                    //Log.v("SDL", "GLSurfaceView_SDL::run(): paused");
                    try {
                        wait(500);
                    } catch(Exception e) { }
                }
                synchronized (this) {
                    if (mDone) {
                        return false;
                    }
                    // changed = mSizeChanged;
                    w = mWidth;
                    h = mHeight;
                    mSizeChanged = false;
                    mRequestRender = false;
                }
                if (mNeedStart) {
                    mEglHelper.start();
                    tellRendererSurfaceCreated = true;
                    changed = true;
                    mNeedStart = false;
                }
                if (changed) {
                    mGL = (GL10) mEglHelper.createSurface(getHolder());
                    tellRendererSurfaceChanged = true;
                }
                if (tellRendererSurfaceCreated) {
                    mRenderer.onSurfaceCreated(mGL, mEglHelper.mEglConfig);
                    tellRendererSurfaceCreated = false;
                }
                if (tellRendererSurfaceChanged) {
                    mRenderer.onSurfaceChanged(mGL, w, h);
                    tellRendererSurfaceChanged = false;
                }
                    /*
                     * Once we're done with GL, we need to call swapBuffers()
                     * to instruct the system to display the rendered frame
                     */
                if( mEglHelper.swap() )
                    return true;
                // We've lost GL context - recreate it
                mRenderer.onSurfaceDestroyed();
                mEglHelper.finish();
                mNeedStart = true;
                if( Globals.NonBlockingSwapBuffers )
                    return false;
              }
        }

        private boolean needToWait() {
            if (mKeyguardManager.inKeyguardRestrictedInputMode()) {
                return true; // We're in lockscreen - sleep until user unlocks the device
            }

            synchronized (this) {
                if (mDone) {
                    return false;
                }

                if ( Globals.HorizontalOrientation != (mWidth > mHeight) )
                    return true; // Wait until screen orientation changes

                if (mPaused || (! mHasSurface)) {
                    return true;
                }

                if ((mWidth > 0) && (mHeight > 0) && (mRequestRender || (mRenderMode == RENDERMODE_CONTINUOUSLY))) {
                    return false;
                }
            }

            return true;
        }

        public void setRenderMode(int renderMode) {
            if ( !((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY)) ) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized(this) {
                mRenderMode = renderMode;
                if (renderMode == RENDERMODE_CONTINUOUSLY) {
                    notify();
                }
            }
        }

        public int getRenderMode() {
            synchronized(this) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            synchronized(this) {
                mRequestRender = true;
                notify();
            }
        }

        public void surfaceCreated() {
            synchronized(this) {
                mHasSurface = true;
                notify();
            }
        }

        public void surfaceDestroyed() {
            synchronized(this) {
                mHasSurface = false;
                notify();
            }
        }

        public void onPause() {
            Log.v("SDL", "GLSurfaceView_SDL::onPause()");
            synchronized (this) {
                mPaused = true;
            }
        }

        public void onResume() {
            Log.v("SDL", "GLSurfaceView_SDL::onResume()");
            synchronized (this) {
                mPaused = false;
                notify();
            }
        }

        public void onWindowResize(int w, int h) {
            Log.v("SDL", "GLSurfaceView_SDL::onWindowResize(): " + w + "x" + h);
            synchronized (this) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                notify();
            }
        }

        public void requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized(this) {
                mDone = true;
                notify();
            }
            try {
                join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         * @param r the runnable to be run on the GL rendering thread.
         */
        public void queueEvent(Runnable r) {
            synchronized(this) {
                mEventQueue.add(r);
            }
        }

        private Runnable getEvent() {
            synchronized(this) {
                if (mEventQueue.size() > 0) {
                    return mEventQueue.remove(0);
                }

            }
            return null;
        }

        private boolean mDone;
        private boolean mPaused;
        private boolean mHasSurface;
        private int mWidth;
        private int mHeight;
        private int mRenderMode;
        private boolean mRequestRender;
        private Renderer mRenderer;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private EglHelper mEglHelper;
        private GL10 mGL = null;
        private boolean mNeedStart = false;
    }

    static class LogWriter extends Writer {

        @Override public void close() {
            flushBuilder();
        }

        @Override public void flush() {
            flushBuilder();
        }

        @Override public void write(char[] buf, int offset, int count) {
            for(int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if ( c == '\n') {
                    flushBuilder();
                }
                else {
                    mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (mBuilder.length() > 0) {
                Log.v("GLSurfaceView", mBuilder.toString());
                mBuilder.delete(0, mBuilder.length());
            }
        }

        private StringBuilder mBuilder = new StringBuilder();
    }

    private static final Semaphore sEglSemaphore = new Semaphore(1);
    private boolean mSizeChanged = true;

    private GLThread mGLThread;
    private EGLConfigChooser mEGLConfigChooser;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
    private KeyguardManager mKeyguardManager;
}
