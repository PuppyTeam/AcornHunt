package jan.acornhunt;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.BoundCamera;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.anddev.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.handler.physics.PhysicsHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXLayer;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXLoader;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXProperties;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXTile;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXTileProperty;
import org.anddev.andengine.entity.layer.tiled.tmx.TMXTiledMap;
import org.anddev.andengine.entity.layer.tiled.tmx.util.exception.TMXLoadException;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.bitmap.BitmapTexture;
import org.anddev.andengine.opengl.texture.bitmap.BitmapTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.constants.Constants;

import android.util.Log;
import android.widget.Toast;

public class PlayingActivity extends BaseGameActivity {
	
	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 320;
	
	private BoundCamera mBoundChaseCamera;

	private BitmapTexture mBitmapTexture;
	private TiledTextureRegion mPlayerTextureRegion;
	
	private BitmapTexture mOnScreenControlBitmapTexture;
	private TextureRegion mOnScreenControlBaseTextureRegion;
	private TextureRegion mOnScreenControlKnobTextureRegion;
	private boolean mPlaceOnScreenControlsAtDifferentVerticalLocations = false;
	
	private TMXTiledMap mTMXTiledMap;
	protected int mCactusCount;

	private enum ControlDirection {LEFT, UP, RIGHT, DOWN, NULL};
	private ControlDirection leftControl = ControlDirection.NULL, 
			rightControl = ControlDirection.NULL;
	private boolean isLeftMoved = true, isRightMoved = false;
	private final double MID_VALUE = 0.785;
	private final int SPRITE_VELOCITY = 50;
	@Override
	public Engine onLoadEngine() {
		Toast.makeText(this, "The tile the player is walking on will be highlighted.", Toast.LENGTH_LONG).show();
		this.mBoundChaseCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		
		final Engine engine = new Engine(
				new EngineOptions(true, 
						ScreenOrientation.LANDSCAPE, 
				new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT)
				, this.mBoundChaseCamera));

