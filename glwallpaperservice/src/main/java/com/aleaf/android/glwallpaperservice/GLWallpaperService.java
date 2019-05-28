package com.aleaf.android.glwallpaperservice;

import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class GLWallpaperService extends WallpaperService {
	private static final String TAG = "GLWallpaperService";

	@Override
	public Engine onCreateEngine() {
		return new GLEngine();
	}

	public class GLEngine extends Engine {
		public final static int RENDERMODE_WHEN_DIRTY = 0;
		public final static int RENDERMODE_CONTINUOUSLY = 1;

		private GLThread mGLThread;
		private GLSurfaceView.EGLConfigChooser mEGLConfigChooser;
		private GLSurfaceView.EGLContextFactory mEGLContextFactory;
		private GLSurfaceView.EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
		private GLSurfaceView.GLWrapper mGLWrapper;
		private int mDebugFlags;
		private int mEGLContextClientVersion;

		public GLEngine() {
			super();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				onResume();
			} else {
				onPause();
			}
			super.onVisibilityChanged(visible);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			// Log.d(TAG, "GLEngine.onCreate()");
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			// Log.d(TAG, "GLEngine.onDestroy()");
			mGLThread.requestExitAndWait();
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			// Log.d(TAG, "onSurfaceChanged()");
			mGLThread.onWindowResize(width, height);
			super.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			//Log.d(TAG, "onSurfaceCreated()");
			mGLThread.surfaceCreated(holder);
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			//Log.d(TAG, "onSurfaceDestroyed()");
			mGLThread.surfaceDestroyed();
			super.onSurfaceDestroyed(holder);
		}

		/**
		 * An EGL helper class.
		 */
		public void setGLWrapper(GLSurfaceView.GLWrapper glWrapper) {
			mGLWrapper = glWrapper;
		}

		public void setDebugFlags(int debugFlags) {
			mDebugFlags = debugFlags;
		}

		public int getDebugFlags() {
			return mDebugFlags;
		}

		public void setRenderer(GLSurfaceView.Renderer renderer) {
			checkRenderThreadState();
			if (mEGLConfigChooser == null) {
				mEGLConfigChooser = new SimpleEGLConfigChooser(true);
			}
			if (mEGLContextFactory == null) {
				mEGLContextFactory = new DefaultContextFactory(mEGLContextClientVersion);
			}
			if (mEGLWindowSurfaceFactory == null) {
				mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
			}
			mGLThread = new GLThread(renderer, mEGLConfigChooser, mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
			mGLThread.start();
		}

        public void setEGLContextClientVersion(int version) {
            checkRenderThreadState();
            mEGLContextClientVersion = version;
        }

		public void setEGLContextFactory(GLSurfaceView.EGLContextFactory factory) {
			checkRenderThreadState();
			mEGLContextFactory = factory;
		}

		public void setEGLWindowSurfaceFactory(GLSurfaceView.EGLWindowSurfaceFactory factory) {
			checkRenderThreadState();
			mEGLWindowSurfaceFactory = factory;
		}

		public void setEGLConfigChooser(GLSurfaceView.EGLConfigChooser configChooser) {
			checkRenderThreadState();
			mEGLConfigChooser = configChooser;
		}

		public void setEGLConfigChooser(boolean needDepth) {
			setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
		}

		public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize,
				int stencilSize) {
			setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize,
					stencilSize));
		}

		public void setRenderMode(int renderMode) {
			mGLThread.setRenderMode(renderMode);
		}

		public int getRenderMode() {
			return mGLThread.getRenderMode();
		}

		public void requestRender() {
			mGLThread.requestRender();
		}

		public void onPause() {
			mGLThread.onPause();
		}

		public void onResume() {
			mGLThread.onResume();
		}

		public void queueEvent(Runnable r) {
			mGLThread.queueEvent(r);
		}

		private void checkRenderThreadState() {
			if (mGLThread != null) {
				throw new IllegalStateException("setRenderer has already been called for this instance.");
			}
		}

        abstract class BaseConfigChooser implements GLSurfaceView.EGLConfigChooser {

            public BaseConfigChooser(int[] configSpec) {
                mConfigSpec = filterConfigSpec(configSpec);
            }

            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                int[] num_config = new int[1];
                egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config);

                int numConfigs = num_config[0];

                if (numConfigs <= 0) {
                    throw new IllegalArgumentException("No configs match configSpec");
                }

                EGLConfig[] configs = new EGLConfig[numConfigs];
                egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, num_config);
                EGLConfig config = chooseConfig(egl, display, configs);
                if (config == null) {
                    throw new IllegalArgumentException("No config chosen");
                }
                return config;
            }

            private int[] filterConfigSpec(int[] configSpec) {
                if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                    return configSpec;
                }
                /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
                 * And we know the configSpec is well formed.
                 */
                int len = configSpec.length;
                int[] newConfigSpec = new int[len + 2];
                System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
                newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
                if (mEGLContextClientVersion == 2) {
                    newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
                } else {
                    newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
                }
                newConfigSpec[len+1] = EGL10.EGL_NONE;
                return newConfigSpec;
            }

            abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs);

            protected int[] mConfigSpec;

        }

        public class ComponentSizeChooser extends BaseConfigChooser {
            public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize,
                                        int stencilSize) {
                super(new int[] { EGL10.EGL_RED_SIZE, redSize, EGL10.EGL_GREEN_SIZE, greenSize, EGL10.EGL_BLUE_SIZE,
                        blueSize, EGL10.EGL_ALPHA_SIZE, alphaSize, EGL10.EGL_DEPTH_SIZE, depthSize, EGL10.EGL_STENCIL_SIZE,
                        stencilSize, EGL10.EGL_NONE });
                mValue = new int[1];
                mRedSize = redSize;
                mGreenSize = greenSize;
                mBlueSize = blueSize;
                mAlphaSize = alphaSize;
                mDepthSize = depthSize;
                mStencilSize = stencilSize;
            }

            @Override
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
                EGLConfig closestConfig = null;
                int closestDistance = 1000;
                for (EGLConfig config : configs) {
                    int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                    int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                    if (d >= mDepthSize && s >= mStencilSize) {
                        int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                        int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                        int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                        int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                        int distance = Math.abs(r - mRedSize) + Math.abs(g - mGreenSize) + Math.abs(b - mBlueSize)
                                + Math.abs(a - mAlphaSize);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestConfig = config;
                        }
                    }
                }
                return closestConfig;
            }

            private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {

                if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                    return mValue[0];
                }
                return defaultValue;
            }

            private int[] mValue;
            // Subclasses can adjust these values:
            protected int mRedSize;
            protected int mGreenSize;
            protected int mBlueSize;
            protected int mAlphaSize;
            protected int mDepthSize;
            protected int mStencilSize;
        }

        /**
         * This class will choose a supported surface as close to RGB565 as possible, with or without a depth buffer.
         *
         */
        public class SimpleEGLConfigChooser extends ComponentSizeChooser {
            public SimpleEGLConfigChooser(boolean withDepthBuffer) {
                super(4, 4, 4, 0, withDepthBuffer ? 16 : 0, 0);
                // Adjust target values. This way we'll accept a 4444 or
                // 555 buffer if there's no 565 buffer available.
                mRedSize = 5;
                mGreenSize = 6;
                mBlueSize = 5;
            }
        }
	}

	/**
	 * Empty wrapper for {@link GLSurfaceView.Renderer}.
	 *
	 * @deprecated Use {@link GLSurfaceView.Renderer} instead.
	 */
	@Deprecated
	public interface Renderer extends GLSurfaceView.Renderer {
	}


}

