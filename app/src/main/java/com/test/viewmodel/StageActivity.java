package com.test.viewmodel;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

public class StageActivity extends AppCompatActivity {

    private PulseEngine engine;
    private TextView orbDisplay;
    private Button boostBtn, drainBtn, voidBtn, shadowBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arena_board);

        orbDisplay = findViewById(R.id.orbDisplay);
        boostBtn   = findViewById(R.id.boostBtn);
        drainBtn   = findViewById(R.id.drainBtn);
        voidBtn    = findViewById(R.id.voidBtn);
        shadowBtn  = findViewById(R.id.shadowBtn);

        // SavedStateHandle → survit même au process death
        engine = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this)
        ).get(PulseEngine.class);

        // Observer lifecycle-aware → zéro leak
        engine.getSignal().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer wave) {
                orbDisplay.setText(String.valueOf(wave));
            }
        });

        boostBtn.setOnClickListener(v  -> engine.amplify());
        drainBtn.setOnClickListener(v  -> engine.dampen());
        voidBtn.setOnClickListener(v   -> engine.flatline());
        shadowBtn.setOnClickListener(v -> engine.amplifyFromShadow()); // Bonus 1
    }
}