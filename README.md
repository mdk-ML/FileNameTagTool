# FileNameTagTool

Java（Swing）实现的"批量加前缀"右键工具：输入一次前缀，把所选文件/文件夹重命名为"前缀 + 原名"。

为解决多选时 Explorer 会多次启动命令的问题，本程序使用 **单实例（ServerSocket）+ 参数汇总**，因此只弹 **一个**输入框。

## 构建（Maven）

需要 JDK 17+ 与 Maven。

```bash
cd tools/FileNameTagTool
mvn -q -DskipTests package
```

生成：

- `tools/FileNameTagTool/target/filename-tagtool-1.0.0.jar`

## 运行（手动测试）

```bash
javaw -jar target/filename-tagtool-1.0.0.jar "C:\path\to\file.txt"
```

## 右键菜单注册表示例

注意：用 `javaw` 运行可以避免弹出控制台黑框。

```reg
Windows Registry Editor Version 5.00

[HKEY_CURRENT_USER\Software\Classes\*\shell\RenameTest\shell\MenuB]
@="MenuB - 批量加前缀(Java)"

[HKEY_CURRENT_USER\Software\Classes\*\shell\RenameTest\shell\MenuB\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\Users\\MU\\Documents\\extensions\\tools\\FileNameTagTool\\target\\filename-tagtool-1.0.0.jar\" add \"%1\""
```

如果你要"删除所有标签（移除文件名里所有 `[...]`）"，把 `add` 换成 `removeAll`：

```reg
[HKEY_CURRENT_USER\Software\Classes\*\shell\RenameTest\shell\MenuB\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\Users\\MU\\Documents\\extensions\\tools\\FileNameTagTool\\target\\filename-tagtool-1.0.0.jar\" removeAll \"%1\""
```

## 可选：打包成 exe（jpackage）

如果你希望目标机器无需单独配置命令（或更像"软件"），可以用 `jpackage` 生成 exe（需要 JDK 17+）。
具体参数可按你安装目录调整。