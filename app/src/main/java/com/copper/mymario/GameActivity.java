package com.copper.mymario;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yalantis.starwars.TilesFrameLayout;
import com.yalantis.starwars.interfaces.TilesFrameLayoutListener;

import java.io.IOException;
import java.io.InputStream;

public class GameActivity extends AppCompatActivity implements TilesFrameLayoutListener {
    // frame width
    private static final int FRAME_W = 71;
    // frame height
    private static final int FRAME_H = 85;
    // number of frames
    private static final int NB_FRAMES = 8;
    // nb of frames in x
    private static final int COUNT_X = 8;
    // nb of frames in y
    private static final int COUNT_Y = 1;
    // frame duration
    // we can slow animation by changing frame duration
    private static final int FRAME_DURATION = 200; // in ms !
    // scale factor for each frame
    private static final int SCALE_FACTOR = 5;
    // stores each frame
    private Bitmap[] bmps;

    private RelativeLayout relativeLayout;
    private ImageView object;
    private ImageView tree1;
    private ImageView tree2;
    private ImageView firstCloud;
    private ImageView secondCloud;
    private ImageView restart;
    private TextView scoreText;
    private float deviceWidth;
    private boolean isMarioAnimating;
    private boolean isMarioDead;
    private boolean isStartedGame;
    private Rect rect1;
    private Rect rect2;
    private Rect rect3;
    private Handler handler;
    private Runnable runnable;
    private ValueAnimator firstCloudAnimation;
    private ValueAnimator secondCloudAnimation;
    private ValueAnimator firstTreeAnimation;
    private ValueAnimator secondTreeAnimation;
    private ValueAnimator upwardAnimation;
    private ValueAnimator downwardAnimation;
    private AnimationDrawable marioLegAnimationDrawable;
    private double score;
    private MediaPlayer mediaPlayerBg;
    private MediaPlayer mediaPlayerJump;
    private MediaPlayer mediaPlayerDie;
    private TilesFrameLayout tilesFrameLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        relativeLayout = (RelativeLayout) findViewById(R.id.activity_google_game);
        object = (ImageView) findViewById(R.id.object);
        tree1 = (ImageView) findViewById(R.id.tree1);
        tree2 = (ImageView) findViewById(R.id.tree2);
        firstCloud = (ImageView) findViewById(R.id.firstCloud);
        secondCloud = (ImageView) findViewById(R.id.secondCloud);
        restart = (ImageView) findViewById(R.id.restart);
        scoreText = (TextView) findViewById(R.id.score_text);

        tilesFrameLayout = (TilesFrameLayout) findViewById(R.id.tiles_frame_layout);
        tilesFrameLayout.setOnAnimationFinishedListener(this);

        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isStartedGame) {
                    stopMediaPlayer();
                    initialize();
                    isStartedGame = true;
                }
                if (isStartedGame && isMarioDead) {
                    isMarioDead = false;
                    stopMediaPlayer();
                    initialize();
                    isMarioAnimating = false;
                    score = 0;
                }
            }
        });

    }

    private void initialize() {

        getScreenWidth();

        setCloudAnimation();

        setFirstTreeAnimation();
        setSecondTreeAnimation();

        setMarioClickListener();

        applySpriteAnimation();

        startTimer();

        playMusic();
    }


    private void playMusic() {
        mediaPlayerBg = MediaPlayer.create(this, R.raw.supermario_bg);
        mediaPlayerBg.setVolume(0.09f, 0.09f);
        mediaPlayerBg.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayerBg.start();
    }


    private void startTimer() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {

                if (checkCollision()) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);

                    firstCloudAnimation.cancel();
                    secondCloudAnimation.cancel();
                    firstTreeAnimation.cancel();
                    secondTreeAnimation.cancel();
                    if (upwardAnimation != null) {
                        upwardAnimation.cancel();
                    }
                    if (downwardAnimation != null) {
                        downwardAnimation.cancel();
                    }
                    marioLegAnimationDrawable.setOneShot(true);
                    isMarioAnimating = true;
                    isMarioDead = true;
                    handler.removeCallbacks(runnable);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        object.setY(657);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        object.setY(1353);
                    }

//                    tilesFrameLayout.startAnimation();

