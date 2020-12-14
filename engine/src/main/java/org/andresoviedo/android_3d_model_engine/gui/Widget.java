package org.andresoviedo.android_3d_model_engine.gui;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.animation.JointTransform;
import org.andresoviedo.android_3d_model_engine.drawer.Renderer;
import org.andresoviedo.android_3d_model_engine.drawer.RendererFactory;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Dimensions;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.event.EventListener;
import org.andresoviedo.util.io.IOUtils;
import org.andresoviedo.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

public class  Widget extends Object3DData implements EventListener {

    protected final float[] projectionMatrix = new float[16];
    protected final float[] viewMatrix = new float[16];
    private final float[] cameraPosition = new float[]{0, 0, 1.1f};
    protected int width;
    protected int height;
    //  protected float ratio;
    protected boolean bindSceneViewMatrix = false;

    protected static Widget of(Object3DData data) {
        final Widget widget = new Widget();
        data.copy(widget);
        //widget.addWidget(data);
        return widget;
    }

    public static class ClickEvent extends EventObject {

        private final Object3DData widget;
        private final float x;
        private final float y;
        private final float z;

        ClickEvent(Object source, Object3DData widget, float x, float y, float z) {
            super(source);
            this.widget = widget;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }

        public Object3DData getWidget() {
            return widget;
        }
    }

    public static class MoveEvent extends EventObject {

        private final Object3DData widget;
        private final float x;
        private final float y;
        private final float z;
        private final float dx;
        private final float dy;

        MoveEvent(Object source, Object3DData widget, float x, float y, float z, float dx, float dy) {
            super(source);
            this.widget = widget;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dx = dx;
            this.dy = dy;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }

        public Object3DData getWidget() {
            return widget;
        }

        public float getDx() {
            return dx;
        }

        public float getDy() {
            return dy;
        }
    }

    public static final int POSITION_TOP_LEFT = 0;
    public static final int POSITION_TOP = 1;
    public static final int POSITION_MIDDLE = 4;
    public static final int POSITION_RIGHT = 5;
    public static final int POSITION_TOP_RIGHT = 2;
    public static final int POSITION_BOTTOM = 7;
    public static final int POSITION_BOTTOM_RIGHT = 3;
    public static final int POSITION_BOTTOM_LEFT = 6;
    public static final int POSITION_LEFT = 8;

    private static int counter = 0;

    // we need this to calculate relative position (screen width / screen height)
    float ratio;

    final List<Widget> widgets = new ArrayList<>();

    // This is the original position of the widget - to rollback animation
    private float[] initialPosition;

    // This is the original position of the widget - to rollback animation
    float[] initialScale;

    {
        setId(getClass().getSimpleName()+"_"+ ++counter);
        setDrawUsingArrays(true);
        setDrawMode(GLES20.GL_LINE_STRIP);
        setVisible(false);
    }

    private Runnable animation;

    public void clear(){
        this.widgets.clear();
    }

    @Override
    public Object3DData setColor(float[] color) {
        super.setColor(color);

        if (!isDrawUsingArrays()) return this;
        if (getColorsBuffer() == null) return this;


        FloatBuffer buffer = getColorsBuffer();
        for (int i = 0; i < buffer.capacity(); i += color.length) {
            float alpha = buffer.get(i + 3);
            if (alpha > 0f) {
                buffer.position(i);
                buffer.put(color);
            }
        }
        setColorsBuffer(buffer);

        return this;
    }

    static void fillArraysWithZero(FloatBuffer vertexBuffer, FloatBuffer colorBuffer){
        for (int i = vertexBuffer.position(); i < vertexBuffer.capacity(); i++) {
            vertexBuffer.put(0f);
        }
        for (int i = colorBuffer.position(); i < colorBuffer.capacity(); i++) {
            colorBuffer.put(0f);
        }
    }

    static void buildBorder(FloatBuffer vertexBuffer, FloatBuffer colorBuffer, float width, float height) {

        // hidden line
        int mark = vertexBuffer.position();
        vertexBuffer.put(vertexBuffer.get(mark-3));
        vertexBuffer.put(vertexBuffer.get(mark-2));
        vertexBuffer.put(vertexBuffer.get(mark-1));
        for (int i = 0; i < 4; i++) colorBuffer.put(0f);
        vertexBuffer.put(0).put(0).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(0f);

        // border
        vertexBuffer.put(0).put(0).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(1f);

        vertexBuffer.put(width).put(0).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(1f);

        vertexBuffer.put(width).put(height).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(1f);

        vertexBuffer.put(0).put(height).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(1f);

        vertexBuffer.put(0).put(0).put(0);
        for (int i = 0; i < 4; i++) colorBuffer.put(1f);

    }

