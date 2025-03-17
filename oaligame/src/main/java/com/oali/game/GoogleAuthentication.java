package com.oali.game;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executors;

public class GoogleAuthentication {
    private SignInRequest signInRequest;
    private SignOutRequest signOutRequest;

    public interface SignInRequest {
        String getServerClientId();

        void onReply(String userDataJson);

        void onError(String exceptionMessage);
    }

    public interface SignOutRequest {
        void onReply();

        void onError(String exceptionMessage);
    }

    public GoogleAuthentication() {
    }

    public void SignIn(SignInRequest signInRequest) {
        this.signInRequest = signInRequest;
        String clientId = this.signInRequest.getServerClientId();
        if (clientId == null || clientId.isEmpty()) {
            this.signInRequest.onError("serverClientId is null or empty");
            return;
        }

        var nonce = CreateGoogleSignInRequestNonce();
        if (nonce == null || nonce.isEmpty()) {
            this.signInRequest.onError("Create google sign-in request nonce failed");
            return;
        }

        var googleIdOption = new GetSignInWithGoogleOption.Builder(clientId)
//                .setServerClientId(clientId)
//                .setFilterByAuthorizedAccounts(false)
//                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build();

        var request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();


        startGoogleSignIn(request);
    }

    private void startGoogleSignIn(GetCredentialRequest request) {
        var executor = Executors.newSingleThreadExecutor();
        var callback = new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                executor.shutdown();
                handleSignInResponse(result);
            }

            @Override
            public void onError(androidx.credentials.exceptions.GetCredentialException e) {
                executor.shutdown();
                handleFailure(e);
            }
        };

        Activity unityActivity = UnityPlayer.currentActivity;
        var credentialManager = CredentialManager.Companion.create(unityActivity);
        credentialManager.getCredentialAsync(unityActivity, request, null, executor, callback);
    }

    private void handleSignInResponse(GetCredentialResponse response) {
        var credential = response.getCredential();
        if (credential instanceof CustomCredential customCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
                try {
                    var jsonObject = new JSONObject();
                    jsonObject.put("id", googleCredential.getId());
                    jsonObject.put("token", googleCredential.getIdToken());
                    jsonObject.put("familyName", googleCredential.getFamilyName());
                    jsonObject.put("givenName", googleCredential.getGivenName());
                    jsonObject.put("displayName", googleCredential.getDisplayName());
                    this.signInRequest.onReply(jsonObject.toString());
                } catch (Exception e) {
                    this.signInRequest.onError(e.getMessage());
                }
            } else {
                this.signInRequest.onError("Unexpected credential type " + customCredential.getType());
            }
        } else {
            this.signInRequest.onError("Failed to sign-in with google");
        }
    }

    private void handleFailure(androidx.credentials.exceptions.GetCredentialException e) {
        this.signInRequest.onError(e.getMessage());
    }

    public void SignOut(SignOutRequest signOutRequest) {
        this.signOutRequest = signOutRequest;
        Activity unityActivity = UnityPlayer.currentActivity;
        var credentialManager = CredentialManager.Companion.create(unityActivity);
        var request = new ClearCredentialStateRequest();
        var executor = Executors.newSingleThreadExecutor();
        var callback = new CredentialManagerCallback<Void, ClearCredentialException>() {
            @Override
            public void onResult(Void unused) {
                executor.shutdown();
                handleSignOut();
            }

            @Override
            public void onError(@NonNull ClearCredentialException e) {
                executor.shutdown();
                handleSignOutFailure(e);
            }
        };
        credentialManager.clearCredentialStateAsync(request, null, executor, callback);
    }

    private void handleSignOutFailure(ClearCredentialException e) {
        this.signOutRequest.onError(e.getMessage());
    }

    private void handleSignOut() {
        this.signOutRequest.onReply();
    }

    private static String CreateGoogleSignInRequestNonce() {
        try {
            var rawNonce = UUID.randomUUID().toString();
            var bytes = rawNonce.getBytes();
            var md = MessageDigest.getInstance("SHA-256");
            var digest = md.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("com.oali.game", e.getMessage());
        }
        return null;
    }
}