		try {
			if(MultiTouch.isSupported(this)) {
				engine.setTouchController(new MultiTouchController());
				if(MultiTouch.isSupportedDistinct(this)) {
					Toast.makeText(this, "MultiTouch detected --> Both controls will work properly!", Toast.LENGTH_LONG).show();
				} else {
					this.mPlaceOnScreenControlsAtDifferentVerticalLocations = true;
					Toast.makeText(this, "MultiTouch detected, but your device has problems distinguishing between fingers.\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
			}
		} catch (final MultiTouchException e) {
			Toast.makeText(this, "Sorry your Android Version does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
		}

		return engine;
	}

	@Override
	public void onLoadResources() {
		BitmapTextureRegionFactory.setAssetBasePath("gfx/");

		this.mBitmapTexture = new BitmapTexture(128, 128, TextureOptions.DEFAULT);

		this.mPlayerTextureRegion = BitmapTextureRegionFactory.createTiledFromAsset(this.mBitmapTexture, this, "player.png", 0, 0, 3, 4);

		this.mOnScreenControlBitmapTexture = new BitmapTexture(256, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureRegionFactory.createFromAsset(this.mOnScreenControlBitmapTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureRegionFactory.createFromAsset(this.mOnScreenControlBitmapTexture, this, "onscreen_control_knob.png", 128, 0);
		
		this.mEngine.getTextureManager().loadTexture(mOnScreenControlBitmapTexture);
		this.mEngine.getTextureManager().loadTexture(this.mBitmapTexture);
	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();

		try {
			final TMXLoader tmxLoader = new TMXLoader(this, this.mEngine.getTextureManager(), TextureOptions.BILINEAR_PREMULTIPLYALPHA, new ITMXTilePropertiesListener() {
				@Override
				public void onTMXTileWithPropertiesCreated(final TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer, final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
					/* We are going to count the tiles that have the property "cactus=true" set. */
					if(pTMXTileProperties.containsTMXProperty("cactus", "true")) {
						PlayingActivity.this.mCactusCount++;
					}
				}
			});
			this.mTMXTiledMap = tmxLoader.loadFromAsset(this, "tmx/desert.tmx");

			Toast.makeText(this, "Cactus count in this TMXTiledMap: " + this.mCactusCount, Toast.LENGTH_LONG).show();
		} catch (final TMXLoadException tmxle) {
			Debug.e(tmxle);
		}
		
		/*for(int i=0; i<this.mTMXTiledMap.getTMXObjectGroups().size(); i++)
			Log.d("TMX", 
					this.mTMXTiledMap.getTMXObjectGroups().get(i)
					.getTMXObjects().get(0).getTMXObjectProperties().toString());*/
		final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(0);
		scene.attachChild(tmxLayer);

		/* Make the camera not exceed the bounds of the TMXEntity. */
		this.mBoundChaseCamera.setBounds(0, tmxLayer.getWidth(), 0, tmxLayer.getHeight());
		this.mBoundChaseCamera.setBoundsEnabled(true);

		/* Calculate the coordinates for the face, so its centered on the camera. */
		final int centerX = (CAMERA_WIDTH - this.mPlayerTextureRegion.getTileWidth()) / 2;
		final int centerY = (CAMERA_HEIGHT - this.mPlayerTextureRegion.getTileHeight()) / 2;

		/* Create the sprite and add it to the scene. */
		final AnimatedSprite player = new AnimatedSprite(centerX, centerY, this.mPlayerTextureRegion);
		this.mBoundChaseCamera.setChaseEntity(player);

		final PhysicsHandler physicsHandler = new PhysicsHandler(player);
		player.registerUpdateHandler(physicsHandler);
		/* Now we are going to create a rectangle that will  always highlight the tile below the feet of the pEntity. */
		final Rectangle currentTileRectangle = new Rectangle(0, 0, this.mTMXTiledMap.getTileWidth(), this.mTMXTiledMap.getTileHeight());
		currentTileRectangle.setColor(1, 0, 0, 0.25f);
		scene.attachChild(currentTileRectangle);

		scene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				/* Get the scene-coordinates of the players feet. */
				final float[] playerFootCordinates = 
						player.convertLocalToSceneCoordinates(12, 31);

				/* Get the tile the feet of the player are currently waking on. */
				final TMXTile tmxTile = 
						tmxLayer.getTMXTileAt(playerFootCordinates[Constants.VERTEX_INDEX_X], playerFootCordinates[Constants.VERTEX_INDEX_Y]);
				if(tmxTile != null)
				Log.d("position", tmxTile.getTileColumn() + ":" + tmxTile.getTileRow());
				TMXTile tile = null; 
				if(tmxTile != null){
					switch(leftControl){
					case LEFT:
						tile = tmxLayer.getTMXTile(tmxTile.getTileColumn() - 1, 
								tmxTile.getTileRow());
						break;
					case RIGHT:
						tile = tmxLayer.getTMXTile(tmxTile.getTileColumn() + 1, 
								tmxTile.getTileRow());
						break;
					case UP:
						tile = tmxLayer.getTMXTile(tmxTile.getTileColumn(), 
								tmxTile.getTileRow() - 1);
						break;
					case DOWN:
						tile = tmxLayer.getTMXTile(tmxTile.getTileColumn(), 
								tmxTile.getTileRow() + 1);
						break;
					case NULL:
						break;
					}
				}
				if(tile != null && tile.getTMXTileProperties(mTMXTiledMap) != null &&
						tile.getTMXTileProperties(mTMXTiledMap)
						.get(0).getValue().equals("false")){
					isLeftMoved = false;
				}else{
					isLeftMoved = true;
				}

				if(tmxTile != null) {
					
					// tmxTile.setTextureRegion(null); <-- Rubber-style removing of tiles =D
					currentTileRectangle.setPosition(tmxTile.getTileX(), tmxTile.getTileY());
				}
			}
		});
		scene.attachChild(player);

		/* Velocity control (left). */
		final int x1 = 0;
		final int y1 = CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight();
		final AnalogOnScreenControl velocityOnScreenControl = 
				new AnalogOnScreenControl
					(x1, y1, this.mBoundChaseCamera, 
						this.mOnScreenControlBaseTextureRegion, 
						this.mOnScreenControlKnobTextureRegion, 
						0.1f, new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl,
					final float pValueX, final float pValueY) {

				Log.d("TMX", isLeftMoved + "");
				Log.d("Playing", leftControl + ":" + rightControl);
				if(pValueX == 0 && pValueY == 0){
					leftControl = ControlDirection.NULL;
				}else{
					double result = Math.atan(pValueY/pValueX);
					if(pValueY > 0){
						if(Math.abs(result) > MID_VALUE)
							leftControl = ControlDirection.DOWN;
						else{
							if(pValueX > 0)
								leftControl = ControlDirection.RIGHT;
							else
								leftControl = ControlDirection.LEFT;
						}
					}else{
						if(Math.abs(result) > MID_VALUE)
							leftControl = ControlDirection.UP;
						else{
							if(pValueX > 0)
								leftControl = ControlDirection.RIGHT;
							else
								leftControl = ControlDirection.LEFT;
						}
					}
				}
				
				if(!isLeftMoved){
					physicsHandler.setVelocity(0, 0);
				}else{
					switch(leftControl){
					case LEFT:
						physicsHandler.setVelocity(-SPRITE_VELOCITY, 0);
						break;
					case RIGHT:
						physicsHandler.setVelocity(SPRITE_VELOCITY, 0);
						break;
					case UP:
						physicsHandler.setVelocity(0, -SPRITE_VELOCITY);
						break;
					case DOWN:
						physicsHandler.setVelocity(0, SPRITE_VELOCITY);
						break;
					case NULL:
						physicsHandler.setVelocity(0, 0);
					}
				}
				switch(leftControl) {
				case DOWN:
					player.animate(new long[]{200, 200, 200}, 6, 8, true);
					break;
				case RIGHT:
					player.animate(new long[]{200, 200, 200}, 3, 5, true);
					break;
				case UP:
					player.animate(new long[]{200, 200, 200}, 0, 2, true);
					break;
				case LEFT:
					player.animate(new long[]{200, 200, 200}, 9, 11, true);
					break;
					default:
			}
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
				/* Nothing. */
			}
		});
		velocityOnScreenControl.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		velocityOnScreenControl.getControlBase().setAlpha(0.5f);

