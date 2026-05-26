/**
 * @file AppNavigator.tsx
 * @description Root navigation stack for the Orasaka mobile client.
 * Orchestrates 6 screens: Auth flow (Login, Register, ForgotPassword, ResetPassword),
 * ChatStream (SSE multi-modal), and Subscription (Premium/Free).
 */

import React from "react";
import { NavigationContainer, DefaultTheme } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";

import { LoginScreen } from "../screens/LoginScreen";
import { RegisterScreen } from "../screens/RegisterScreen";
import { ForgotPasswordScreen } from "../screens/ForgotPasswordScreen";
import { ResetPasswordScreen } from "../screens/ResetPasswordScreen";
import { ChatStreamScreen } from "../screens/ChatStreamScreen";
import { SubscriptionScreen } from "../screens/SubscriptionScreen";

/**
 * Navigation parameter list — typed route params for all 6 screens.
 */
export type RootStackParamList = {
  Login: undefined;
  Register: undefined;
  ForgotPassword: undefined;
  ResetPassword: { token: string };
  ChatStream: undefined;
  Subscription: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

/** Cinematic dark theme matching the Orasaka design system. */
const OrasakaDarkTheme = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    primary: "hsl(200, 90%, 55%)",
    background: "hsl(220, 20%, 6%)",
    card: "hsl(220, 16%, 10%)",
    text: "hsl(220, 20%, 92%)",
    border: "hsl(220, 14%, 18%)",
    notification: "hsl(200, 90%, 55%)",
  },
};

export function AppNavigator(): React.JSX.Element {
  return (
    <NavigationContainer theme={OrasakaDarkTheme}>
      <Stack.Navigator
        initialRouteName="Login"
        screenOptions={{
          headerShown: false,
          animation: "slide_from_right",
          contentStyle: { backgroundColor: "hsl(220, 20%, 6%)" },
        }}
      >
        {/* Auth Flow */}
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Register" component={RegisterScreen} />
        <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
        <Stack.Screen name="ResetPassword" component={ResetPasswordScreen} />

        {/* Core Screens */}
        <Stack.Screen name="ChatStream" component={ChatStreamScreen} />
        <Stack.Screen name="Subscription" component={SubscriptionScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
