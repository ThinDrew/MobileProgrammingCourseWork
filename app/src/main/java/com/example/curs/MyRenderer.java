package com.example.curs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    private Model cup, water, banana, apple, table, pear, orange;
    private Context context;
    float[] eyePos = new float[3];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    int[] textureNames = new int[]{R.drawable.apple_texture, R.drawable.banana_texture, R.drawable.pear_texture, R.drawable.table_texture, R.drawable.orange_texture, R.drawable.cup_texture};
    int[] textures;
    float time;

    MyRenderer(Context context){
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        time = 0;

        //Отсечение нелицевых граней
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        //Проверка на ошибки
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("OpenGLError", "Error code: " + error);
        }

        //Массив для смещения объектов
        float[] translate;
        //Переменная для изменения масштаба объектов
        float scale = 1;

        //Генерируем текстуры
        textures = new int[textureNames.length];
        GLES20.glGenTextures(textures.length, textures, 0);

        //Заполняем и связываем текстуры
        for(int i = 0; i < textureNames.length; i++){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            Bitmap pearBitmap = BitmapFactory.decodeResource(context.getResources(), textureNames[i]);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, pearBitmap, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        //Создание моделей, передача в них текстур и параметров для смещения, масштабирования и сглаживания
        translate = new float[]{2, 0, 2};
        pear = new Model(context, "pear.obj", textures[2], GLES20.GL_TEXTURE2, translate, scale, true);

        translate = new float[]{0, 0, 0};
        apple = new Model(context, "apple.obj", textures[0], GLES20.GL_TEXTURE0, translate, scale, true);

        translate = new float[]{0, -0.5f, -4};
        scale = 0.5f;
        banana = new Model(context, "banana.obj", textures[1], GLES20.GL_TEXTURE1, translate, scale, true);

        translate = new float[]{0, -3.5f, 0};
        scale = 0.5f;
        table = new Model(context, "table.obj", textures[3], GLES20.GL_TEXTURE3, translate, scale, false);

        translate = new float[]{5, 0, -5};
        scale = 1;
        orange = new Model(context, "orange.obj", textures[4], GLES20.GL_TEXTURE4, translate, scale, true);

        translate = new float[]{5, 0, 5};
        float[] cupColor = new float[]{0.2f, 0.2f, 0.2f, 0.5f};
        cup = new Model(context, "cup.obj", cupColor, translate, scale, false);

        float[] waterColor = new float[]{0f, 0.2f, 1f, 0.8f};
        water = new Model(context, "water.obj", waterColor, translate, scale, false);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 1000);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);

        eyePos[0] = (float)Math.cos(time)*30f;
        eyePos[1] = 30f;
        eyePos[2] = (float)Math.sin(time)*30f;

        Matrix.setLookAtM(viewMatrix, 0, eyePos[0], eyePos[1], eyePos[2], 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        //Отрисовка объектов с текстурами
        pear.draw(viewProjectionMatrix, eyePos, true);
        apple.draw(viewProjectionMatrix, eyePos, true);
        banana.draw(viewProjectionMatrix, eyePos, true);
        orange.draw(viewProjectionMatrix, eyePos, true);
        table.draw(viewProjectionMatrix, eyePos, true);

        //Включаем прозрачность для отрисовки стакана и воды
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthFunc(GLES20.GL_ALWAYS);

        water.draw(viewProjectionMatrix, eyePos, false);
        cup.draw(viewProjectionMatrix, eyePos, false);

        GLES20.glDepthFunc(GLES20.GL_LESS);

        //Отключаем прозрачность после отрисовки стакана и воды
        GLES20.glDisable(GLES20.GL_BLEND);

        time+=0.01f;
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Проверяем статус компиляции шейдера
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            // Компиляция завершилась неудачно, получаем журнал компиляции
            String shaderInfoLog = GLES20.glGetShaderInfoLog(shader);
            Log.e("ShaderCompilation Error", "Error compiling shader: " + shaderInfoLog);

            // Здесь вы можете добавить оповещение на экран или выполнить другие действия по обработке ошибки
        } else {
            Log.d("ShaderCompilation", "Shader compiled successfully");
        }

        return shader;
    }
}

