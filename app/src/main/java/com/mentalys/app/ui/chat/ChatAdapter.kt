package com.mentalys.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mentalys.app.databinding.ItemChatMessageBinding
import io.noties.markwon.Markwon

data class ChatMessage(
    val userText: String? = null,
    val aiText: String? = null,
    val isLoading: Boolean = false
)

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatMessage>()

    fun addWelcome(text: String) {
        items.add(ChatMessage(aiText = text))
        notifyItemInserted(items.lastIndex)
    }

    fun addUser(text: String) {
        items.add(ChatMessage(userText = text))
        notifyItemInserted(items.lastIndex)
    }

    fun addAiLoading(): Int {
        items.add(ChatMessage(isLoading = true))
        val idx = items.lastIndex
        notifyItemInserted(idx)
        return idx
    }

    fun finishAiLoading(index: Int, text: String) {
        if (index < items.size) {
            items[index] = ChatMessage(aiText = text)
            notifyItemChanged(index)
        }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(private val b: ItemChatMessageBinding) : RecyclerView.ViewHolder(b.root) {
        private val markwon = Markwon.create(b.root.context)

        fun bind(msg: ChatMessage) {
            b.cardUser.visibility = View.GONE
            b.cardAi.visibility = View.GONE

            when {
                msg.userText != null -> {
                    b.cardUser.visibility = View.VISIBLE
                    b.tvUserText.text = msg.userText
                }
                msg.isLoading -> {
                    b.cardAi.visibility = View.VISIBLE
                    b.progressAi.visibility = View.VISIBLE
                    b.tvAiText.visibility = View.GONE
                }
                msg.aiText != null -> {
                    b.cardAi.visibility = View.VISIBLE
                    b.progressAi.visibility = View.GONE
                    b.tvAiText.visibility = View.VISIBLE
                    markwon.setMarkdown(b.tvAiText, msg.aiText)
                }
            }
        }
    }
}
