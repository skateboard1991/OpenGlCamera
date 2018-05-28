package com.skateboard.jni

object DataConvert
{

    val RGBA_YUV420SP = 0x00004012
    val BGRA_YUV420SP = 0x00004210
    val RGBA_YUV420P = 0x00014012
    val BGRA_YUV420P = 0x00014210
    val RGB_YUV420SP = 0x00003012
    val RGB_YUV420P = 0x00013012
    val BGR_YUV420SP = 0x00003210
    val BGR_YUV420P = 0x00013210

    external fun rgbaToYuv(rgba: ByteArray, width: Int, height: Int, yuv: ByteArray, type: Int)


    init
    {
        System.loadLibrary("VideoConvert")
    }

}
