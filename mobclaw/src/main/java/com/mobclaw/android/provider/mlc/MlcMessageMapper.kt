package com.mobclaw.android.provider.mlc

import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionRole
import com.mobclaw.android.model.ChatMessage

internal object MlcMessageMapper {

    fun toMlcMessages(messages: List<ChatMessage>): List<ChatCompletionMessage> {
        return messages.map { msg ->
            val role = when (msg.role) {
                "system" -> ChatCompletionRole.system
                "assistant" -> ChatCompletionRole.assistant
                "tool" -> ChatCompletionRole.tool
                else -> ChatCompletionRole.user
            }
            ChatCompletionMessage(role = role, content = msg.content)
        }
    }
}
