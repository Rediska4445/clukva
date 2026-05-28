package rf.ebanina.utils.concurrency;

public class ThreadInterruptedException extends InterruptedException {
    public ThreadInterruptedException() {
    }

    public ThreadInterruptedException(String s) {
        super(s);
    }
}
