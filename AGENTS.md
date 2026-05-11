# OpenCode Agent Rules

本文件定义了AI辅助编码时代理应遵循的Java开发规范。

## 代码风格规范

### 1. 类设计规范
- **尽可能避免使用内部类，按功能拆分复杂需求**
  - 将复杂逻辑拆分为独立的类，提高代码可读性和可维护性
  - 每个类应职责单一，遵循单一职责原则（SRP）
  - 通过重构优化代码结构，而非使用内部类堆积逻辑
  - ✅ 推荐：
    ```java
    // 将内部类拆分为独立的类文件
    public class OrderProcessor {
        private final OrderValidator validator;
        private final OrderCalculator calculator;
        
        public OrderProcessor(OrderValidator validator, OrderCalculator calculator) {
            this.validator = validator;
            this.calculator = calculator;
        }
    }
    
    // 独立的验证类
    public class OrderValidator {
        public boolean validate(Order order) {
            // 验证逻辑
        }
    }
    
    // 独立的计算类
    public class OrderCalculator {
        public BigDecimal calculateTotal(Order order) {
            // 计算逻辑
        }
    }
    ```
  - ❌ 避免：
    ```java
    // 避免使用大量内部类堆积逻辑
    public class OrderProcessor {
        private class OrderValidator {
            // 内部类实现
        }
        
        private class OrderCalculator {
            // 内部类实现
        }
        
        private class OrderLogger {
            // 内部类实现
        }
    }
    ```
  - 例外情况：简单的回调接口、事件监听器等可使用Lambda或匿名内部类

### 2. 导入规范
- **变量、类等引用应尽量使用简写形式，避免完整包名**
  - ✅ 推荐：`Files.readString(path)`
  - ❌ 避免：`java.nio.file.Files.readString(path)`
  - 通过合理的import语句来简化代码阅读

### 3. 代码结构规范
- **if语句必须包含{}括号，即使只有一行代码**
  - ✅ 推荐：
    ```java
    if (condition) {
        doSomething();
    }
    ```
  - ❌ 避免：
    ```java
    if (condition) doSomething();
    ```

### 4. 大括号换行规范
- **方法声明后的大括号必须换行**
  - ✅ 推荐：
    ```java
    public int getIconHeight() {
        return 17;
    }
    ```
  - ❌ 避免：
    ```java
    public int getIconHeight() { return 17; }
    ```
  - 此规范同样适用于类声明、控制语句（if/for/while/switch等）
  - ✅ 推荐：
    ```java
    public class UserService {
        public void processUser(User user) {
            if (user != null) {
                for (Order order : user.getOrders()) {
                    processOrder(order);
                }
            }
        }
    }
    ```
  - ❌ 避免：
    ```java
    public class UserService {
        public void processUser(User user) {
            if (user != null) { for (Order order : user.getOrders()) { processOrder(order); } }
        }
    }
    ```
  - ✅ 推荐：
    ```java
    if (condition) {
        doSomething();
    }
    ```
  - ❌ 避免：
    ```java
    if (condition) doSomething();
    ```

## 注释规范

### 5. 方法注释
- **所有方法均应有JavaDoc注释**
  - 包含方法功能描述
  - 包含参数说明（@param）
  - 包含返回值说明（@return）
  - 包含异常说明（@throws），如适用
  - 示例：
    ```java
    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户唯一标识
     * @return 用户信息对象，未找到时返回null
     * @throws IllegalArgumentException 当userId为空时抛出
     */
    public User getUserById(String userId) {
        // 实现代码
    }
    ```

