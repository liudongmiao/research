### 标题：Java 中自动装箱与拆箱探究
### 作者：子非
### 原文：https://github.com/liudongmiao/research/blob/master/java-autobox/autobox.md
### 简介：本文通过反编译 Java 代码，探究自动装箱与拆箱过程；同时更进一步，描述基本类型 == 意义。

Java 1.5 引入了自动装箱（Autoboxing）与拆箱（Unboxing）的概念。事实上，装箱（Boxing）与拆箱源自 C#，虽然 C# 可以认为是微软向 Java 致敬。

C# 自 2002 年 1 月发布 1.0 起，就支持装箱与拆箱（与 Java 中意义不同）；而 Java 自 2002 年 12 月起，就开始考虑支持装箱，这些定义在 [JSR 201](https://www.jcp.org/en/jsr/detail?id=201) 中。虽然 C# 发布 1.0 以后 1 个月后，Java 就发布了 1.4，但是 Java 直到 2004 年 9 月才发布 1.5 开始支持自动装箱与拆箱。

# Java 中自动装箱只是语法糖

也正因为如此，Java 中的自动装箱与拆箱更像一个语法糖。先看代码：

```java
public class Boxing {

    Integer box(int i) {
        return i;
    }

    int unbox(Integer i) {
        return i;
    }

    boolean equals(Integer i, int j) {
        return i == j;
    }

}
```

在 IDEA 中，如果查看编译之后的文件，会是这样子：

```java
public class Boxing {

    public Boxing() {
    }

    Integer box(int i) {
        return Integer.valueOf(i);
    }

    int unbox(Integer i) {
        return i.intValue();
    }

    boolean equals(Integer i, int j) {
        return i.intValue() == j;
    }

}
```

从上可以看出，基本类型`int`向包装类`Integer`转换时，使用`valueOf`；而`Integer`向`int`转换时，使用`intValue`。事实上，在所有的基本类型与包装类转换过程中，都是使用类似的手法：

| 基本类型 | 包装类 | 自动装箱，自 1.5 起 | 拆箱 |
| ------- | ----- | ------- | --- |
| boolean | Boolean | valueOf(boolean) | booleanValue() |
| byte | Byte | valueOf(byte) | byteValue() |
| char | Character | valueOf(char) | charValue() |
| float | Float | valueOf(float) | floatValue() |
| int | Integer | valueOf(int) | intValue() |
| long | Long | valueOf(long) | longValue() |
| short | Short | valueOf(short) | shortValue() |
| double | Double | valueOf(double) | doubleValue() |

注意：所有包装类，都是`final`的，无法扩展；同时，在拆箱过程中，可能会出现空指针异常`NullPointerException`。

# 装箱在 C# 与 Java 中意义不同

虽然 C# 有装箱与拆箱概念，但意义完全不一样。C# 中的装箱差不多是这个意思：

```java
Integer i = Integer.valueOf("42");
Object o = i;
```

而 C# 的拆箱差不多是这个意思：

```java
Object o = 42;
Integer i = (Integer) o;
```

而这些，在 Java 中，不过只是普通的类型转换，因为除了基本类型外，一切皆对象。而在 C# 中，没有 Java 所谓的基本类型，即使写着`double`，也不过是`struct Double`的一个别名，这才是真正意义上的一切皆对象。

# valueOf 中的缓存范围及自动装箱可能的异常

自此，我们看到了 Java 1.5 中引入的自动装箱，实际上都是`valueOf`。根据 Java 语言规范（目前 Oracle 官方仅提供 1.6 及后续版本），对于特定范围内的基本类型，在装箱后的引用应该一样（以下简称为“缓存”）。语言规范规定：

| 基本类型 | 包装类 | 规范缓存要求 | Oracle 实现 |
| ------- | ----- | --- | ---- |
| boolean | Boolean | 所有 | 一致 |
| byte | Byte | 所有 | 一致 |
| char | Character | [\u0000, \u007f] | 一致 |
| float | Float | 无 | 一致 |
| int | Integer | [-128, 127] | 基本一致，上限可变<sup>`*`</sup> |
| long | Long | 无 | [-128, 127] |
| short | Short | [-128, 127] | 一致 |
| double | Double | 无 | 一致 |

<sup>`*`</sup> 对于`Integer`中自动装箱值一样的上限，可以通过`-XX:AutoBoxCacheMax=<size>`来指定。由于实现采用一个数组，而数组大小理论上限是`0x7fffffff`(Integer.MAX_VALUE)，可以算出理论最大上限。但实际上，由于要预先分配内存，所以实际上限与内存相关，但肯定超过`127`。

值得注意的是，由于`valueOf`绝大部分值都不缓存，会生成对象，在生成对象过程中，可能会出现内存不足`OutOfMemoryError`。

在实际中，也不要装箱后使用`==`比较两个对象的引用，因为这种应用场景是不合理的。

# `==` 到底是啥

一般理解的意义，`==`是比较引用，但是对于基本类型，本身就不是一个对象，`==`是什么呢？我们还是可以看反编译后的结果，这时，神器 IDEA 也看不明白，只能使用`javac`查看了。

```java
public class Equals {

    boolean equals(boolean a, boolean b) {
        return a == b;
    }

}
```

生成`class`以后，使用`javac -c`查看，大致类似：

```
  boolean equals(boolean, boolean);
    Code:
       0: iload_1
       1: iload_2
       2: if_icmpne     9
       5: iconst_1
       6: goto          10
       9: iconst_0
      10: ireturn
```

这里无意解释`jvm`各种指令，仅关心真正有含义的`if_icmpne`就好，以下是基本类型及对象使用`==`中关键指令：

| 类型 | `jvm`指令 | 解释 |
| --- | --- | --- |
| boolean | if_icmpne | 两个整数是否相等, 同 int |
| byte | if_icmpne | 两个整数是否相等, 同 int |
| char | if_icmpne | 两个整数是否相等, 同 int |
| double | dcmpl | 比较两个双精度数，分别返回 0, 1, -1；如果为 NaN，返回 1 |
| float | fcmpl | 比较两个单精度数，分别返回 0, 1, -1；如果为 NaN，返回 1 |
| int | if_icmpne | 两个整数是否相等  |
| long | lcmp | 比较两个长整数，分别返回 0, 1, -1 |
| short | if_icmpne | 两个整数是否相等, 同 int |
| Object | if_acmpne | 比较两个对象引用 |
| 数组 | if_acmpne | 比较两个对象引用 |

通过直接的`jvm`指令，我们可以看到，对于浮点数而言，`NaN != NaN`。而至于`NaN != NaN`，可以理解为是规范，因为`NaN`有多种表达方式。有关浮点数的存储中可能的坑，可以阅读鄙人的[《0.1 + 0.2你算对了吗？》](http://mp.weixin.qq.com/s?__biz=MzAwOTE0ODEwMQ==&mid=2650686196&idx=1&sn=281521f872016b49fca2d1b4d7c9e737)。而在这个过程中，IDEA 有一个 bug：

```java
double d = Double.NaN;
Double da = Double.NaN;
Double db = Double.NaN;
boolean ba = d == d; // false
boolean bb = da == da; // true (idea提示false)
boolean bc = da == db; // false
boolean bd = d == da; // false
```