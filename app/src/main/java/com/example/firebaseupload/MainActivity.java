package com.example.firebaseupload;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity {


    private static boolean mainCondition = false;
    ViewPagerAdapter viewPagerAdapter;
    //    @BindView(R.id.vpApp)
    private static ViewPager vpApp;
    private static boolean slidable = true;
    private static boolean original = true;
    private static boolean ACTIVITY_START_STATE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ButterKnife.bind(this);
        vpApp = findViewById(R.id.vpApp);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpApp.setAdapter(viewPagerAdapter);
        openHomeScreen();

        ACTIVITY_START_STATE = true;


    }

    public static void openHomeScreen() {
        vpApp.setCurrentItem(1);
    }

    public static boolean getLanguage() {
        return original;
    }

    public static void setLanguage(boolean state) {
        original = state;
    }

    public static void setCondition(boolean condition) {
        mainCondition = condition;

    }

    public static boolean getCondition() {
        return mainCondition;

    }

    public static boolean getActivityStart() {
        return ACTIVITY_START_STATE;
    }

    public static void setActivityStart(boolean state) {
        ACTIVITY_START_STATE = state;
    }


}



