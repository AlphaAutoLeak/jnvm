import com.alphaautoleak.jnvm.Main;

public class TestZKM {

    public static void main(String[] args) {
        Main.main(new String[]{
                "--jar", "SnakeGame-zkm.jar",
                "--out", "SnakeGame-zkm-obf.jar",
                "--target", "x86_64-windows-gnu",
                "--anti-debug", "true",
                "--native-dir", "native",
                "--debug", "false"
        });
    }

}
