package com.delulutopia.feature.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.translate
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * You › Language (L8; web settings/language): each language named in its own words. A pick applies at
 * once and is saved to the account, so every device follows; if the server refuses, the old one returns.
 */
@Composable
fun LanguageScreen(vm: LanguageViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val current = I18n.lang
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = t("settings.language"), onBack = { nav.back() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Lang.entries.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = lang == current, role = Role.RadioButton, onClick = { vm.pick(lang, notice) })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Radio(lang == current)
                        Text(lang.native, Modifier.weight(1f), fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            SubNote(t("settings.languageNote"))
        }
    }
}

@HiltViewModel
class LanguageViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    fun pick(lang: Lang, notice: (String) -> Unit) {
        val before = I18n.lang
        if (lang == before) return
        I18n.set(lang)
        viewModelScope.launch {
            try {
                account.setLanguage(lang)
            } catch (e: ApiException) {
                I18n.set(before)
                notice(translate(before, "settings.languageNotSaved"))
            }
        }
    }
}
