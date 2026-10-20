package com.github.furqanhun.letmeread

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.models.member.GuildMember
import com.discord.utilities.color.ColorCompat
import com.discord.views.UsernameView
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import java.lang.ref.WeakReference
import kotlin.jvm.functions.Function0

@AliucordPlugin
class LetMeRead : Plugin() {

    private var memberNameId = 0
    private var memberPrimaryAttr = 0

    init {
        settingsTab = SettingsTab(
            PluginSettings::class.java,
            SettingsTab.Type.BOTTOM_SHEET,
        ).withArgs(settings)
    }

    override fun start(context: Context) {
        memberNameId = Utils.getResId("channel_members_list_item_name", "id")
        memberPrimaryAttr = Utils.getResId("primary_000", "attr")
        patchChatMessages()
        patchChatMessageText()
        patchWidgetChatList()
        patchMemberList()
        patchThemeChanges()
    }

    private fun patchChatMessages() {
        try {
            val method = WidgetChatListAdapterItemMessage::class.java.getDeclaredMethod(
                "getAuthorTextColor",
                GuildMember::class.java,
            )

            patcher.patch(
                method,
                Hook { callFrame ->
                    val holder = callFrame.thisObject as? WidgetChatListAdapterItemMessage ?: return@Hook
                    if (chatAdapterRef?.get() == null) {
                        chatAdapterRef = WeakReference(holder.adapter)
                        chatRecyclerRef = WeakReference(holder.adapter.recycler)
                    }

                    if (!settings.getBool(PluginSettings.KEY_CHAT_ENABLED, true)) return@Hook

                    val member = callFrame.args[0] as? GuildMember ?: return@Hook
                    val rawColor = callFrame.result as? Int ?: return@Hook
                    if (member.color == 0) return@Hook

                    val itemView = holder.itemView
                    val bgColor = ThemeBackgroundResolver.getChatBackgroundColor(itemView)
                    val minRatio = settings.getFloat(PluginSettings.KEY_CONTRAST_RATIO, 3.5f).toDouble()
                    val mode = settings.getInt(PluginSettings.KEY_MODE, ColorContrastEngine.MODE_LIGHTNESS_SHIFT)

                    callFrame.result = ColorContrastEngine.ensureContrast(rawColor, bgColor, minRatio, mode)
                },
            )
        } catch (error: Throwable) {
            logger.error("Failed to patch WidgetChatListAdapterItemMessage", error)
        }
    }

    private fun patchChatMessageText() {
        try {
            val method = WidgetChatListAdapterItemMessage::class.java.declaredMethods.firstOrNull {
                it.name == "processMessageText" && it.parameterTypes.size == 2
            } ?: return

            val textTagKey = Utils.getResId("chat_list_adapter_item_text", "id")
            val replyTagKey = Utils.getResId("chat_list_adapter_item_text_reply_content", "id")

            patcher.patch(
                method,
                Hook { callFrame ->
                    val textView = callFrame.args[0] as? TextView ?: return@Hook
                    val holder = callFrame.thisObject as? WidgetChatListAdapterItemMessage ?: return@Hook

                    chatAdapterRef = WeakReference(holder.adapter)
                    chatRecyclerRef = WeakReference(holder.adapter.recycler)

                    val originalColor = if (textTagKey != 0) {
                        (textView.getTag(textTagKey) as? Int) ?: textView.currentTextColor.also {
                            textView.setTag(textTagKey, it)
                        }
                    } else {
                        textView.currentTextColor
                    }

                    val replyView = if (replyTagKey != 0) holder.itemView.findViewById<TextView>(replyTagKey) else null
                    val originalReplyColor = if (replyView != null && replyTagKey != 0) {
                        (replyView.getTag(replyTagKey) as? Int) ?: replyView.currentTextColor.also {
                            replyView.setTag(replyTagKey, it)
                        }
                    } else {
                        replyView?.currentTextColor
                    }

                    if (!settings.getBool(PluginSettings.KEY_CHAT_TEXT_ENABLED, false)) {
                        textView.setTextColor(originalColor)
                        if (originalReplyColor != null) replyView?.setTextColor(originalReplyColor)
                        return@Hook
                    }

                    val bgColor = ThemeBackgroundResolver.getChatOverallBackgroundColor(textView.context)
                    val minRatio = settings.getFloat(PluginSettings.KEY_CONTRAST_RATIO, 3.5f).toDouble()

                    val targetColor = ColorContrastEngine.ensureTextContrast(originalColor, bgColor, minRatio)
                    textView.setTextColor(targetColor)
                    replyView?.setTextColor(targetColor)
                },
            )
        } catch (error: Throwable) {
            logger.error("Failed to patch chat message text", error)
        }
    }

    private fun patchWidgetChatList() {
        try {
            val chatListClass = Class.forName("com.discord.widgets.chat.list.WidgetChatList")
            val method = chatListClass.declaredMethods.firstOrNull {
                it.name == "onViewBoundOrOnResume"
            } ?: return

            patcher.patch(
                method,
                Hook { callFrame ->
                    val chatList = callFrame.thisObject ?: return@Hook
                    val field = chatListClass.getDeclaredField("adapter").apply { isAccessible = true }
                    val adapter = field.get(chatList) as? RecyclerView.Adapter<*> ?: return@Hook
                    chatAdapterRef = WeakReference(adapter)
                    adapter.notifyDataSetChanged()
                },
            )
        } catch (error: Throwable) {
            logger.error("Failed to patch WidgetChatList", error)
        }
    }

