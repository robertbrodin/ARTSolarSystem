/*
 * Copyright 2018 Google LLC.
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
package com.google.ar.sceneform.samples.solarsystem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.VirtualDisplay;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;

import android.hardware.display.DisplayManager;


import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore and Sceneform APIs.
 */
public class SolarActivity extends AppCompatActivity {
    private static final int RC_PERMISSIONS = 0x123;
    private boolean installRequested;

    private GestureDetector gestureDetector;
    private Snackbar loadingMessageSnackbar = null;

    private ArSceneView arSceneView;

    private ModelRenderable sunRenderable;
    private ModelRenderable mercuryRenderable;
    private ModelRenderable venusRenderable;
    private ModelRenderable earthRenderable;
    private ModelRenderable lunaRenderable;
    private ModelRenderable marsRenderable;
    private ModelRenderable jupiterRenderable;
    private ModelRenderable saturnRenderable;
    private ModelRenderable uranusRenderable;
    private ModelRenderable neptuneRenderable;
    private ViewRenderable solarControlsRenderable;

    private final SolarSettings solarSettings = new SolarSettings();

    // True once scene is loaded
    private boolean hasFinishedLoading = false;

    // True once the scene has been placed.
    private boolean hasPlacedSolarSystem = false;

    // Astronomical units to meters ratio. Used for positioning the planets of the solar system.
    private static final float AU_TO_METERS = 0.5f;

