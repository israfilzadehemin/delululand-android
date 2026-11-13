package com.delulutopia.core.ui

/** What the start flow leaves for the signed-in app to say once Home is up. */
object AfterStart {
    /** U9: "Couldn't add your photo. You can add it in Profile." */
    @Volatile var photoFailed: Boolean = false
}
