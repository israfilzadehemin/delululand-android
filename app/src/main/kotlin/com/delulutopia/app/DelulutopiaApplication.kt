package com.delulutopia.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.ui.HttpOwner
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/** Pictures load through the app's OkHttp client, so avatars and photos carry the session and entry cookies; stamps are SVG. */
@HiltAndroidApp
class DelulutopiaApplication :
    Application(),
    SingletonImageLoader.Factory,
    HttpOwner {

    @Inject lateinit var okHttp: Lazy<OkHttpClient>

    override val httpClient: OkHttpClient get() = okHttp.get()

    override fun onCreate() {
        super.onCreate()
        // The remembered language (or the phone's) before the first screen draws (L6).
        I18n.init(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttp.get() }))
            add(SvgDecoder.Factory())
        }
        .build()
}
