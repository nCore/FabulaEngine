package com.macbury.fabula.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.lights.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.lights.Lights;
import com.badlogic.gdx.graphics.g3d.materials.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.macbury.fabula.editor.WorldEditorFrame;
import com.macbury.fabula.editor.brushes.Brush;
import com.macbury.fabula.editor.brushes.TerrainBrush;
import com.macbury.fabula.manager.GameManager;
import com.macbury.fabula.manager.ResourceManager;
import com.macbury.fabula.terrain.Terrain;
import com.macbury.fabula.terrain.Terrain.TerrainDebugListener;
import com.macbury.fabula.terrain.Tile;
import com.macbury.fabula.utils.ActionTimer;
import com.macbury.fabula.utils.ActionTimer.TimerListener;
import com.macbury.fabula.utils.EditorCamController;
import com.macbury.fabula.utils.TopDownCamera;

public class WorldEditScreen extends BaseScreen implements InputProcessor, TimerListener, TerrainDebugListener {
  public String debugInfo = "";
  private static final String TAG = "WorldScreen";
  private static final float APPLY_BRUSH_EVERY = 0.01f;
  private TopDownCamera camera;
  private Terrain terrain;
  private ModelBatch modelBatch;
  private EditorCamController camController;
  private ActionTimer brushTimer;
  private Brush        currentBrush;
  private TerrainBrush terrainBrush;
  
  public WorldEditScreen(GameManager manager) {
    super(manager);
    
    try {
      ResourceManager.shared().loadSynch();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    this.brushTimer = new ActionTimer(APPLY_BRUSH_EVERY, this);
    
    ModelBuilder modelBuilder = new ModelBuilder();
    Model model               = modelBuilder.createBox(1f, 0.1f, 1f,  new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal);
    
    this.camera = new TopDownCamera();
    Gdx.app.log(TAG, "Initialized screen");
    this.terrain = new Terrain(this, 50, 50, true);
    terrain.setDebugListener(this);
    terrain.fillEmptyTilesWithDebugTile();
    terrain.buildSectors();
    camera.position.set(0, 17, 0);
    camera.lookAt(0, 0, 0);
    
    terrainBrush = new TerrainBrush(terrain);
    currentBrush = terrainBrush;
    
    this.camController = new EditorCamController(camera);
    InputMultiplexer inputMultiplexer = new InputMultiplexer(this, camController);
    Gdx.input.setInputProcessor(inputMultiplexer);
  }

  @Override
  public void dispose() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void hide() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void pause() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void render(float delta) {
    this.brushTimer.update(delta);
    camController.update();
    camera.update();
    
    this.terrain.render(this.camera);
    //modelBatch.begin(camera);
    //modelBatch.render(cursorInstance);
    //modelBatch.end();
    
    debugInfo = "FPS: "+ Gdx.graphics.getFramesPerSecond() + " Java Heap: " + (Gdx.app.getJavaHeap() / 1024) + " KB" + " Native Heap: " + (Gdx.app.getNativeHeap() / 1024);
    
    handlePick();
  }
  
  private void handlePick() {
    if (Gdx.input.isKeyPressed(Keys.F)) {
      Vector3 pos = new Vector3();
      //cursorInstance.transform.getTranslation(pos);
      camera.lookAt(pos);
    } 
    
    if (Gdx.input.isKeyPressed(Keys.Q)) {
      Vector3 pos = new Vector3();
      //cursorInstance.transform.getTranslation(pos);
      //terrain.applyHill(pos, 0.1f);
    } else if (Gdx.input.isKeyPressed(Keys.A))  {
      Vector3 pos = new Vector3();
      //cursorInstance.transform.getTranslation(pos);
      //terrain.applyHill(pos, -0.1f);
    }
  }

  @Override
  public void resize(int width, int height) {
    
    camera.viewportWidth = Gdx.graphics.getWidth();
    camera.viewportHeight = Gdx.graphics.getHeight();
    this.camera.update(true);
  }
  
  @Override
  public void resume() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void show() {
    Gdx.app.log(TAG, "Showed screen");
  }
  
  public TopDownCamera getCamera() {
    return camera;
  }

  @Override
  public boolean keyDown(int arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean keyTyped(char arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean keyUp(int arg0) {
    // TODO Auto-generated method stub
    return false;
  }
  
  Vector3 mouseTilePosition = new Vector3();
  @Override
  public boolean mouseMoved(int x, int y) {
    Ray ray     = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
    Vector3 pos = terrain.getSnappedPositionForRay(ray, mouseTilePosition);
    
    if (pos != null) {
      currentBrush.setPosition(pos.x, pos.z);
    }
    return true;
  }

  @Override
  public boolean scrolled(int arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean touchDown(int x, int y, int pointer, int button) {
    if (button == Buttons.LEFT) {
      this.brushTimer.start();
      return true;
    }
    return false;
  }

  @Override
  public boolean touchDragged(int arg0, int arg1, int arg2) {
    //Gdx.app.log(TAG, "Dragging");
    return false;
  }

  @Override
  public boolean touchUp(int x, int y, int pointer, int button) {
    if (button == Buttons.LEFT) {
      this.brushTimer.stop();
      return true;
    }
    return false;
  }

  @Override
  public void onTimerTick(ActionTimer timer) {
    currentBrush.onApply();
  }

  @Override
  public void onDebugTerrainConfigureShader(ShaderProgram shader) {
    shader.setUniformf("u_brush_position", currentBrush.getPosition());
    shader.setUniformf("u_brush_size", currentBrush.getSize());
    shader.setUniformf("u_wireframe", 0.0f);
  }

}
