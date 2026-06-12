package net.jpalayoor.moneytracker.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import net.jpalayoor.moneytracker.R;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);

        root.findViewById(R.id.btnAboutBack).setOnClickListener(v ->
                Navigation.findNavController(root).popBackStack()
        );

        // handle email contact
        root.findViewById(R.id.tvEmail).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse(
                    "mailto:moneytracker@jpalayoor.com?subject=Money%20Tracker%20Feedback"));
            startActivity(Intent.createChooser(intent, "Send Email"));
        });

        return root;
    }
}