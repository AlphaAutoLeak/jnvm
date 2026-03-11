# benchmark

# Java-ObfuscatorTest

[benchmarks](https://github.com/huzpsb/JavaObfuscatorTest)

![1](https://github.com/AlphaAutoLeak/jnvm/blob/master/imgs/perf1.png)

| Obfuscator                        | Test#1  | Test#2   | Performance | Size |
|-----------------------------------|---------|----------|-------------|------|
| [None](https://www.java.com/#LOL) | PPPPPPP | PPPPPPPP | 27ms        | 29KB |
| [jnvm](./)                        | PPPPPPP | FPPPPEPP | 560ms       | 60KB | 
| [jnvm-string](./)                 | PPPPPPP | FPPPPEPP | 590ms       | 64KB | 

# evaluator

```
PS F:\JNVM\samples\demo> java -jar .\demo-obf.jar
Today's date is 2026-03-10
Performing small int test...
10 * 5 = 50
3 / 15 = 0
5 ^ 11 = 14
10 ^ 11 = 1
4 - 13 = -9
1 ^ 4 = 5
12 + 5 = 17
14 + 15 = 29
10 + 18 = 28
12 / 3 = 4
10 % 6 = 4
15 / 10 = 1
5 * 18 = 90
8 * 19 = 152
12 * 4 = 48
11 % 8 = 3
10 ^ 7 = 13
10 ^ 5 = 15
18 - 17 = 1
9 / 20 = 0

Performing random math operations...
Performing double test -783234008. Operation name is *
10.736554500849767 * 0.74113233949422 = 7.957207755321985
Performing int test -783234008. Operation name is ^
259 ^ 923 = 664
Performing int test -783234008. Operation name is -
530 - 923 = -393
Performing double test -783234008. Operation name is /
2.7573095161824757 / 1.1028629585647993 = 2.500137931707014
Performing int test -783234008. Operation name is ^
259 ^ 923 = 664
Performing int test -783234008. Operation name is +
530 + 892 = 1422
Performing int test -783234008. Operation name is *
472 * 472 = 222784
Performing double test -783234008. Operation name is -
3.535287429360438 - 1.1028629585647993 = 2.432424470795639
Performing int test -783234008. Operation name is ^
552 ^ 636 = 84
Performing double test -783234008. Operation name is %
0.21480131492236743 % 5.932633085882487 = 0.21480131492236743
Performing int test -783234008. Operation name is %
450 % 769 = 450
Performing int test -783234008. Operation name is /
988 / 769 = 1
Performing double test -783234008. Operation name is /
8.683696317015794 / 1.5007304056520934 = 5.786313307380867
Performing int test -783234008. Operation name is +
988 + 432 = 1420
Performing int test -783234008. Operation name is %
432 % 51 = 24
Performing double test -783234008. Operation name is %
15.846849796405586 % 11.312333434915566 = 4.53451636149002
Performing int test -783234008. Operation name is -
784 - 636 = 148
Performing double test -783234008. Operation name is %
9.624071224255548 % 1.1028629585647993 = 0.8011675557371536
Performing double test -783234008. Operation name is /
9.79012425459241 / 0.74113233949422 = 13.209684334208927
Performing double test -783234008. Operation name is *
1.9817656768587806 * 11.312333434915566 = 22.41839412649766
Performing int test -783234008. Operation name is ^
450 ^ 472 = 26

Computing statistics
Averages for (i, d): 7117.5, 6.650516350895737Loaded 2 tests
Testing annotations
Test, 0.36, 36

Original Text: Hello World
AES Key (Hex Form): 460A19D14339B096A4E4CC22EC65B210
Encrypted Text (Hex Form): 028E0B4E25DFFA66849A99A86962FB1A
Decrypted Text: Hello World

Testing cryptography (Blowfish)
Testing large string
Successfully compared strings
Successfully decrypted hello world 123 1605479835458
Used 107ms
```