    private ArrayList<Planet> planetList = new ArrayList<Planet>();

    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static int DISPLAY_WIDTH = 480;
    private static int DISPLAY_HEIGHT = 640;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection.Callback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;
    private Surface getSurface;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!DemoUtils.checkIsSupportedDeviceOrFinish(this)) {
            // Not a supported device.
            return;
        }

        setContentView(R.layout.activity_solar);
        arSceneView = findViewById(R.id.ar_scene_view);


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        DISPLAY_HEIGHT = metrics.heightPixels;
        DISPLAY_WIDTH = metrics.widthPixels;

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mToggleButton.setText("Click to record!");
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onToggleScreenShare(v);

            }
        });

        mMediaProjectionCallback = new MediaProjectionCallback();


        // Build all the planet models.
        CompletableFuture<ModelRenderable> sunStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Sol.sfb")).build();
        CompletableFuture<ModelRenderable> mercuryStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Mercury.sfb")).build();
        CompletableFuture<ModelRenderable> venusStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Venus.sfb")).build();
        CompletableFuture<ModelRenderable> earthStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Earth.sfb")).build();
        CompletableFuture<ModelRenderable> lunaStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Luna.sfb")).build();
        CompletableFuture<ModelRenderable> marsStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Mars.sfb")).build();
        CompletableFuture<ModelRenderable> jupiterStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Jupiter.sfb")).build();
        CompletableFuture<ModelRenderable> saturnStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Saturn.sfb")).build();
        CompletableFuture<ModelRenderable> uranusStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Uranus.sfb")).build();
        CompletableFuture<ModelRenderable> neptuneStage =
                ModelRenderable.builder().setSource(this, Uri.parse("Neptune.sfb")).build();

        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> solarControlsStage =
                ViewRenderable.builder().setView(this, R.layout.solar_controls).build();

        CompletableFuture.allOf(
                sunStage,
                mercuryStage,
                venusStage,
                earthStage,
                lunaStage,
                marsStage,
                jupiterStage,
                saturnStage,
                uranusStage,
                neptuneStage,
                solarControlsStage)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderable", throwable);
                                return null;
                            }

                            try {
                                sunRenderable = sunStage.get();
                                mercuryRenderable = mercuryStage.get();
                                venusRenderable = venusStage.get();
                                earthRenderable = earthStage.get();
                                lunaRenderable = lunaStage.get();
                                marsRenderable = marsStage.get();
                                jupiterRenderable = jupiterStage.get();
                                saturnRenderable = saturnStage.get();
                                uranusRenderable = uranusStage.get();
                                neptuneRenderable = neptuneStage.get();
                                solarControlsRenderable = solarControlsStage.get();

                                // Everything finished loading successfully.
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderable", ex);
                            }

                            return null;
                        });

        // Set up a tap gesture detector.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                onSingleTap(e);
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });

        // Set a touch listener on the Scene to listen for taps.
        arSceneView
                .getScene()
                .setOnTouchListener(
                        (HitTestResult hitTestResult, MotionEvent event) -> {
                            // If the solar system hasn't been placed yet, detect a tap and then check to see if
                            // the tap occurred on an ARCore plane to place the solar system.
                            if (!hasPlacedSolarSystem) {
                                return gestureDetector.onTouchEvent(event);
                            }

                            // Otherwise return false so that the touch event can propagate to the scene.
                            return false;
                        });

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            if (loadingMessageSnackbar == null) {
                                return;
                            }

                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }

                            for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                                if (plane.getTrackingState() == TrackingState.TRACKING) {
                                    hideLoadingMessage();
                                }
                            }
                        });

        // Lastly request CAMERA permission which is required by ARCore.
        DemoUtils.requestCameraPermission(this, RC_PERMISSIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arSceneView == null) {
            return;
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = DemoUtils.hasCameraPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaRecorder.reset();

            mMediaProjection = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!DemoUtils.hasCameraPermission(this)) {
            if (!DemoUtils.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                DemoUtils.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent tap) {
        if (!hasFinishedLoading) {
            // We can't do anything yet.
            return;
        }

        Frame frame = arSceneView.getArFrame();
        if (frame != null) {
            if (!hasPlacedSolarSystem && tryPlaceSolarSystem(tap, frame)) {
                hasPlacedSolarSystem = true;
            }
        }
    }

    private boolean tryPlaceSolarSystem(MotionEvent tap, Frame frame) {
        if (tap != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    // Create the Anchor.
                    Anchor anchor = hit.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arSceneView.getScene());
                    Node solarSystem = createSolarSystem();
                    anchorNode.addChild(solarSystem);
                    return true;
                }
            }
        }

        return false;
    }

    private Node createSolarSystem() {
        Node base = new Node();

        Node sun = new Node();
        sun.setParent(base);
        sun.setLocalPosition(new Vector3(0.0f, 0.5f, 0.0f));

        Node sunVisual = new Node();
        sunVisual.setParent(sun);
        sunVisual.setRenderable(sunRenderable);
        sunVisual.setLocalScale(new Vector3(1f, 1f, 1f));
        float sunScale = 0.5f;

        Node solarControls = new Node();
        solarControls.setParent(sun);
        solarControls.setRenderable(solarControlsRenderable);
        solarControls.setLocalPosition(new Vector3(0.0f, 0.25f, 0.0f));

        View solarControlsView = solarControlsRenderable.getView();
        SeekBar orbitSpeedBar = solarControlsView.findViewById(R.id.orbitSpeedBar);
        orbitSpeedBar.setProgress((int) (solarSettings.getOrbitSpeedMultiplier() * 10.0f));
        orbitSpeedBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float ratio = (float) progress / (float) orbitSpeedBar.getMax();
                        solarSettings.setOrbitSpeedMultiplier(ratio * 10.0f);

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        SeekBar rotationSpeedBar = solarControlsView.findViewById(R.id.rotationSpeedBar);
        rotationSpeedBar.setProgress((int) (solarSettings.getRotationSpeedMultiplier() * 10.0f));
        rotationSpeedBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float ratio = (float) progress / (float) rotationSpeedBar.getMax();
                        solarSettings.setRotationSpeedMultiplier(ratio * 10.0f);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        SeekBar planetScaleBar = solarControlsView.findViewById(R.id.scaleBar);
        planetScaleBar.setProgress((int) (solarSettings.getScaleMultiplier() * 1.0f));
        planetScaleBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //float ratio = (((float) progress) / (float) planetScaleBar.getMax());
                        //float ratio = (float) progress / (float) planetScaleBar.getMax();
                        float ratio = progress;
                        float ratioDistance = progress / 10.0f;
                        //ratio = ratio * 2.0f;

                        //solarSettings.setScaleMultiplier(ratio * 10.0f);

                        solarControls.setLocalPosition(new Vector3(0.0f, 0.5f * ratio, 0.0f));

                        // Also need to scale the sun before because it is not in planetList.
                        //sunVisual.setLocalScale(new Vector3(ratio * sunScale, ratio * sunScale, ratio * sunScale));

                        // Trying out world scale
                        sunVisual.setWorldScale(new Vector3(ratio * sunScale, ratio * sunScale, ratio * sunScale));
                        for (int x = 0; x < planetList.size(); x++) {
                            // Sets scale based off ratio
                            //planetList.get(x).setLocalScale(new Vector3(ratio / planetList.get(x).myScale, ratio / planetList.get(x).myScale, ratio / planetList.get(x).myScale));

                            //TODO: Having issues with the planets not scaling up. -> localScale is not working.

                            //TODO: Added worldScale and setLocalPosition. LocalPosition is based off of a different ratio variable.
                            //planetList.get(x).setLocalScale(new Vector3(planetList.get(x).parent.getLocalScale().x * planetList.get(x).myScale, planetList.get(x).parent.getLocalScale().y  * planetList.get(x).myScale, planetList.get(x).parent.getLocalScale().z  * planetList.get(x).myScale));
                            //planetList.get(x).setLocalScale(new Vector3(10.0f, 10.0f, 10.0f));
                            // planetList.get(x).setLocalScale(new Vector3(planetList.get(x).myScale, planetList.get(x).myScale, planetList.get(x).myScale));
                            // Sets local position based off of distance from sun (x vector is changed)
                            //planetList.get(x).setLocalPosition(new Vector3(planetList.get(x).getLocalPosition().x * ratio, planetList.get(x).getLocalPosition().y, planetList.get(x).getLocalPosition().z));
                            planetList.get(x).setWorldScale(new Vector3(planetList.get(x).myScale * ratio, planetList.get(x).myScale * ratio, planetList.get(x).myScale * ratio));
                            planetList.get(x).setLocalPosition(new Vector3((planetList.get(x).fromParent * AU_TO_METERS) * ratioDistance, 0.0f, 0.0f));
                        }

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        SeekBar distanceBar = solarControlsView.findViewById(R.id.distanceBar);
        distanceBar.setProgress((int) (solarSettings.getDistanceMultiplier() * 2.0f));
        distanceBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        float ratio = (float) progress / (float) distanceBar.getMax();
                        solarSettings.setDistanceMultiplier(ratio * 2.0f);

                        for (int i = 0; i < planetList.size(); i++) {
                            planetList.get(i).setLocalPosition(new Vector3(planetList.get(i).getLocalScale().x * ratio, planetList.get(i).getLocalPosition().y, planetList.get(i).getLocalPosition().z));

                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        // Toggle the solar controls on and off by tapping the sun.
        sunVisual.setOnTapListener(
                (hitTestResult, motionEvent) -> solarControls.setEnabled(!solarControls.isEnabled()));

        createPlanet("Mercury", sun, 0.4f, 47f, mercuryRenderable, 0.019f, sunVisual);

        createPlanet("Venus", sun, 0.7f, 35f, venusRenderable, 0.0475f, sunVisual);

        Node earth = createPlanet("Earth", sun, 1.0f, 29f, earthRenderable, 0.05f, sunVisual);

        createPlanet("Moon", earth, 0.15f, 100f, lunaRenderable, 0.018f, sunVisual);

        createPlanet("Mars", sun, 1.5f, 24f, marsRenderable, 0.0265f, sunVisual);

        createPlanet("Jupiter", sun, 2.2f, 13f, jupiterRenderable, 0.16f, sunVisual);

        createPlanet("Saturn", sun, 3.5f, 9f, saturnRenderable, 0.1325f, sunVisual);

        createPlanet("Uranus", sun, 5.2f, 7f, uranusRenderable, 0.1f, sunVisual);

        createPlanet("Neptune", sun, 6.1f, 5f, neptuneRenderable, 0.074f, sunVisual);

        return base;
    }

    private Node createPlanet(
            String name,
            Node parent,
            float auFromParent,
            float orbitDegreesPerSecond,
            ModelRenderable renderable,
            float planetScale,
            Node theSun) {
        // Orbit is a rotating node with no renderable positioned at the sun.
        // The planet is positioned relative to the orbit so that it appears to rotate around the sun.
        // This is done instead of making the sun rotate so each planet can orbit at its own speed.
        RotatingNode orbit = new RotatingNode(solarSettings, true);
        orbit.setDegreesPerSecond(orbitDegreesPerSecond);
        orbit.setParent(parent);

        // Create the planet and position it relative to the sun.
        Planet planet = new Planet(this, name, planetScale, renderable, solarSettings, theSun, auFromParent);
        planetList.add(planet);
        planet.setParent(orbit);

        //planet.setLocalPosition(new Vector3((auFromParent * AU_TO_METERS, 0.0f, 0.0f));
        planet.setLocalPosition(new Vector3(auFromParent * AU_TO_METERS, 0.0f, 0.0f));


        return planet;
    }


  private void showLoadingMessage() {
    if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
      return;
    }

    loadingMessageSnackbar =
        Snackbar.make(
            SolarActivity.this.findViewById(android.R.id.content),
            R.string.plane_finding,
            Snackbar.LENGTH_INDEFINITE);
    loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
    loadingMessageSnackbar.show();
  }

  private void hideLoadingMessage() {
    if (loadingMessageSnackbar == null) {
      return;
    }

    loadingMessageSnackbar.dismiss();
    loadingMessageSnackbar = null;
  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != PERMISSION_CODE) {
      return;
    }
    if (resultCode != RESULT_OK) {
      Toast.makeText(this,
              "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
      mToggleButton.setChecked(false);
      return;
    }
    mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    mVirtualDisplay = createVirtualDisplay();
    mMediaRecorder.start();
  }

  public void onToggleScreenShare(View view) {
    if (((ToggleButton) view).isChecked()) {
      initRecorder();
      prepareRecorder();
      mProjectionManager = (MediaProjectionManager) getSystemService
              (Context.MEDIA_PROJECTION_SERVICE);


      mToggleButton.setBackgroundColor(Color.TRANSPARENT);
      mToggleButton.setText("   ");
      shareScreen();
    } else {
      //mMediaRecorder.stop();
      mMediaRecorder.reset();
      stopScreenSharing();
      mToggleButton.setText("Click to record!");
      mToggleButton.setVisibility(View.VISIBLE);
      mToggleButton.setBackgroundColor(getResources().getColor(R.color.lightBlueTheme));
    }
  }

  private void shareScreen() {
    if (mMediaProjection == null) {
      startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
      return;
    }
    mVirtualDisplay = createVirtualDisplay();
    mMediaRecorder.start();

  }

  private void stopScreenSharing() {
    if (mVirtualDisplay == null) {
      return;
    }
    mVirtualDisplay.release();
    mMediaRecorder.reset();
    //mMediaRecorder.release();
  }

  private VirtualDisplay createVirtualDisplay() {
    return mMediaProjection.createVirtualDisplay("MainActivity",
            DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
  }

  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override
    public void onStop() {
      if (mToggleButton.isChecked()) {
        mToggleButton.setChecked(false);
        mMediaRecorder.stop();
        //mMediaRecorder.reset();
      }
      mMediaProjection = null;
      stopScreenSharing();
    }
  }

  private void prepareRecorder() {
    try {
      mMediaRecorder.prepare();
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      finish();
    }
  }

  public String getFilePath() {
    // DCIM/Camera
    final String directory = Environment.getExternalStorageDirectory() + File.separator + "Recordings";
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show();
      return null;
    }
    final File folder = new File(directory);
    boolean success = true;
    if (!folder.exists()) {
      success = folder.mkdir();
    }
    String filePath;
    if (success) {
      String videoName = ("capture_" + getCurSysDate() + ".mp4");
      filePath = directory + File.separator + videoName;
    } else {
      Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show();
      return null;
    }
    return filePath;
  }

  public String getCurSysDate() {
    return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
  }

  private void initRecorder() {
    int YOUR_REQUEST_CODE = 200; // could be something else..
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
              YOUR_REQUEST_CODE);


      if (mMediaRecorder == null) {
        mMediaRecorder = new MediaRecorder();
      }
      //CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);

      mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
      mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
      mMediaRecorder.setVideoFrameRate(30);
      mMediaRecorder.setOutputFile(getFilePath());
      mMediaRecorder.setVideoEncodingBitRate(10000000);

      //mMediaRecorder.setOutputFile(getFilePath());
    }
  }
}
