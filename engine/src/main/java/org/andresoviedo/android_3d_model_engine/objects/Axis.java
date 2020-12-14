package org.andresoviedo.android_3d_model_engine.objects;

import android.opengl.GLES20;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.io.IOUtils;

public final class Axis {

    public static Object3DData build() {
        return build(0.025f);
    }

    /**
     * Build a -1 to 1 unit axis in all axis with a letter on the positive direction
     *
     * @param letterSize unit size of the letters, between 0 and 1
     * @return the 3D axis
     */
    public static Object3DData build(float letterSize) {

        final float ls = letterSize;

        final float[] axisVertexLinesData = new float[]{
                //@formatter:off
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, // right
                0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, // left
                0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, // up
                0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, // down
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, // z+
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, // z-

                //0.95f, 0.15f, 0, 1, 0, 0, 0.95f, -0.15f, 0, 1, 0f, 0f, // Arrow X (>)
                //-0.95f, 0.15f, 0, -1, 0, 0, -0.95f, -0.15f, 0, -1, 0f, 0f, // Arrow X (<)
                //-0.15f, 0.95f, 0, 0, 1, 0, 0.15f, 0.95f, 0, 0, 1f, 0f, // Arrox Y (^)
                //-0.15f, 0, 0.95f, 0, 0, 1, 0.15f, 0, 0.95f, 0, 0, 1, // Arrox z (v)

                1, ls, ls, 1, -ls, -ls, 1, -ls, ls, 1F, ls, -ls, // Letter X
                -ls * 0.7f, 1, -ls, 0, 1, 0, ls * 0.7f, 1, -ls, -ls * 0.7f, 1, ls, // Letter Y
                -ls, ls, 1, ls, ls, 1, ls, ls, 1, -ls, -ls, 1, -ls, -ls, 1, ls, -ls, 1 // letter z
                //@formatter:on
        };

        return new Object3DData(IOUtils.createFloatBuffer(axisVertexLinesData.length).put(axisVertexLinesData))
                .setDrawMode(GLES20.GL_LINES).setId("axis");
    }
}
