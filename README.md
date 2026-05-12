# PulseEngine — ViewModel + LiveData Demo

Android Jetpack architecture demo showing the evolution from classic instance variables to ViewModel, LiveData, and SavedStateHandle.

---

## Demo Videos

### Video 1 — The Classic Bug (No ViewModel)
Counter resets to 0 on screen rotation. This is the problem every Android developer faced before Jetpack.



https://github.com/user-attachments/assets/97555a10-205c-45bc-9672-0ff257f6326b


### Video 2 — ViewModel + LiveData Fix
Counter survives rotation intact. `PulseEngine` lives outside the Activity lifecycle inside `ViewModelStore`.


https://github.com/user-attachments/assets/1b50ae0e-f1e2-4a83-8a26-e6cc114f3cc7


### Video 3 — SavedStateHandle Improvement
Counter survives after the app is swiped away from Recents. `SavedStateHandle` persists data across process death.


https://github.com/user-attachments/assets/ef059afc-2212-4cc3-846d-34ab7efa4175












---

## Project Structure

```
app/src/main/java/com/test/viewmodel/
├── StageActivity.java       # UI layer — observes LiveData, delegates logic to ViewModel
├── PulseEngine.java         # ViewModel — holds state, survives configuration changes

app/src/main/res/layout/
└── arena_board.xml          # Layout — orbDisplay, boostBtn, drainBtn, voidBtn, shadowBtn
```

---

## Architecture

```
StageActivity  (View)
      |  observe(getSignal())
      |  amplify() / dampen() / flatline()
      v
PulseEngine  (ViewModel)
      |  heartbeat : MutableLiveData<Integer>
      |  vault     : SavedStateHandle
      v
LiveData  ->  notifies UI automatically on value change
```

---

## Part 1 — Without ViewModel

```java
public class MainActivity extends AppCompatActivity {
    private int count = 0; // lost on rotation

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("count_key", count); // manual boilerplate, limited to primitives
    }
}
```

The Activity is destroyed and recreated on rotation. `count` resets to 0 unless manually saved via `onSaveInstanceState`, which cannot handle objects, threads, or LiveData.

---

## Part 2 — With ViewModel + LiveData

**PulseEngine.java**
```java
public class PulseEngine extends ViewModel {
    private final MutableLiveData<Integer> heartbeat;
    private final SavedStateHandle vault;

    public PulseEngine(SavedStateHandle vault) {
        this.vault = vault;
        Integer frozen = vault.get("frozen_wave");
        heartbeat = new MutableLiveData<>(frozen != null ? frozen : 0);
    }

    public void amplify() {
        Integer tick = heartbeat.getValue();
        if (tick != null) heartbeat.setValue(tick + 1);
    }

    public LiveData<Integer> getSignal() {
        return heartbeat;
    }
}
```

**StageActivity.java**
```java
engine = new ViewModelProvider(this).get(PulseEngine.class);

engine.getSignal().observe(this, wave -> {
    orbDisplay.setText(String.valueOf(wave));
});
```

The ViewModel lives in `ViewModelStore` and survives rotation. `observe()` is lifecycle-aware — the observer is removed automatically when the Activity is destroyed, preventing memory leaks. The new Activity instance reconnects to the same ViewModel automatically.

---

## Part 3 — SavedStateHandle

```java
public class PulseEngine extends ViewModel {
    private static final String VAULT_KEY = "frozen_wave";
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
}
```

**Factory setup in StageActivity:**
```java
engine = new ViewModelProvider(
    this,
    new SavedStateViewModelFactory(getApplication(), this)
).get(PulseEngine.class);
```

`SavedStateHandle` writes values into the system Bundle on every update. The value survives process death as long as the OS has time to call `onSaveInstanceState` before killing the process.

---

## Bonus — postValue from Background Thread

```java
public void amplifyFromShadow() {
    new Thread(() -> {
        Integer tick = heartbeat.getValue();
        if (tick != null) {
            heartbeat.postValue(tick + 1); // thread-safe
        }
    }).start();
}
```

`setValue()` must be called from the main thread.  
`postValue()` is safe from any background thread.

---

## Test Results

| Test | Result |
|------|--------|
| Screen rotation (Ctrl+F11) | Counter intact |
| Dark / light theme switch | Counter intact |
| Swipe app from Recents | Counter intact (SavedStateHandle) |
| `adb shell am force-stop` | Resets to 0 — expected, OS skips onSaveInstanceState |
| `observe()` commented out | UI stops updating — confirms LiveData is required |

---

## Survival Matrix

| Event | Raw variable | ViewModel | SavedStateHandle |
|-------|-------------|-----------|-----------------|
| Screen rotation | No | Yes | Yes |
| Home button | Yes | Yes | Yes |
| Swipe from Recents | No | No | Yes |
| System RAM kill | No | No | Yes |
| `force-stop` / ADB | No | No | No |

---

## Dependencies

```gradle
dependencies {
    implementation "androidx.lifecycle:lifecycle-viewmodel:2.7.0"
    implementation "androidx.lifecycle:lifecycle-livedata:2.7.0"
    implementation "androidx.lifecycle:lifecycle-viewmodel-savedstate:2.7.0"
}
```

---

## Key Concepts

| Concept | Description |
|---------|-------------|
| `ViewModel` | Survives configuration changes — rotation, theme switch |
| `MutableLiveData` | Observable, writable inside ViewModel only |
| `LiveData` | Read-only exposure to the UI layer |
| `observe()` | Lifecycle-aware subscription, auto-removed on destroy |
| `SavedStateHandle` | Persists across process death via Bundle mechanism |
| `postValue()` | Thread-safe LiveData update from background threads |

---

Built with Android Jetpack · API 30 · Pixel 4a Emulator