//                    ExplosionField explosionField = new ExplosionField(getBaseContext());
//                    explosionField.explode(object);

                    //play music for death of mario
                    mediaPlayerDie = MediaPlayer.create(getBaseContext(), R.raw.mario_die);
                    mediaPlayerDie.start();

                    Toast.makeText(getBaseContext(), "Game Over........", Toast.LENGTH_SHORT).show();
                } else {
                    handler.postDelayed(runnable, 100);
                }
            }
        };
        handler.postDelayed(runnable, 100);
    }

    public boolean checkCollision() {

        rect1 = new Rect((int) object.getX() + 20, (int) object.getY() + 10, (int) object.getX() + object.getWidth() - 20, (int) object.getY() + object.getHeight() - 10);
        rect2 = new Rect((int) tree1.getX() + 20, (int) tree1.getY() + 10, (int) tree1.getX() + tree1.getWidth() - 20, (int) tree1.getY() + tree1.getHeight() - 10);
        rect3 = new Rect((int) tree2.getX() + 20, (int) tree2.getY() + 10, (int) tree2.getX() + tree2.getWidth() - 20, (int) tree2.getY() + tree2.getHeight() - 10);

        if (rect1 != null && rect2 != null && rect3 != null) {
            return rect1.intersect(rect2) || rect1.intersect(rect3);
        }
        return false;

    }


    private void setCloudAnimation() {
        firstCloud.setX(deviceWidth);

        firstCloudAnimation = ValueAnimator.ofFloat(deviceWidth, -deviceWidth - 300);
        firstCloudAnimation.setRepeatCount(ValueAnimator.INFINITE);
        firstCloudAnimation.setInterpolator(new LinearInterpolator());
        firstCloudAnimation.setDuration(7000L);
        firstCloudAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                firstCloud.setTranslationX(progress);
            }
        });
        firstCloudAnimation.start();

        secondCloud.setX(deviceWidth);

        secondCloudAnimation = ValueAnimator.ofFloat(deviceWidth, -deviceWidth - 300);
        secondCloudAnimation.setRepeatCount(ValueAnimator.INFINITE);
        secondCloudAnimation.setInterpolator(new LinearInterpolator());
        secondCloudAnimation.setDuration(9000L);
        secondCloudAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                secondCloud.setTranslationX(progress);
            }
        });
        secondCloudAnimation.start();

    }


    private void setFirstTreeAnimation() {
        tree1.setX(deviceWidth);

        firstTreeAnimation = ValueAnimator.ofFloat(deviceWidth, -deviceWidth - 300);
        firstTreeAnimation.setRepeatCount(ValueAnimator.INFINITE);
        firstTreeAnimation.setInterpolator(new LinearInterpolator());
        firstTreeAnimation.setDuration(setSpeed((long) ((Math.random()) * 9000) + 1000));


        firstTreeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();

                score += Math.ceil((deviceWidth - (Math.abs(progress))) / 10000);

                scoreText.setText("Score : " + "" + (int) score);

                tree1.setTranslationX(progress);
            }
        });
        firstTreeAnimation.start();

    }


    private long setSpeed(long speed) {
//        long speed = (long) ((Math.random()) * 9000) + 1000;
        if (speed > 5000 && speed < 8000) {
            return speed;
        }
        return 5000;
    }

    private void setSecondTreeAnimation() {

        tree2.setX(deviceWidth);
        secondTreeAnimation = ValueAnimator.ofFloat(deviceWidth, -deviceWidth - 300);
        secondTreeAnimation.setRepeatCount(ValueAnimator.INFINITE);
        secondTreeAnimation.setInterpolator(new LinearInterpolator());
        secondTreeAnimation.setDuration(setSpeed((long) ((Math.random()) * 9000) + 1000) / 2);
        secondTreeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                tree2.setTranslationX(progress);
            }
        });
        secondTreeAnimation.start();

    }


    private void setMarioClickListener() {

        relativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (!isMarioAnimating && !isMarioDead) {

                        //Music for jumping mario
                        mediaPlayerJump = MediaPlayer.create(getBaseContext(), R.raw.mario_jump);
                        mediaPlayerJump.start();

                        upwardAnimation = ValueAnimator.ofFloat(0, -500);
                        upwardAnimation.setRepeatCount(0);
                        upwardAnimation.setInterpolator(new LinearInterpolator());
                        upwardAnimation.setDuration(300);
                        upwardAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                final float progress = (float) animation.getAnimatedValue();
                                object.setTranslationY(progress);
                                isMarioAnimating = true;
                            }
                        });
                        upwardAnimation.start();

                        upwardAnimation.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                isMarioAnimating = true;
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                isMarioAnimating = true;
                                setDownwardAnimation();
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                                setDownwardAnimation();
                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                    }
                }
                return true;
            }
        });

    }


    private void setDownwardAnimation() {
        downwardAnimation = ValueAnimator.ofFloat(-500, 0);
        downwardAnimation.setRepeatCount(0);
        downwardAnimation.setInterpolator(new LinearInterpolator());
        downwardAnimation.setDuration(300);
        downwardAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                object.setTranslationY(progress);
                isMarioAnimating = true;
            }
        });
        downwardAnimation.start();

        downwardAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isMarioAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mediaPlayerJump != null) {
                    mediaPlayerJump.stop();
                    mediaPlayerJump.release();
                    mediaPlayerJump = null;
                }
                isMarioAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

    }


    private void getScreenWidth() {
        final Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point deviceDisplay = new Point();
        display.getSize(deviceDisplay);
        deviceWidth = deviceDisplay.x;
    }


    private void applySpriteAnimation() {
        // load bitmap from assets
        Bitmap bitmap = getBitmapFromAssets(this, "mario.png");

        if (bitmap != null) {
            // cut bitmaps from bird bmp to array of bitmaps
            bmps = new Bitmap[NB_FRAMES];
            int currentFrame = 0;

            for (int i = 0; i < COUNT_Y; i++) {
                for (int j = 0; j < COUNT_X; j++) {
                    bmps[currentFrame] = Bitmap.createBitmap(bitmap, FRAME_W
                            * j, FRAME_H * i, FRAME_W, FRAME_H);

                    // apply scale factor
                    bmps[currentFrame] = Bitmap.createScaledBitmap(
                            bmps[currentFrame], FRAME_W * SCALE_FACTOR, FRAME_H
                                    * SCALE_FACTOR, true);

                    if (++currentFrame >= NB_FRAMES) {
                        break;
                    }
                }
            }

            // create animation programmatically
            marioLegAnimationDrawable = new AnimationDrawable();
            marioLegAnimationDrawable.setOneShot(false); // repeat animation

            for (int i = 0; i < NB_FRAMES; i++) {
                marioLegAnimationDrawable.addFrame(new BitmapDrawable(getResources(), bmps[i]),
                        FRAME_DURATION);
            }

            // load animation on image
            if (Build.VERSION.SDK_INT < 16) {
                object.setBackgroundDrawable(marioLegAnimationDrawable);
            } else {
                object.setBackground(marioLegAnimationDrawable);
            }

            // start animation on image
            object.post(new Runnable() {

                @Override
                public void run() {
                    marioLegAnimationDrawable.start();
                }

            });

        }
    }


    private Bitmap getBitmapFromAssets(Context context,
                                       String filepath) {
        AssetManager assetManager = context.getAssets();
        InputStream istr = null;
        Bitmap bitmap = null;

        try {
            istr = assetManager.open(filepath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException ioe) {
            // manage exception
        } finally {
            if (istr != null) {
                try {
                    istr.close();
                } catch (IOException e) {
                }
            }
        }

        return bitmap;
    }


    @Override
    protected void onStop() {
        super.onStop();

        stopMediaPlayer();
    }


    private void stopMediaPlayer() {
        if (mediaPlayerBg != null) {
            mediaPlayerBg.stop();
            mediaPlayerBg.release();
            mediaPlayerBg = null;
        }
        if (mediaPlayerDie != null) {
            mediaPlayerDie.stop();
            mediaPlayerDie.release();
            mediaPlayerDie = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMediaPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mediaPlayerBg = MediaPlayer.create(this, R.raw.supermario_bg);
        mediaPlayerBg.setVolume(0.09f, 0.09f);
//        mediaPlayerBg.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayerBg.start();
    }

    @Override
    public void onAnimationFinished() {

    }
}
