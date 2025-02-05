package com.example.bluetoothchat;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Main");
                            break;
                        case 1:
                            tab.setText("Empty 1");
                            break;
                        case 2:
                            tab.setText("Empty 2");
                            break;
                    }
                }).attach();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_main) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_empty1) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.navigation_empty2) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });
    }
}