    protected Widget addBackground(Widget widget){
        Widget background = Quad.build(widget.getCurrentDimensions());
        background.setId(widget.getId()+"_bg");
        background.setScale(widget.getScale());
        background.setLocation(widget.getLocation());
        background.setVisible(widget.isVisible());
        background.setSolid(false);
        widget.addListener(background);
        this.widgets.add(0, background);
        Log.v("GUI", background.toString());
        return background;
    }

    /**
     * Should be called only when object has dimensions
     * @param relativePosition
     * @return
     */
    public Object3DData setPosition(int relativePosition) {
        this.setLocation(calculatePosition(relativePosition, getCurrentDimensions(), getScaleX(), ratio));
        return this;
    }

    /**
     * @param scale scale relative to parent. 1f = 100%
     * @return this
     */
    @Override
    public Object3DData setScale(float[] scale) {
        if (this.initialScale == null)
            this.initialScale = scale;
        return super.setScale(scale);
    }

    public Object3DData setRelativeScale(float[] scale){
        // default scale
        if (getParent() == null)
            return setScale(scale);

        // recalculate based on parent
        // That is, -1+1 is 100% parent dimension
        Dimensions parentDim = getParent().getCurrentDimensions();
        float relScale = parentDim.getRelationTo(getCurrentDimensions());
        Log.v("Widget","Relative scale ("+getId()+"): "+relScale);
        float[] newScale = Math3DUtils.multiply(scale, relScale);
        Log.v("Widget","New scale ("+getId()+"): "+Arrays.toString(newScale));

        return super.setScale(newScale);
    }

    float[] getInitialScale() {
        if (initialScale == null) return getScale();
        return initialScale;
    }

    @Override
    public Object3DData setLocation(float[] location) {
        //og.i("Widget", "setPosition() id:"+getId()+", position: "+ Arrays.toString(position));
        super.setLocation(location);
        if (this.initialPosition == null)
            this.initialPosition = location.clone();
        return this;
    }

    public float[] getInitialPosition() {
        return initialPosition;
    }

    /**
     * Calculate final position taking into account the screen ratio (vertical or horizontal layout)
     *
     * @param relativePosition the widget's relative position to screen
     * @param dimensions the widget's dimensions
     * @param newScale the widget's scale
     * @param ratio screen width ratio
     * @return the final position of the widget in the screen
     */
    static float[] calculatePosition(int relativePosition, Dimensions dimensions, float newScale, float ratio){

        float x, y, z = 0;
        switch (relativePosition) {
            case POSITION_TOP_LEFT:
                x = -ratio - dimensions.getMin()[0];
                y = 1 - dimensions.getMax()[1];
                z = -dimensions.getMax()[2];
                break;
            case POSITION_TOP:
                x = -dimensions.getWidth()/2 - dimensions.getMin()[0];
                y = 1 - dimensions.getMax()[1];
                break;
            case POSITION_TOP_RIGHT:
                x = ratio - dimensions.getMax()[0];
                y = 1 - dimensions.getMax()[1];
                break;
            case POSITION_MIDDLE:
                x = -dimensions.getWidth()/2 - dimensions.getMin()[0];
                y = -dimensions.getHeight()/2 - dimensions.getMin()[1];
                break;
            case POSITION_RIGHT:
                x = ratio -dimensions.getWidth();
                x -= dimensions.getMin()[0];
                y = -dimensions.getCenter()[1];
                break;
            case POSITION_BOTTOM:
                x = -dimensions.getWidth()/2 - dimensions.getMin()[0];
                y = -1 - dimensions.getMin()[1];
                break;
            case POSITION_BOTTOM_RIGHT:
                x = ratio -dimensions.getWidth();
                x -= dimensions.getMin()[0];
                y = -1 - dimensions.getMin()[1];
                break;
            case POSITION_BOTTOM_LEFT:
                x = -ratio - dimensions.getMin()[0];
                y = -1 - dimensions.getMin()[1];
                break;
            case POSITION_LEFT:
                x = -ratio + dimensions.getWidth();
                x += dimensions.getMin()[0];
                y = dimensions.getHeight()/2;
                y += dimensions.getMin()[1];
                break;
            default:
                throw new UnsupportedOperationException();
        }
        Log.d("Widget","Final position: "+ Arrays.toString(new float[]{x,y,z})+", dimensions:"+dimensions+", scale: "+newScale);
        return new float[]{x, y, z};
    }

