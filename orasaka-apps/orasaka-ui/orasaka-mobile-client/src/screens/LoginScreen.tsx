import React, { useState } from "react";
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import type { RootStackParamList } from "../navigation/AppNavigator";

type Props = NativeStackScreenProps<RootStackParamList, "Login">;

export function LoginScreen({ navigation }: Props): React.JSX.Element {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Orasaka</Text>
      <Text style={styles.subtitle}>Sign in to your account</Text>

      <TextInput
        id="login-email"
        style={styles.input}
        placeholder="Email"
        placeholderTextColor="hsl(220, 12%, 42%)"
        keyboardType="email-address"
        autoCapitalize="none"
        value={email}
        onChangeText={setEmail}
      />
      <TextInput
        id="login-password"
        style={styles.input}
        placeholder="Password"
        placeholderTextColor="hsl(220, 12%, 42%)"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />

      <TouchableOpacity id="login-submit" style={styles.primaryBtn}>
        <Text style={styles.primaryBtnText}>Sign In</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => navigation.navigate("ForgotPassword")}>
        <Text style={styles.link}>Forgot password?</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={() => navigation.navigate("Register")}>
        <Text style={styles.link}>Create an account</Text>
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
    fontSize: 32,
    fontWeight: "800",
    color: "hsl(200, 90%, 55%)",
    textAlign: "center",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 15,
    color: "hsl(220, 12%, 62%)",
    textAlign: "center",
    marginBottom: 32,
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
