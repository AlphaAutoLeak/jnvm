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
Today's date is 2026-03-11
Performing small int test...
14 % 1 = 0
2 + 10 = 12
12 ^ 6 = 10
20 / 12 = 1
16 ^ 8 = 24
10 - 4 = 6

Performing random math operations...
Performing double test 0. Operation name is *
5.932633085882487 * 3.535287429360438 = 20.97356317152818
Performing int test 1. Operation name is +
892 + 893 = 1785
Performing int test 2. Operation name is ^
636 ^ 409 = 997
Performing double test 3. Operation name is %
0.21480131492236743 % 9.79012425459241 = 0.21480131492236743
Performing double test 4. Operation name is +
9.862795945665074 + 3.535287429360438 = 13.39808337502551
Performing int test 5. Operation name is *
409 * 106 = 43354
Performing int test 6. Operation name is /
957 / 259 = 3
Performing int test 7. Operation name is ^
892 ^ 784 = 108
Performing double test 8. Operation name is +
0.2805255257633217 + 3.535287429360438 = 3.81581295512376
Performing double test 9. Operation name is *
0.2805255257633217 * 8.683696317015794 = 2.435998474899876
Performing double test 10. Operation name is *
1.5007304056520934 * 0.74113233949422 = 1.1122398364910457
Performing int test 11. Operation name is +
74 + 432 = 506
Performing int test 12. Operation name is -
106 - 259 = -153
Performing double test 13. Operation name is /
0.2805255257633217 / 15.846849796405586 = 0.017702289689585562
Performing double test 14. Operation name is +
5.932633085882487 + 8.683696317015794 = 14.61632940289828
Performing double test 15. Operation name is *
9.862795945665074 * 10.736554500849767 = 105.89244620139318
Performing double test 16. Operation name is %
9.79012425459241 % 0.21480131492236743 = 0.12406508308587572
Performing double test 17. Operation name is *
8.683696317015794 * 8.683696317015794 = 75.40658172615368
Performing double test 18. Operation name is *
0.2805255257633217 * 2.422188626186955 = 0.6794857378590334

Computing statistics
Averages for (i, d): 3588.6923076923076, 19.8905924640892Loaded 2 tests
Testing annotations
Test, 0.36, 36

Original Text: Hello World
AES Key (Hex Form): 488D7B00A5A982FFEA0BAB94CB30C14A
Encrypted Text (Hex Form): 7D0C59B699E187AC12CADAC6D04C007C
Decrypted Text: Hello World

Testing cryptography (Blowfish)
Testing large string
Successfully compared strings
Successfully decrypted hello world 123 1605479835458
Used 82ms
```