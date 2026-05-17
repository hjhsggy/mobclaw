package com.mobclaw.android.provider.mlc

import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionRole
import com.mobclaw.android.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class MlcMessageMapperTest {

    @Test
    fun toMlcMessages_mapsRoles() {
        val messages = listOf(
            ChatMessage.system("sys"),
            ChatMessage.user("hi"),
            ChatMessage.assistant("ok"),
            ChatMessage.tool("result"),
        )

        val mapped = MlcMessageMapper.toMlcMessages(messages)

        assertEquals(4, mapped.size)
        assertEquals(ChatCompletionRole.system, mapped[0].role)
        assertEquals(ChatCompletionRole.user, mapped[1].role)
        assertEquals(ChatCompletionRole.assistant, mapped[2].role)
        assertEquals(ChatCompletionRole.tool, mapped[3].role)
        assertEquals("sys", mapped[0].content?.asText())
        assertEquals("hi", mapped[1].content?.asText())
    }
}
