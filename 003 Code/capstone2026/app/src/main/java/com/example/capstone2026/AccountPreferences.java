package com.example.capstone2026;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class AccountPreferences {
    private AccountPreferences() { }

    public static SharedPreferences open(Context context, String category) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String suffix = user == null ? "guest" : "user_" + user.getUid();
        // Legacy device-wide data has no owner; do not assign it to an arbitrary account.
        return context.getSharedPreferences(category + "_" + suffix, Context.MODE_PRIVATE);
    }
}
