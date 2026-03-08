package com.cloudnest.app;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import java.util.Collections;

/**
 * Central helper for Google Authentication and Drive API Credentials.
 * Simplifies token acquisition for UploadWorkers and API service builders.
 */
public class GoogleAuthHelper {

    /**
     * Gets the current GoogleSignInClient for authentication management.
     */
    public static GoogleSignInClient getSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE), 
                               new Scope(DriveScopes.DRIVE_METADATA), 
                               new Scope(DriveScopes.DRIVE_READONLY))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    /**
     * Retrieves the GoogleAccountCredential object required to build the Drive service.
     * @param context The current context.
     * @param account The signed-in Google account.
     * @return Initialized GoogleAccountCredential.
     */
    public static GoogleAccountCredential getCredential(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());
        return credential;
    }

    /**
     * Checks if a user is currently signed in.
     */
    public static boolean isSignedIn(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context) != null;
    }

    /**
     * Helper to sign the user out.
     */
    public static void signOut(Context context) {
        getSignInClient(context).signOut();
    }

    /**
     * Validates if the currently signed-in account has all required Drive permissions.
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) return false;

        Scope[] requiredScopes = new Scope[]{
                new Scope(DriveScopes.DRIVE_FILE),
                new Scope(DriveScopes.DRIVE_METADATA),
                new Scope(DriveScopes.DRIVE_READONLY)
        };
        
        return GoogleSignIn.hasPermissions(account, requiredScopes);
    }
}