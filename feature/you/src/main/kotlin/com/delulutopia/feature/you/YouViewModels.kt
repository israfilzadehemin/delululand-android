package com.delulutopia.feature.you

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DevicePrefs
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.NotifyChange
import com.delulutopia.core.network.model.NotifySettings
import com.delulutopia.core.network.model.Profile
import com.delulutopia.core.network.model.ProfileBody
import com.delulutopia.core.network.model.UniverseUpdate
import com.delulutopia.core.network.model.UniverseView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The You tab (doc "Delululand — You Tab & New Buttons" §4) and Appearance. */
@HiltViewModel
class YouViewModel @Inject constructor(
    private val backend: Backend,
    home: HomeRepository,
    private val prefs: DevicePrefs,
) : ViewModel() {
    val home: StateFlow<HomeRepository.State> = home.state
    val leaves: StateFlow<Boolean> = prefs.leaves
    var notify by mutableStateOf<NotifySettings?>(null)
        private set

    fun load() {
        viewModelScope.launch { runCatching { backend.run { notifications() } }.onSuccess { notify = it } }
    }

    fun setLeaves(on: Boolean) = prefs.setLeaves(on)
}

/** Notifications S3–S7: each switch saves at once, optimistically. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(private val backend: Backend, private val home: HomeRepository) : ViewModel() {
    val homeState: StateFlow<HomeRepository.State> = home.state
    var settings by mutableStateOf<NotifySettings?>(null)
        private set
    var failed by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                settings = backend.run { notifications() }
                failed = false
            } catch (_: ApiException) {
                failed = settings == null
            }
        }
    }

    fun change(paused: Boolean? = null, values: Map<String, Boolean>? = null, onFail: () -> Unit) {
        val before = settings ?: return
        settings = applyPatch(before, paused, values.orEmpty())
        viewModelScope.launch {
            try {
                settings = backend.run { saveNotifications(NotifyChange(paused, values)) }
                home.refreshSoon()
            } catch (_: ApiException) {
                settings = before
                onFail()
            }
        }
    }
}

/** The optimistic copy of a settings change (S6). */
fun applyPatch(s: NotifySettings, paused: Boolean?, values: Map<String, Boolean>): NotifySettings = NotifySettings(
    paused = paused ?: s.paused,
    sections = s.sections.map { sec ->
        sec.copy(enabled = values["section.${sec.key}"] ?: sec.enabled, events = sec.events.map { e -> e.copy(enabled = values[e.key] ?: e.enabled) })
    },
)

