package com.aleaf.android.glwallpaperservice;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

class DefaultContextFactory implements GLSurfaceView.EGLContextFactory {
	private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	private int mEGLContextClientVersion;
	public DefaultContextFactory(int eGLContextClientVersion){
		mEGLContextClientVersion = eGLContextClientVersion;
	}

	public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
		int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
				EGL10.EGL_NONE };

		return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
				mEGLContextClientVersion != 0 ? attrib_list : null);
	}

	public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
		egl.eglDestroyContext(display, context);
	}
}