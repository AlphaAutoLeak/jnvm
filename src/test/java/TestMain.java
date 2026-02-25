import com.alphaautoleak.jnvm.Main;

public class TestMain {

    public static void main(String[] args) {
        Main.main(new String[]{
                "--jar", "demo.jar",
                "--out", "demo-protected.jar",
                "--target", "x86_64-windows-gnu",
                "--anti-debug", "true",   // 调试时关掉
                "--native-dir", "native",
                "--debug", "true"
        });
    }

}
