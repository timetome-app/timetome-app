package me.timeto.shared.vm

import kotlinx.coroutines.flow.*
import me.timeto.shared.db.ShortcutDb
import me.timeto.shared.launchEx
import me.timeto.shared.launchExIo
import me.timeto.shared.misc.DialogsManager
import me.timeto.shared.misc.UiException

class ShortcutFormVm(
    shortcutDb: ShortcutDb?,
) : __Vm<ShortcutFormVm.State>() {

    data class State(
        val shortcutDb: ShortcutDb?,
        val name: String,
        val uri: String,
    ) {

        val title: String = if (shortcutDb != null) "Edit Shortcut" else "New Shortcut"
        val saveText: String = if (shortcutDb != null) "Save" else "Create"
        val isSaveEnabled = (name.isNotBlank() && uri.isNotBlank())

        val nameHeader = "SHORTCUT NAME"
        val namePlaceholder = "Name"

        val uriHeader = "SHORTCUT LINK"
        val uriPlaceholder = "Link"
    }

    override val state = MutableStateFlow(
        State(
            shortcutDb = shortcutDb,
            name = shortcutDb?.name ?: "",
            uri = shortcutDb?.uri ?: "",
        )
    )

    fun setName(name: String) {
        state.update { it.copy(name = name) }
    }

    fun setUri(uri: String) {
        state.update { it.copy(uri = uri) }
    }

    fun setAndroidPackage(androidPackage: String) {
        state.update {
            it.copy(uri = "${ShortcutDb.ANDROID_PACKAGE_PREFIX}$androidPackage")
        }
    }

    fun save(
        dialogsManager: DialogsManager,
        onSuccess: (ShortcutDb) -> Unit,
    ): Unit = scopeVm().launchEx {
        try {
            val name: String = state.value.name
            val uri: String = state.value.name
            val oldShortcutDb: ShortcutDb? = state.value.shortcutDb
            val newShortcutDb: ShortcutDb = if (oldShortcutDb != null)
                oldShortcutDb.updateWithValidation(name = name, uri = uri)
            else
                ShortcutDb.insertWithValidation(name = name, uri = uri)
            onUi { onSuccess(newShortcutDb) }
        } catch (e: UiException) {
            dialogsManager.alert(e.uiMessage)
        }
    }
}
