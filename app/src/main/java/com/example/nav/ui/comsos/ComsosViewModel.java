package com.example.nav.ui.comsos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ComsosViewModel extends ViewModel {

    private final MutableLiveData<String> mTitleText;
    private final MutableLiveData<String> mScheduleText;

    public ComsosViewModel() {
        mTitleText = new MutableLiveData<>();
        mTitleText.setValue("Командир-панель");

        mScheduleText = new MutableLiveData<>();
        mScheduleText.setValue("Расписание бронирований");
    }

    public LiveData<String> getTitleText() {
        return mTitleText;
    }

    public LiveData<String> getScheduleText() {
        return mScheduleText;
    }
}