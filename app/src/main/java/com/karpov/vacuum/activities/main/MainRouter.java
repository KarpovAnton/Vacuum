package com.karpov.vacuum.activities.main;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.karpov.vacuum.R;
import com.karpov.vacuum.activities.account.AccountActivity;
import com.karpov.vacuum.fragments.ProfileFragment;
import com.karpov.vacuum.network.data.dto.ProfilePreviewDto;

import javax.inject.Inject;

public class MainRouter implements MainContract.Router {
    @Inject
    MainActivity activity;

    @Inject
    public MainRouter() {

    }

    public void openProfile(ProfilePreviewDto profileDto) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.configure(profileDto);
        replaceFragment(fragment);
    }

    @Override
    public void removeFragment() {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
            if (fragment != null)
                activity.getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public void openAccountActivity() {
        Intent mainIntent = new Intent(activity.getApplicationContext(), AccountActivity.class);
        activity.startActivity(mainIntent);
    }

    void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.profileContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
