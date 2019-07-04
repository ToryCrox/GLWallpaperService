
动态壁纸的OpenGL实现，结合GLSurcfaceView支持OpenGL2.0

### 引用库

```
implementation "com.github.ToryCrox:GLWallpaperService:1.0"
```

### 使用

```
public class Live2dWallpaperService extends GLWallpaperService {
    private final String TAG = "Live2dWallpaperService#"+hashCode();


    @Override
    public Engine onCreateEngine() {
        return new MyEngine();
    }

    private class MyEngine extends GLEngine{

        Live2dDemoManager mLive2dManager;
        Live2d3ViewDelegate mDelegate;

        private MyEngine(){
            super();
            Log.d(TAG, "MyEngine");
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);
            //设置透明
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            Context context = getApplicationContext();
            mLive2dManager = new Live2dDemoManager(context);
            mDelegate = mLive2dManager.getViewDelegate();
            setRenderer(mDelegate.getRender());
            mLive2dManager.setBackgroundImage(ContextCompat.getDrawable(context, R.drawable.bg_pic_test2));
            mLive2dManager.loadModel("RURI_NEW/琉璃6_新增表情.model3.json");
            Log.d(TAG, "onCreate");
        }
       ....
}


```


