import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import type { RootStackParamList } from "../navigation/AppNavigator";

type Props = NativeStackScreenProps<RootStackParamList, "ChatStream">;

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
}

export function ChatStreamScreen(_props: Props): React.JSX.Element {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);

  const handleSend = () => {
    if (!input.trim() || isSending) return;

    const userMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      role: "user",
      content: input.trim(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsSending(true);

    // TODO: Integrate SSE streaming via Gateway BFF proxy
    setTimeout(() => {
      const assistantMsg: ChatMessage = {
        id: `msg-${Date.now()}-reply`,
        role: "assistant",
        content: "SSE streaming integration pending — connect to Gateway BFF proxy.",
      };
      setMessages((prev) => [...prev, assistantMsg]);
      setIsSending(false);
    }, 1000);
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
      keyboardVerticalOffset={Platform.OS === "ios" ? 90 : 0}
    >
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Orasaka Chat</Text>
      </View>

      <FlatList
        data={messages}
        keyExtractor={(item) => item.id}
        style={styles.messageList}
        contentContainerStyle={styles.messageContent}
        renderItem={({ item }) => (
          <View
            style={[
              styles.messageBubble,
              item.role === "user" ? styles.userBubble : styles.assistantBubble,
            ]}
          >
            <Text style={styles.messageText}>{item.content}</Text>
          </View>
        )}
      />

      <View style={styles.inputBar}>
        <TextInput
          id="chat-input"
          style={styles.textInput}
          placeholder="Message Orasaka…"
          placeholderTextColor="hsl(220, 12%, 42%)"
          value={input}
          onChangeText={setInput}
          editable={!isSending}
          multiline
          maxLength={4000}
        />
        <TouchableOpacity
          id="chat-send"
          style={[styles.sendBtn, (isSending || !input.trim()) && styles.sendBtnDisabled]}
          onPress={handleSend}
          disabled={isSending || !input.trim()}
        >
          <Text style={styles.sendBtnText}>↑</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "hsl(220, 20%, 6%)",
  },
  header: {
    paddingTop: 60,
    paddingBottom: 16,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: "hsl(220, 14%, 18%)",
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: "700",
    color: "hsl(220, 20%, 92%)",
  },
  messageList: {
    flex: 1,
  },
  messageContent: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  messageBubble: {
    maxWidth: "80%",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 16,
    marginBottom: 8,
  },
  userBubble: {
    alignSelf: "flex-end",
    backgroundColor: "hsl(200, 90%, 55%)",
  },
  assistantBubble: {
    alignSelf: "flex-start",
    backgroundColor: "hsl(220, 16%, 14%)",
  },
  messageText: {
    fontSize: 15,
    color: "hsl(220, 20%, 92%)",
    lineHeight: 21,
  },
  inputBar: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderTopWidth: 1,
    borderTopColor: "hsl(220, 14%, 18%)",
    backgroundColor: "hsl(220, 16%, 10%)",
  },
  textInput: {
    flex: 1,
    backgroundColor: "hsl(220, 20%, 6%)",
    borderWidth: 1,
    borderColor: "hsl(220, 14%, 18%)",
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 15,
    color: "hsl(220, 20%, 92%)",
    maxHeight: 100,
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: "hsl(200, 90%, 55%)",
    alignItems: "center",
    justifyContent: "center",
    marginLeft: 8,
  },
  sendBtnDisabled: {
    opacity: 0.4,
  },
  sendBtnText: {
    color: "hsl(220, 20%, 6%)",
    fontSize: 20,
    fontWeight: "700",
  },
});
