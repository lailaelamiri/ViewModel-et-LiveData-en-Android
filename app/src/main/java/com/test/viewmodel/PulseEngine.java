package com.test.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class PulseEngine extends ViewModel {

    private static final String VAULT_KEY = "frozen_wave";
    private final MutableLiveData<Integer> heartbeat;
    private final SavedStateHandle vault;

    public PulseEngine(SavedStateHandle vault) {
        this.vault = vault;
        Integer frozen = vault.get(VAULT_KEY);
        heartbeat = new MutableLiveData<>(frozen != null ? frozen : 0);
    }

    public void amplify() {
        Integer tick = heartbeat.getValue();
        if (tick != null) {
            int next = tick + 1;
            heartbeat.setValue(next);
            vault.set(VAULT_KEY, next);
        }
    }

    public void dampen() {
        Integer tick = heartbeat.getValue();
        if (tick != null) {
            int next = tick - 1;
            heartbeat.setValue(next);
            vault.set(VAULT_KEY, next);
        }
    }

    public void flatline() {
        heartbeat.setValue(0);
        vault.set(VAULT_KEY, 0);
    }

    // Bonus 1 — postValue safe depuis n'importe quel thread
    public void amplifyFromShadow() {
        new Thread(() -> {
            Integer tick = heartbeat.getValue();
            if (tick != null) {
                int next = tick + 1;
                heartbeat.postValue(next);      // thread-safe
                vault.set(VAULT_KEY, next);
            }
        }).start();
    }

    public LiveData<Integer> getSignal() {
        return heartbeat;
    }
}