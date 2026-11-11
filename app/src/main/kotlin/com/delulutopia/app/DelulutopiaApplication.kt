package com.delulutopia.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/** Pictures load through the app's OkHttp client, so avatars and photos carry the session and entry cookies. */
@HiltAndroidApp
class DelulutopiaApplication :
    Application(),
    SingletonImageLoader.Factory {

    @Inject lateinit var okHttp: Lazy<OkHttpClient>

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttp.get() })) }
        .build()
}
