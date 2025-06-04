package com.example.nav.ui.comsos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nav.databinding.FragmentComsosBinding;

public class ComsosFragment extends Fragment {

    private FragmentComsosBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ComsosViewModel comsosViewModel =
                new ViewModelProvider(this).get(ComsosViewModel.class);

        binding = FragmentComsosBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView titleTextView = binding.textComsosTitle;
        comsosViewModel.getTitleText().observe(getViewLifecycleOwner(), titleTextView::setText);

        final TextView scheduleTextView = binding.textBookingsSchedule;
        comsosViewModel.getScheduleText().observe(getViewLifecycleOwner(), scheduleTextView::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}