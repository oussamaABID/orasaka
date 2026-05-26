import React, { useState } from "react";
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import type { RootStackParamList } from "../navigation/AppNavigator";

type Props = NativeStackScreenProps<RootStackParamList, "ResetPassword">;

export function ResetPasswordScreen({ route, navigation }: Props): React.JSX.Element {
  const { token } = route.params;
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  return (
    <View style={styles.container}>
      <Text style={styles.title}>New Password</Text>
      <Text style={styles.subtitle}>Enter your new password below</Text>
      <Text style={styles.tokenHint}>Token: {token.slice(0, 8)}…</Text>

      <TextInput
        id="reset-new-password"
        style={styles.input}
        placeholder="New password"
        placeholderTextColor="hsl(220, 12%, 42%)"
        secureTextEntry
        value={newPassword}
        onChangeText={setNewPassword}
      />
      <TextInput
        id="reset-confirm-password"
        style={styles.input}
        placeholder="Confirm password"
        placeholderTextColor="hsl(220, 12%, 42%)"
        secureTextEntry
        value={confirmPassword}
        onChangeText={setConfirmPassword}
      />

      <TouchableOpacity id="reset-submit" style={styles.primaryBtn}>
        <Text style={styles.primaryBtnText}>Reset Password</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => navigation.navigate("Login")}>
        <Text style={styles.link}>Back to Sign In</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 28,
    backgroundColor: "hsl(220, 20%, 6%)",
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    color: "hsl(220, 20%, 92%)",
    textAlign: "center",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 15,
    color: "hsl(220, 12%, 62%)",
    textAlign: "center",
    marginBottom: 8,
  },
  tokenHint: {
    fontSize: 12,
    color: "hsl(220, 12%, 42%)",
    textAlign: "center",
    marginBottom: 24,
    fontFamily: "monospace",
  },
  input: {
    backgroundColor: "hsl(220, 16%, 10%)",
    borderWidth: 1,
    borderColor: "hsl(220, 14%, 18%)",
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: "hsl(220, 20%, 92%)",
    marginBottom: 14,
  },
  primaryBtn: {
    backgroundColor: "hsl(200, 90%, 55%)",
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: "center",
    marginTop: 8,
    marginBottom: 20,
  },
  primaryBtnText: {
    color: "hsl(220, 20%, 6%)",
    fontSize: 16,
    fontWeight: "700",
  },
  link: {
    color: "hsl(200, 90%, 55%)",
    textAlign: "center",
    fontSize: 14,
    marginTop: 10,
  },
});