		scene.setChildScene(velocityOnScreenControl);


		/* Rotation control (right). */
		final int y2 = (this.mPlaceOnScreenControlsAtDifferentVerticalLocations) ? 0 : y1;
		final int x2 = CAMERA_WIDTH - this.mOnScreenControlBaseTextureRegion.getWidth();
		final AnalogOnScreenControl rotationOnScreenControl = 
				new AnalogOnScreenControl(x2, y2, 
						this.mBoundChaseCamera, 
						this.mOnScreenControlBaseTextureRegion, 
						this.mOnScreenControlKnobTextureRegion, 
						0.1f, new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(
					final BaseOnScreenControl pBaseOnScreenControl, 
					final float pValueX, final float pValueY) {
				if(isRightMoved){
					physicsHandler.setVelocity(pValueX * 100, pValueY * 100);
				}
				if(pValueX == 0 && pValueY == 0){
					rightControl = ControlDirection.NULL;
				}else{
					double result = Math.atan(pValueY/pValueX);
					if(pValueY > 0){
						if(Math.abs(result) > MID_VALUE){
							rightControl = ControlDirection.DOWN;
						}else{
							if(pValueX > 0)
								rightControl = ControlDirection.RIGHT;
							else
								rightControl = ControlDirection.LEFT;
						}
					}else{
						if(Math.abs(result) > MID_VALUE){
							rightControl = ControlDirection.UP;
						}else{
							if(pValueX > 0)
								rightControl = ControlDirection.RIGHT;
							else
								rightControl = ControlDirection.LEFT;
						}
					}
				}
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
				/* Nothing. */
			}
		});
		rotationOnScreenControl.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		rotationOnScreenControl.getControlBase().setAlpha(0.5f);

		velocityOnScreenControl.setChildScene(rotationOnScreenControl);
		
		return scene;
	}

	@Override
	public void onLoadComplete() {

	}
}