    void animate(final JointTransform start, final JointTransform end, long millis){
        final JointTransform result = new JointTransform(new float[16]);
        final long startTime = SystemClock.uptimeMillis();
        result.setVisible(start.isVisible());
        setVisible(start.isVisible());
        animation = () -> {
            long elapsed = SystemClock.uptimeMillis() - startTime;
            if (elapsed >= millis) {
                elapsed = millis;
                result.setVisible(end.isVisible());
                animation = null;
            }
            float progression = (float) elapsed / millis;
            Math3DUtils.interpolate(result, start, end, progression);
            setLocation(unbox(result.getLocation()));
            setScale(unbox(result.getScale()));
            setRotation(unbox(result.getRotation1()));
            setRotation2(unbox(result.getRotation2()), unbox(result.getRotation2Location()));
            setVisible(result.isVisible());
        };
    }

    private static float[] unbox(Float[] boxed){
        float[] ret = new float[boxed.length];
        for (int i=0; i<boxed.length; i++){
            ret[i] = boxed[i];
        }
        return ret;
    }

    public void onDrawFrame(){
        if (animation != null) animation.run();
    }

    /**
     * Unproject world coordinates into model original coordinates
     * @param clickEvent click event
     * @return original model coordinates
     */
    protected float[] unproject(GUI.ClickEvent clickEvent){
        float x = clickEvent.getX();
        float y = clickEvent.getY();
        float z = clickEvent.getZ();
        x -= getLocationX();
        x /= getScaleX();
        y -= getLocationY();
        y /= getScaleY();
        z -= getLocationZ();
        z /= getScaleZ();
        return new float[]{x,y,z};
    }

    /**
     * @param width horizontal screen pixels
     * @param height vertical screen pixels
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.ratio = (float) width / height;

        // setup view & projection
        Matrix.setLookAtM(viewMatrix, 0,
                cameraPosition[0], cameraPosition[1], cameraPosition[2],
                0, 0, 0,
                0, 1, 0);
        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 10);

        FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(5 * 3);
        vertexBuffer.put(-ratio).put(-1).put(-1);
        vertexBuffer.put(+ratio).put(-1).put(-1);
        vertexBuffer.put(+ratio).put(1).put(-1);
        vertexBuffer.put(-ratio).put(1).put(-1);
        vertexBuffer.put(-ratio).put(-1).put(-1);
        setVertexBuffer(vertexBuffer);

        setVisible(true);

    }

    public void setBindSceneViewMatrix(boolean bind){
        this.bindSceneViewMatrix = bind;
    }

    public void render(RendererFactory rendererFactory, Camera viewMatrix, float[] lightPosInWorldSpace, float[] colorMask) {
        super.render(rendererFactory, viewMatrix, lightPosInWorldSpace, colorMask);
        for (int i = 0; i < widgets.size(); i++) {
            renderWidget(rendererFactory, viewMatrix, widgets.get(i), lightPosInWorldSpace, colorMask);
        }
    }

    private void renderWidget(RendererFactory rendererFactory, Camera viewMatrix, Widget widget, float[] lightPosInWorldSpace, float[]
            colorMask) {

        // if widget is now visible don't draw it
        if (!widget.isVisible()) return;

        // run animation on widget
        if (widget instanceof Widget) {
            ((Widget)widget).onDrawFrame();
        }

        // get drawer for this widget
        Renderer drawer = rendererFactory.getDrawer(widget, false, false, false, false, true);


        GLES20.glLineWidth(2.0f);


        if (widget.bindSceneViewMatrix) {
            float[] transform = new float[16];
            Matrix.setLookAtM(transform, 0,
                    0, 0, 0,
                    -viewMatrix.getxPos(), -viewMatrix.getyPos(), -viewMatrix.getzPos(),
                    viewMatrix.getxUp() - viewMatrix.getxPos(), viewMatrix.getyUp() - viewMatrix.getyPos(), viewMatrix.getzUp() - viewMatrix.getzPos());
            widget.setBindTransform(transform);
        }

        drawer.draw(widget, projectionMatrix, this.viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosition);
    }

    @Override
    public boolean onEvent(EventObject event) {
        return true;
    }

    // set position based on layout:
    // top-left, top-middle, top-right       =   0 1 2
    // middle-left, middle, middle-right     =   3 4 5
    // bottom-left, bottom, bottom-right     =   6 7 8
    public void addWidget(Widget widget) {
        this.widgets.add(widget);
        if (widget instanceof Widget){
            ((Widget) widget).ratio = ratio;
        }
        addListener(widget);
        if (widget.getParent() == null) widget.setParent(this);
        Log.i("Widget","New widget: "+widget);
    }
}