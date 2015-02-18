package cullycross.airhockeygame;

import android.content.Context;
import android.opengl.GLSurfaceView;

import static android.opengl.Matrix.*;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cullycross.airhockeygame.objects.Mallet;
import cullycross.airhockeygame.objects.Puck;
import cullycross.airhockeygame.objects.Table;
import cullycross.airhockeygame.programs.ColorShaderProgram;
import cullycross.airhockeygame.programs.TextureShaderProgram;
import cullycross.airhockeygame.utils.Geometry;
import cullycross.airhockeygame.utils.MatrixHelper;
import cullycross.airhockeygame.utils.TextureHelper;

/**
 * Created by cullycross on 2/14/15.
 */
public class AirHockeyRenderer implements GLSurfaceView.Renderer {

    private final Context mContext;

    private final float [] mProjectionMatrix = new float[16];
    private final float [] mModelMatrix = new float[16];
    private final float [] mViewMatrix = new float[16];
    private final float [] mViewProjectionMatrix = new float[16];
    private final float [] mModelViewProjectionMatrix = new float[16];
    private final float [] mInvertedViewProjectionMatrix = new float[16];

    private Table mTable;
    private Mallet mMallet;
    private Puck mPuck;

    private TextureShaderProgram mTextureProgram;
    private ColorShaderProgram mColorProgram;

    private int mTexture;

    private boolean mMalletPressed = false;
    private Geometry.Point mBlueMalletPosition;

    public AirHockeyRenderer(Context context) {
        this.mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mTable = new Table();
        mMallet = new Mallet(0.08f, 0.15f, 32);
        mPuck = new Puck(0.06f, 0.02f, 32);

        mTextureProgram = new TextureShaderProgram(mContext);
        mColorProgram = new ColorShaderProgram(mContext);

        mBlueMalletPosition =
                new Geometry.Point(0f, mMallet.height / 2f, 0.4f);

        mTexture = TextureHelper
                .loadTexture(mContext, R.drawable.air_hockey_surface_512x512);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0 ,0 , width, height);

        MatrixHelper.perspectiveM(mProjectionMatrix, 45,
                (float) width / (float) height, 1f, 10f);

        setLookAtM(mViewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        multiplyMM(mViewProjectionMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        invertM(mInvertedViewProjectionMatrix, 0, mViewProjectionMatrix, 0);

        positionTableInTheScene();
        mTextureProgram.useProgram();
        mTextureProgram.setUniforms(mModelViewProjectionMatrix, mTexture);
        mTable.bindData(mTextureProgram);
        mTable.draw();

        positionObjectInScene(0f, mMallet.height / 2f, -0.4f);
        mColorProgram.useProgram();
        mColorProgram.setUniforms(mModelViewProjectionMatrix, 1f, 0f, 0f);
        mMallet.bindData(mColorProgram);
        mMallet.draw();

        positionObjectInScene(0f, mMallet.height / 2f, 0.4f);
        mColorProgram.setUniforms(mModelViewProjectionMatrix, 0f, 0f, 1f);
        mMallet.draw();

        positionObjectInScene(0f, mPuck.height / 2f, 0f);
        mColorProgram.setUniforms(mModelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        mPuck.bindData(mColorProgram);
        mPuck.draw();
    }

    private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(mModelMatrix, 0);
        translateM(mModelMatrix, 0, x, y, z);
        multiplyMM(mModelViewProjectionMatrix, 0, mViewProjectionMatrix,
                0, mModelMatrix, 0);
    }

    private void positionTableInTheScene() {
        setIdentityM(mModelMatrix, 0);
        rotateM(mModelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(mModelViewProjectionMatrix,
                0, mViewProjectionMatrix, 0, mModelMatrix, 0);
    }

    public void handleTouchDrag(float normalizedX, float normalizedY) {

    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        Sphere malletBoundingSphere = new Sphere(new Geometry.Point(
                mBlueMalletPosition.x,
                mBlueMalletPosition.y,
                mBlueMalletPosition.z),
                mMallet.height / 2f
        );
        mMalletPressed = Geometry.intersects(malletBoundingSphere, ray);
    }

    private Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {

        final float [] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float [] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float [] nearPointWorld = new float[4];
        final float [] farPointWorld = new float[4];

        multiplyMM(nearPointWorld, 0,
                mInvertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMM(farPointWorld, 0,
                mInvertedViewProjectionMatrix, 0, farPointNdc, 0);

        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay =
                new Geometry.Point(nearPointWorld[0],
                        nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay =
                new Geometry.Point(farPointWorld[0],
                        farPointWorld[1], farPointWorld[2]);

        return new Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }
}
