package com.skateboard.cameralib.util

enum class GlError private constructor(internal var code: Int, internal var msg: String)
{

    OK(0, "ok"),
    ConfigErr(101, "config not support");

    fun value(): Int
    {
        return code
    }

    override fun toString(): String
    {
        return msg
    }
}