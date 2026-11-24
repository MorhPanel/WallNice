package com.morhpanel.nekowall.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import android.view.SurfaceHolder
import com.morhpanel.nekowall.data.models.WallpaperFitMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VideoGlRenderer(
    private val surfaceHolder: SurfaceHolder,
    private val onFrameRendered: () -> Unit = {}
) {
    private val lock = Any()

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null

    private var program: Int = 0
    private var aPositionHandle: Int = 0
    private var aTexCoordHandle: Int = 0
    private var uSTMatrixHandle: Int = 0
    private var sTextureHandle: Int = 0
    private var uModeHandle: Int = 0
    private var uBlurOffsetHandle: Int = 0

    @Volatile
    private var surfaceWidth: Int = 1080
    @Volatile
    private var surfaceHeight: Int = 1920
    @Volatile
    private var videoWidth: Int = 1080
    @Volatile
    private var videoHeight: Int = 1920

    private val stMatrix = FloatArray(16)
    private val quadCoords = FloatArray(8)
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private val texCoords = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    init {
        vertexBuffer = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texCoords)
                position(0)
            }
    }

    private val vertexShaderSource = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uSTMatrix;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = (uSTMatrix * aTexCoord).xy;
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;
        uniform int uMode;
        uniform vec2 uBlurOffset;

        void main() {
            if (uMode == 1) {
                vec4 color = vec4(0.0);
                color += texture2D(sTexture, vTexCoord + vec2(-uBlurOffset.x * 1.5, -uBlurOffset.y * 1.5)) * 0.08;
                color += texture2D(sTexture, vTexCoord + vec2(0.0, -uBlurOffset.y * 1.5)) * 0.12;
                color += texture2D(sTexture, vTexCoord + vec2(uBlurOffset.x * 1.5, -uBlurOffset.y * 1.5)) * 0.08;
                color += texture2D(sTexture, vTexCoord + vec2(-uBlurOffset.x * 1.5, 0.0)) * 0.12;
                color += texture2D(sTexture, vTexCoord) * 0.20;
                color += texture2D(sTexture, vTexCoord + vec2(uBlurOffset.x * 1.5, 0.0)) * 0.12;
                color += texture2D(sTexture, vTexCoord + vec2(-uBlurOffset.x * 1.5, uBlurOffset.y * 1.5)) * 0.08;
                color += texture2D(sTexture, vTexCoord + vec2(0.0, uBlurOffset.y * 1.5)) * 0.12;
                color += texture2D(sTexture, vTexCoord + vec2(uBlurOffset.x * 1.5, uBlurOffset.y * 1.5)) * 0.08;
                gl_FragColor = vec4(color.rgb * 0.82, 1.0);
            } else {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        }
    """.trimIndent()

    fun initialize(): Surface? {
        synchronized(lock) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return null

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return null

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                return null
            }

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return null

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surfaceHolder.surface, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return null

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return null

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            program = createProgram(vertexShaderSource, fragmentShaderSource)
            if (program == 0) return null

            aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            aTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
            sTextureHandle = GLES20.glGetUniformLocation(program, "sTexture")
            uModeHandle = GLES20.glGetUniformLocation(program, "uMode")
            uBlurOffsetHandle = GLES20.glGetUniformLocation(program, "uBlurOffset")

            surfaceTexture = SurfaceTexture(textureId).apply {
                setOnFrameAvailableListener {
                    onFrameRendered()
                }
            }
            inputSurface = Surface(surfaceTexture)
            return inputSurface
        }
    }

    fun updateDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            surfaceWidth = width
            surfaceHeight = height
        }
    }

    fun updateVideoSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            videoWidth = width
            videoHeight = height
        }
    }

    fun render(fitMode: WallpaperFitMode) {
        synchronized(lock) {
            val st = surfaceTexture ?: return
            if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE || eglContext == EGL14.EGL_NO_CONTEXT) return
            if (!surfaceHolder.surface.isValid) return

            try {
                if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return
                st.updateTexImage()
                st.getTransformMatrix(stMatrix)

                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

                GLES20.glUseProgram(program)
                GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)
                GLES20.glUniform1i(sTextureHandle, 0)

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

                val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.coerceAtLeast(1).toFloat()
                val videoAspect = videoWidth.toFloat() / videoHeight.coerceAtLeast(1).toFloat()

                when (fitMode) {
                    WallpaperFitMode.BLUR -> {
                        val bgScaleX: Float
                        val bgScaleY: Float
                        if (videoAspect > surfaceAspect) {
                            bgScaleX = videoAspect / surfaceAspect
                            bgScaleY = 1.0f
                        } else {
                            bgScaleX = 1.0f
                            bgScaleY = surfaceAspect / videoAspect
                        }

                        GLES20.glUniform1i(uModeHandle, 1)
                        GLES20.glUniform2f(
                            uBlurOffsetHandle,
                            12.0f / surfaceWidth.coerceAtLeast(1).toFloat(),
                            12.0f / surfaceHeight.coerceAtLeast(1).toFloat()
                        )
                        drawQuad(bgScaleX, bgScaleY)

                        val fgScaleX: Float
                        val fgScaleY: Float
                        if (videoAspect > surfaceAspect) {
                            fgScaleX = 1.0f
                            fgScaleY = surfaceAspect / videoAspect
                        } else {
                            fgScaleX = videoAspect / surfaceAspect
                            fgScaleY = 1.0f
                        }

                        GLES20.glUniform1i(uModeHandle, 0)
                        drawQuad(fgScaleX, fgScaleY)
                    }

                    WallpaperFitMode.CROP -> {
                        val scaleX: Float
                        val scaleY: Float
                        if (videoAspect > surfaceAspect) {
                            scaleX = videoAspect / surfaceAspect
                            scaleY = 1.0f
                        } else {
                            scaleX = 1.0f
                            scaleY = surfaceAspect / videoAspect
                        }
                        GLES20.glUniform1i(uModeHandle, 0)
                        drawQuad(scaleX, scaleY)
                    }

                    WallpaperFitMode.FIT -> {
                        val scaleX: Float
                        val scaleY: Float
                        if (videoAspect > surfaceAspect) {
                            scaleX = 1.0f
                            scaleY = surfaceAspect / videoAspect
                        } else {
                            scaleX = videoAspect / surfaceAspect
                            scaleY = 1.0f
                        }
                        GLES20.glUniform1i(uModeHandle, 0)
                        drawQuad(scaleX, scaleY)
                    }
                }

                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
            } catch (_: Exception) {
            }
        }
    }

    private fun drawQuad(scaleX: Float, scaleY: Float) {
        quadCoords[0] = -scaleX
        quadCoords[1] = -scaleY
        quadCoords[2] = scaleX
        quadCoords[3] = -scaleY
        quadCoords[4] = -scaleX
        quadCoords[5] = scaleY
        quadCoords[6] = scaleX
        quadCoords[7] = scaleY

        vertexBuffer.position(0)
        vertexBuffer.put(quadCoords)
        vertexBuffer.position(0)

        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        val prog = GLES20.glCreateProgram()
        if (prog != 0) {
            GLES20.glAttachShader(prog, vertexShader)
            GLES20.glAttachShader(prog, fragmentShader)
            GLES20.glLinkProgram(prog)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(prog)
                return 0
            }
        }
        return prog
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader)
                return 0
            }
        }
        return shader
    }

    fun release() {
        synchronized(lock) {
            try {
                inputSurface?.release()
                inputSurface = null
                surfaceTexture?.release()
                surfaceTexture = null

                if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
                    try {
                        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                    } catch (_: Exception) {
                    }
                }

                if (textureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                    textureId = 0
                }
                if (program != 0) {
                    GLES20.glDeleteProgram(program)
                    program = 0
                }

                if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(eglDisplay, eglSurface)
                        eglSurface = EGL14.EGL_NO_SURFACE
                    }
                    if (eglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                        eglContext = EGL14.EGL_NO_CONTEXT
                    }
                    eglDisplay = EGL14.EGL_NO_DISPLAY
                }
            } catch (_: Exception) {
            }
        }
    }
}
