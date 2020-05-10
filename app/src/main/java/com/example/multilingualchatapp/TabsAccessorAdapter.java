package com.example.multilingualchatapp;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class TabsAccessorAdapter extends FragmentPagerAdapter {

    private String translationOption;

    TabsAccessorAdapter(FragmentManager fm, String translationOption) {
        super(fm);
        this.translationOption = translationOption;
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                //passing the translationOption variable, which indicates if the user has the translation
                //feature on or off, to know whether to translate the last messages between the user
                //and his contacts that are displayed in the ChatFragment
                return new ChatsFragment(translationOption);
            case 1:
                return new ContactsFragment();
            case 2:
                return new RequestsFragment();
            default:
                return null;
        }
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
                return "Chats";
            case 1:
                return "Contacts";
            case 2:
                return "Requests";

            default:
                return null;
        }
    }
}
