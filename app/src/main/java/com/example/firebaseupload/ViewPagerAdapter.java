package com.example.firebaseupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentPagerAdapter implements IAdapter {
    private Fragment fragment;
    private String title;
    private List<ITabbedFragment> fragments;
//    private IAdapter mListener;

    ViewPagerAdapter(@NonNull FragmentManager fm, int behaviour) {
        super(fm, behaviour);

        fragments = new ArrayList<>();
        fragments.add(CameraFragment.newInstance(this));
        fragments.add(HomeScreen.newInstance(this));
        fragments.add(SettingsFragment.newInstance(this));


    }

    @NonNull
    @Override
    public Fragment getItem(int position) {

        return (Fragment) fragments.get(position);
//        fragment = new Fragment();
//        switch (position) {
//            case 0:
//                fragment = new CameraFragment();
//                break;
//            case 1:
//                fragment = new HomeScreen();
//                break;
//            case 2:
//                fragment = new SettingsFragment();
//                break;
//
//        }
//        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {

        switch (position) {
            case 0:
                title = "Camera";
                break;
            case 1:
                title = "VIRTUAL EYE";
                break;
            case 2:
                title = "Settings";
                break;
        }

        return title;
    }


    @Override
    public void onSend(Object o, Fragment fragment) {
        for (ITabbedFragment f : fragments) {
            if (!f.equals(fragment)) f.onReceive(o);
        }
    }
}