/** Profile (P1–P7): the three fields and the zone save together; the photo on its own. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val backend: Backend,
    private val account: AccountRepository,
    private val home: HomeRepository,
    private val session: SessionRepository,
) : ViewModel() {
    val homeState: StateFlow<HomeRepository.State> = home.state
    var profile by mutableStateOf<Profile?>(null)
        private set
    var failed by mutableStateOf(false)
        private set
    var form by mutableStateOf(ProfileForm())
    var saved by mutableStateOf(ProfileForm())
        private set
    var serverError by mutableStateOf<Map<String, String>>(emptyMap())
    var busy by mutableStateOf(false)
        private set
    var photoBusy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                show(backend.run { profile() })
            } catch (_: ApiException) {
                failed = true
            }
        }
    }

    private fun show(p: Profile) {
        profile = p
        form = ProfileForm.of(p)
        saved = form
    }

    fun set(field: String, value: String) {
        form = form.with(field, value)
        serverError = serverError - field
    }

    private fun refresh(p: Profile) {
        profile = p
        viewModelScope.launch {
            home.load()
            session.refresh()
        }
    }

    fun save(onDone: (String?) -> Unit) {
        busy = true
        viewModelScope.launch {
            try {
                val f = form
                val p = backend.run { saveProfile(ProfileBody(f.username, f.firstName.trim(), f.lastName.trim(), f.timezone)) }
                refresh(p)
                show(p)
                onDone(t("profile.saved"))
            } catch (e: ApiException) {
                when (e.code) {
                    "USERNAME_TAKEN" -> serverError = serverError + ("username" to t("profile.usernameTaken"))
                    "USERNAME_INVALID" -> serverError = serverError + ("username" to t("profile.316LettersOrDigits"))
                    "TIMEZONE_INVALID" -> serverError = serverError + ("timezone" to t("profile.pickTimeZone"))
                    "NAME_INVALID" -> serverError = serverError + ("lastName" to t("profile.nameRule"))
                    else -> onDone(t("common.couldntSave"))
                }
            } finally {
                busy = false
            }
        }
    }

    fun upload(jpeg: ByteArray, onFail: (String) -> Unit) {
        photoBusy = true
        viewModelScope.launch {
            try {
                refresh(account.uploadAvatar(jpeg))
            } catch (e: ApiException) {
                onFail(if (!e.isOffline) e.message else t("profile.photoNotUpdated"))
            } finally {
                photoBusy = false
            }
        }
    }

    fun remove(onFail: (String) -> Unit) {
        photoBusy = true
        viewModelScope.launch {
            try {
                refresh(account.removeAvatar())
            } catch (_: ApiException) {
                onFail(t("profile.photoNotRemoved"))
            } finally {
                photoBusy = false
            }
        }
    }
}

/** Settings › Universe (doc "Delululand — Universe" §2.6). */
@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val backend: Backend,
    private val home: HomeRepository,
    private val session: SessionRepository,
) : ViewModel() {
    val homeState: StateFlow<HomeRepository.State> = home.state
    var view by mutableStateOf<UniverseView?>(null)
        private set
    var failed by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
    var welcome by mutableStateOf("")
    var savedName by mutableStateOf("")
        private set
    var savedWelcome by mutableStateOf("")
        private set
    var serverError by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set
    var revealed by mutableStateOf<String?>(null)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                show(backend.run { universe() })
            } catch (_: ApiException) {
                failed = view == null
            }
        }
    }

    private fun show(v: UniverseView) {
        view = v
        name = v.name
        welcome = v.welcome
        savedName = v.name
        savedWelcome = v.welcome
    }

    fun save(notice: (String) -> Unit) {
        busy = true
        viewModelScope.launch {
            try {
                show(backend.run { updateUniverse(UniverseUpdate(name.trim(), welcome)) })
                home.load()
                session.refresh()
                notice(t("settings.universeSaved"))
            } catch (e: ApiException) {
                if (e.code == "UNIVERSE_NAME_INVALID") serverError = e.message else notice(if (e.status in 1..499) e.message else t("common.couldntSave"))
            } finally {
                busy = false
            }
        }
    }

    fun rotate(notice: (String) -> Unit) {
        viewModelScope.launch {
            try {
                revealed = backend.run { rotateKey() }.code
                runCatching { view = backend.run { universe() } }
            } catch (_: ApiException) {
                notice(t("settings.keyNotMade"))
            }
        }
    }
}

/** Change PIN (FS-A17): current, new, confirm; one keypad. */
@HiltViewModel
class ChangePinViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    var step by mutableStateOf(0)
        private set
    var values by mutableStateOf(listOf("", "", ""))
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var changed by mutableStateOf(false)
        private set

    fun reset() {
        step = 0
        values = listOf("", "", "")
        error = null
        busy = false
        changed = false
    }

    fun digits(d: String) {
        values = values.mapIndexed { i, v -> if (i == step) d else v }
        if (error != null && d.isNotEmpty()) error = null
    }

    fun complete(pin: String, brand: String, onChanged: (String) -> Unit) {
        error = null
        when (step) {
            0 -> step = 1
            1 -> step = 2
            else -> if (pin != values[1]) {
                values = listOf(values[0], "", "")
                step = 1
                error = t("common.pinsDiffer")
            } else {
                submit(values[0], pin, brand, onChanged)
            }
        }
    }

    private fun submit(current: String, next: String, brand: String, onChanged: (String) -> Unit) {
        busy = true
        viewModelScope.launch {
            try {
                account.changePin(current, next)
                changed = true
                onChanged(next)
                delay(1_200)
            } catch (e: ApiException) {
                values = listOf("", "", "")
                step = 0
                error = when {
                    e.code == "WRONG_PIN" || e.code == "PIN_THROTTLED" -> t("common.wrongPin")
                    // A refusal with its own words (a malformed PIN) is not "can't reach".
                    !e.isOffline -> e.message
                    else -> t("common.cantReachTryAgain", "brand" to brand)
                }
            } finally {
                busy = false
            }
        }
    }
}

/** X1: the PIN behind the fingerprint is checked once (a login, as at the PIN screen) before it is kept. */
@HiltViewModel
class FingerprintPinViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    var digits by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    fun check(userId: Int, pin: String, brand: String, onOk: (String) -> Unit) {
        busy = true
        viewModelScope.launch {
            try {
                account.login(userId, pin)
                onOk(pin)
            } catch (e: ApiException) {
                digits = ""
                error = if (e.code == "WRONG_PIN" || e.code == "PIN_THROTTLED") t("common.wrongPin") else t("common.cantReachTryAgain", "brand" to brand)
            } finally {
                busy = false
            }
        }
    }
}
