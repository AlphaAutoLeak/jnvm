import com.alphaautoleak.jnvm.Main;

public class TestSnake {

    public static void main(String[] args) {
        Main.main(new String[]{
                "--jar", "SnakeGame.jar",
                "--out", "SnakeGame-obf.jar",
                "--target", "x86_64-windows-gnu",
                "--anti-debug", "true",
                "--native-dir", "native",
                "--debug", "false"
        });
    }

}