### 6. Swing/CS编程注释规范
- **涉及Swing等CS（客户端）编程的代码，尽可能每一行都加上注释**
  - 目的：帮助熟悉BS开发（Spring全家桶、HTML相关技术）的工程师理解CS编程模式
  - 注释应侧重于：Swing组件的用途、事件模型机制、布局管理器行为、线程调度（EDT）等CS特有概念
  - 适当对比BS与CS的差异，帮助建立认知映射（如：`JTable` 类似于前端的 `<table>`，`LayoutManager` 类似于CSS布局）
  - ✅ 推荐：
    ```java
    // 创建表格模型，类似于前端中定义表格的数据结构
    DefaultTableModel model = new DefaultTableModel();
    // 设置表头列名，对应HTML中的<th>标签
    model.setColumnIdentifiers(new String[]{"姓名", "年龄", "部门"});

    // 创建JTable组件并绑定数据模型，类似于前端将数据渲染到<table>元素
    JTable table = new JTable(model);
    // 设置表格行高，提升可读性
    table.setRowHeight(25);
    // 启用表格行选中模式，允许用户点击选中整行
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // 将表格放入滚动面板，Swing中大型组件通常需要 JScrollPane 包裹才能正常显示滚动条
    JScrollPane scrollPane = new JScrollPane(table);
    // 将滚动面板添加到主窗口，使用BorderLayout的CENTER区域自动填充剩余空间
    frame.add(scrollPane, BorderLayout.CENTER);
    ```
  - ❌ 避免：
    ```java
    DefaultTableModel model = new DefaultTableModel();
    model.setColumnIdentifiers(new String[]{"姓名", "年龄", "部门"});
    JTable table = new JTable(model);
    table.setRowHeight(25);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(table);
    frame.add(scrollPane, BorderLayout.CENTER);
    ```

### 7. 行内注释
- **复杂逻辑应补充行内注释**
  - 解释"为什么"而不是"做什么"
  - 对算法、业务规则、边界条件等进行说明
  - 示例：
    ```java
    // 使用滑动窗口算法，时间复杂度O(n)
    // 右指针扩展窗口，左指针收缩窗口以保持条件
    while (right < array.length) {
        window.add(array[right]);
        // 当窗口内元素超过k个时，移动左指针
        while (window.size() > k) {
            window.remove(array[left++]);
        }
        right++;
    }
    ```

## 语言规范

### 8. 模型输出语言
- **模型的所有思考、回复、解释均必须使用简体中文**
  - 包括：代码分析、方案说明、问题回答、注释说明等一切非代码文本输出
  - ✅ 推荐：`这个方法的作用是解析用户输入并返回格式化后的结果`
  - ❌ 避免：`This method parses user input and returns formatted results`
  - 技术术语可保留英文（如HTTP、JSON、API等），但上下文说明必须使用中文

### 9. 用户界面文本
- **所有面向用户的文本均使用简体中文**
  - 包括：错误消息、日志输出、API响应消息、UI标签等
  - ✅ 推荐：`throw new IllegalArgumentException("用户ID不能为空");`
  - ❌ 避免：`throw new IllegalArgumentException("User ID cannot be null");`
  - 技术术语可保留英文（如HTTP、JSON、API等）

## 参考规范

### 10. 阿里巴巴Java开发手册
- **尽可能遵守《阿里巴巴Java开发手册》**
  - 命名规范
  - 常量定义
  - 代码格式
  - OOP规约
  - 集合处理
  - 并发处理
  - 控制语句
  - 注释规约
  - 异常处理
  - 日志规约
  - 单元测试
  - 安全规约
  - MySQL数据库
  - 工程结构
  - 设计规约

## 示例代码

```java
package com.example.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件处理服务类
 */
public class FileService {

    /**
     * 读取文件内容并按行返回
     *
     * @param filePath 文件路径
     * @return 文件内容行列表
     * @throws IOException 当文件读取失败时抛出
     */
    public List<String> readLines(String filePath) throws IOException {
        // 验证输入参数
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        Path path = Path.of(filePath);

        // 检查文件是否存在且可读
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 使用Java NIO读取所有行，自动处理字符编码
        return Files.readAllLines(path);
    }
}
```
