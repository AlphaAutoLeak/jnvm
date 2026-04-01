import com.alphaautoleak.jnvm.Main;

final class SampleProtectionRunner {

    private SampleProtectionRunner() {
    }

    static void run(String configPath) {
        Main.main(new String[]{configPath});
    }
}
