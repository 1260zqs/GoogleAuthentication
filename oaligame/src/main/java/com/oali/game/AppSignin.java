package com.oali.game;


public class AppSignin {
    public static void GoogleSignIn(GoogleAuthentication.SignInRequest signInRequest) {
        new GoogleAuthentication().SignIn(signInRequest);
    }

    public static void GoogleSignOut(GoogleAuthentication.SignOutRequest signOutRequest) {
        new GoogleAuthentication().SignOut(signOutRequest);
    }

}