    private fun patchMemberList() {
        try {
            val method = ChannelMembersListViewHolderMember::class.java.getDeclaredMethod(
                "bind",
                ChannelMembersListAdapter.Item.Member::class.java,
                Function0::class.java,
            )

            patcher.patch(
                method,
                Hook { callFrame ->
                    val holder = callFrame.thisObject as? ChannelMembersListViewHolderMember ?: return@Hook
                    val itemView = holder.itemView

                    if (memberListAdapterRef?.get() == null) {
                        (itemView.parent as? RecyclerView)?.let { recycler ->
                            memberListAdapterRef = WeakReference(recycler.adapter)
                            memberListRecyclerRef = WeakReference(recycler)
                        }
                    }

                    if (!settings.getBool(PluginSettings.KEY_MEMBER_LIST_ENABLED, true)) return@Hook

                    val member = callFrame.args[0] as? ChannelMembersListAdapter.Item.Member ?: return@Hook
                    val usernameView = getUsernameView(itemView) ?: return@Hook

                    val rawColor = member.color ?: (
                        if (memberPrimaryAttr != 0) ColorCompat.getThemedColor(usernameView, memberPrimaryAttr) else 0
                    )
                    if (rawColor == 0) return@Hook

                    val bgColor = ThemeBackgroundResolver.getMemberSidebarBackgroundColor(itemView)
                    val minRatio = settings.getFloat(PluginSettings.KEY_CONTRAST_RATIO, 3.5f).toDouble()
                    val mode = settings.getInt(PluginSettings.KEY_MODE, ColorContrastEngine.MODE_LIGHTNESS_SHIFT)

                    usernameView.setUsernameColor(
                        ColorContrastEngine.ensureContrast(rawColor, bgColor, minRatio, mode),
                    )
                },
            )
        } catch (error: Throwable) {
            logger.error("Failed to patch ChannelMembersListViewHolderMember", error)
        }
    }

    private fun getUsernameView(itemView: View): UsernameView? {
        if (memberNameId != 0) {
            (itemView.getTag(memberNameId) as? UsernameView)?.let { return it }
            val view = itemView.findViewById<UsernameView>(memberNameId)
            if (view != null) {
                itemView.setTag(memberNameId, view)
                return view
            }
        }
        return findUsernameView(itemView)?.also {
            if (memberNameId != 0) itemView.setTag(memberNameId, it)
        }
    }

    private fun findUsernameView(view: View): UsernameView? {
        if (view is UsernameView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findUsernameView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun patchThemeChanges() {
        try {
            val storeClass = Class.forName("com.discord.stores.StoreUserSettingsSystem")
            for (method in storeClass.declaredMethods) {
                if (method.name == "setThemeInternal" || method.name == "setTheme") {
                    patcher.patch(
                        method,
                        Hook {
                            refreshChat()
                        },
                    )
                }
            }
        } catch (_: Throwable) {}

        try {
            val loaderClass = Class.forName("dev.vendicated.aliucordplugs.themer.ThemeLoader")
            for (method in loaderClass.declaredMethods) {
                if (method.name == "loadTheme" || method.name == "loadThemes") {
                    patcher.patch(
                        method,
                        Hook {
                            refreshChat()
                        },
                    )
                }
            }
        } catch (_: Throwable) {}

        try {
            val rmClass = Class.forName("dev.vendicated.aliucordplugs.themer.ResourceManager")
            for (method in rmClass.declaredMethods) {
                if (method.name == "clean") {
                    patcher.patch(
                        method,
                        Hook {
                            refreshChat()
                        },
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        chatAdapterRef = null
        chatRecyclerRef = null
        memberListAdapterRef = null
        memberListRecyclerRef = null
        ThemeBackgroundResolver.invalidate()
        ColorContrastEngine.clearCache()
    }

    companion object {
        var chatAdapterRef: WeakReference<RecyclerView.Adapter<*>>? = null
        var chatRecyclerRef: WeakReference<RecyclerView>? = null
        var memberListAdapterRef: WeakReference<RecyclerView.Adapter<*>>? = null
        var memberListRecyclerRef: WeakReference<RecyclerView>? = null

        fun refreshChat() {
            ThemeBackgroundResolver.invalidate()
            ColorContrastEngine.clearCache()
            val adapter = chatAdapterRef?.get()
            val recycler = chatRecyclerRef?.get()
            if (adapter != null) {
                if (recycler != null) {
                    recycler.post { adapter.notifyDataSetChanged() }
                } else {
                    adapter.notifyDataSetChanged()
                }
            }

            val memberAdapter = memberListAdapterRef?.get()
            val memberRecycler = memberListRecyclerRef?.get()
            if (memberAdapter != null) {
                if (memberRecycler != null) {
                    memberRecycler.post { memberAdapter.notifyDataSetChanged() }
                } else {
                    memberAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}
