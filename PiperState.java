package pppp.g1;

public enum PiperState {
    AT_GATE,
    TO_GOAL,
    TO_GATE,
    UNLOAD {
        @Override
        public PiperState nextState() {
            return values()[0];
        }
    };

    public PiperState nextState() {
        return values()[ordinal() + 1];
    }
}
