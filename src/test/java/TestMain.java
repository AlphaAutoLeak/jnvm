import com.alphaautoleak.jnvm.Main;

public class TestMain {

    public static void main(String[] args) {
        Main.main(new String[]{
                "--jar", "test/demo.jar",
                "--out", "test/demo-protected.jar",
                "--target", "x86_64-windows-gnu",
                "--anti-debug", "true",
                "--native-dir", "native",
                "--debug", "false"
        });
    }

}
