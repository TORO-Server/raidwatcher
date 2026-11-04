package io.github.fhrk_78.raidwatcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatCommandsTest {
    @Test
    void tellCommandsSkipTheTarget() {
        assertEquals(new ChatCommands.Message("hello there", true), ChatCommands.parse("/tell Steve hello there"));
        assertEquals(new ChatCommands.Message("hi", true), ChatCommands.parse("/MSG  Steve   hi"));
        assertEquals(new ChatCommands.Message("hi", true), ChatCommands.parse("/minecraft:w Steve hi"));
    }

    @Test
    void replyCommandsHaveNoTarget() {
        assertEquals(new ChatCommands.Message("hello", true), ChatCommands.parse("/r hello"));
        assertEquals(new ChatCommands.Message("a b", true), ChatCommands.parse("/reply a b"));
    }

    @Test
    void channelCommandsAreCheckedButNotCountedAsMessages() {
        assertEquals(new ChatCommands.Message("hello", false), ChatCommands.parse("/ch general hello"));
        assertEquals(new ChatCommands.Message("hello", false), ChatCommands.parse("/lunachat join general hello"));
    }

    @Test
    void commandsWithoutTextAreIgnored() {
        assertNull(ChatCommands.parse("/tell Steve"));
        assertNull(ChatCommands.parse("/tell"));
        assertNull(ChatCommands.parse("/r"));
        assertNull(ChatCommands.parse("/ch general"));
        assertNull(ChatCommands.parse("/ch join general"));
    }

    @Test
    void otherCommandsAreIgnored() {
        assertNull(ChatCommands.parse("/home bed"));
        assertNull(ChatCommands.parse("/raidwatcher tell Steve hi"));
        // WorldEdit のコマンドは「//」から始まる
        assertNull(ChatCommands.parse("//r 5 stone"));
        assertNull(ChatCommands.parse("/"));
    }
}
