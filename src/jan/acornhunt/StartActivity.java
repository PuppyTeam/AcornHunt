package jan.acornhunt;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.BoundCamera;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnAreaTouchListener;
import org.anddev.andengine.entity.scene.Scene.ITouchArea;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.bitmap.BitmapTexture;
import org.anddev.andengine.opengl.texture.bitmap.BitmapTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import android.content.Intent;
import android.widget.Toast;


public class StartActivity extends BaseGameActivity {

    private final String LOG_TAG = "MainActivity";
    private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 320;

	private Camera camera;

	private BitmapTexture bgTexture1, bgTexture2, 
						titleTexture, playTexture;
	private TextureRegion bgTextureRegion1, bgTextureRegion2;

	protected int mCactusCount;

	@Override
	public Engine onLoadEngine() {
		this.camera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions
				(true, ScreenOrientation.LANDSCAPE, 
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), 
				this.camera));
	}

	@Override
	public void onLoadResources() {

		BitmapTextureRegionFactory.setAssetBasePath("gfx/start/");

		this.bgTexture1 = new BitmapTexture(512, 1024,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.bgTexture2 = new BitmapTexture(512, 1024,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);

		this.bgTextureRegion1 = BitmapTextureRegionFactory.
				createFromAsset(this.bgTexture1, this, 
						"start_1.png", 0, 0);
		this.bgTextureRegion2 = BitmapTextureRegionFactory.
				createFromAsset(this.bgTexture2, this, 
						"start_2.png", 0, 0);

		this.mEngine.getTextureManager().loadTexture(this.bgTexture1);
		this.mEngine.getTextureManager().loadTexture(this.bgTexture2);			
	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		
		/* Calculate the coordinates for the face, so its centered on the camera. */
		final int centerX = (CAMERA_WIDTH - 
				this.bgTextureRegion1.getWidth()) / 2;
		final int centerY = (CAMERA_HEIGHT - 
				this.bgTextureRegion1.getHeight()) / 2;

		final Sprite start1 = new Sprite(centerX, centerY,
				this.bgTextureRegion1);
		scene.attachChild(start1);
	
		final Sprite start2 = new Sprite(centerX, centerY,
				this.bgTextureRegion2);
		scene.attachChild(start2);
		scene.setOnAreaTouchListener(new IOnAreaTouchListener(){
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					ITouchArea pTouchArea, float pTouchAreaLocalX,
					float pTouchAreaLocalY) {
				Toast.makeText(getApplicationContext(), pTouchAreaLocalX
						+ ":" + pTouchAreaLocalY, Toast.LENGTH_SHORT).show();
				return false;
			}
		});
		
		return scene;
	}

	@Override
	public void onLoadComplete() {
		Intent intent = new Intent(this, PlayingActivity.class);
		startActivity(intent);
		this.finish();
	}
}