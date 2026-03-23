import java.util.concurrent.atomic.AtomicReference;

public class GameState {
    public enum State{
        PLAYING,
        WAITING,
        OVER,
        TERMINATED
    }

    private static final AtomicReference<State> state = new AtomicReference<State>(State.WAITING);

    public static void setState(State newValue){
        state.set(newValue);
    }

    public static State getState(){
        return state.get();
    }
}
