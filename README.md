# FileNameTagTool

一款基于 Java Swing 的文件名标签管理工具，支持批量添加、移除、重排文件名中的标签，帮助用户高效管理文件分类。

## 功能特性

- **批量添加标签**：为选中的文件批量添加自定义标签
- **批量移除标签**：从选中的文件中批量移除指定标签
- **标签顺序重排**：按照配置的全局顺序重新排列文件名中的标签
- **版本管理**：自动为文件创建新版本（如 V1 → V2）
- **无标签复制**：复制文件并移除所有标签
- **智能标签**：支持日期标签、历史标签等智能标签功能
- **Everything 集成**：集成 Everything 搜索引擎，快速查找带标签的文件
- **可视化管理**：提供图形化界面，支持拖拽排序标签顺序

## 系统要求

- **JDK 17+**
- **Maven 3.6+**（用于构建）
- **Everything 客户端**（可选，用于搜索功能）
- **Windows 操作系统**（依赖 Everything DLL）

## 构建

```bash
cd FileNameTagTool
mvn -q -DskipTests package
```

生成文件：`target/filename-tagtool-1.0.0.jar`

## 运行

### 命令行运行

```bash
javaw -jar target/filename-tagtool-1.0.0.jar <action> <file1> [file2] ...
```

### 支持的操作

| 操作 | 参数 | 说明 |
|------|------|------|
| 添加标签 | `add` | 打开标签管理窗口，自动切换到添加标签页 |
| 移除标签 | `remove` | 打开标签管理窗口，自动切换到移除标签页 |
| 移除所有标签 | `removeAll` | 移除文件名中的所有标签 |
| 创建新版本 | `newVersion` | 创建文件的新版本（V1 → V2） |
| 无标签复制 | `copyWithoutTags` | 复制文件并移除所有标签 |
| 标签管理 | `manage` | 打开标签管理窗口 |

### 示例

```bash
# 添加标签
javaw -jar target/filename-tagtool-1.0.0.jar add "C:\path\to\file.txt"

# 移除标签
javaw -jar target/filename-tagtool-1.0.0.jar remove "C:\path\to\file.txt"

# 批量操作
javaw -jar target/filename-tagtool-1.0.0.jar add "C:\path\to\file1.txt" "C:\path\to\file2.txt"
```

## 右键菜单配置

### 注册表配置示例

```reg
Windows Registry Editor Version 5.00

; 添加标签
[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\AddTag]
@="添加标签"

[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\AddTag\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\path\\to\\filename-tagtool-1.0.0.jar\" add \"%1\""

; 移除标签
[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\RemoveTag]
@="移除标签"

[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\RemoveTag\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\path\\to\\filename-tagtool-1.0.0.jar\" remove \"%1\""

; 移除所有标签
[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\RemoveAllTags]
@="移除所有标签"

[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\RemoveAllTags\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\path\\to\\filename-tagtool-1.0.0.jar\" removeAll \"%1\""

; 创建新版本
[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\NewVersion]
@="创建新版本"

[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\NewVersion\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\path\\to\\filename-tagtool-1.0.0.jar\" newVersion \"%1\""

; 标签管理
[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\Manage]
@="标签管理"

[HKEY_CURRENT_USER\Software\Classes\*\shell\FileNameTagTool\shell\Manage\command]
@="\"C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe\" -jar \"C:\\path\\to\\filename-tagtool-1.0.0.jar\" manage \"%1\""
```

## 配置文件

配置文件位置：`~/.filenametagtool/filename-tagtool.conf`

### 配置项说明

```properties
# 窗口位置和大小
window.x=0
window.y=0
window.w=0
window.h=0

# 分割线位置
ui.divider=0

# 标签管理窗口位置和大小
groupTagsWindowX=0
groupTagsWindowY=0
groupTagsWindowWidth=0
groupTagsWindowHeight=0

# Everything工具路径
everythingPath=C:/Program Files/Everything/Everything.exe

# 图标文件目录路径
iconPath=C:/Users/MU/Documents/FileNameTagTool/ico/

# 添加标签页分隔线位置
addTagTab.horizontalDivider=0
addTagTab.verticalDivider=0
addTagTab.smartHistoryDivider=0

# 标签列表（多个标签用逗号分隔）
tag={文件名},{版本号},{当前日期},工作,重要
```

## 标签语法

- 标签使用中文方括号包裹：`【标签名】`
- 版本号标签格式：`【V1】`、`【V2】` 等
- 日期标签格式：`【20260506】`（8位数字）
- 标签可以出现在文件名的任意位置

### 示例

```
【工作】文档.docx
【V2】【重要】报告.docx
项目计划【20260506】.docx
【紧急】【V3】需求文档.docx
```

## 项目结构

```
FileNameTagTool/
├── src/main/java/cn/mdkml/filenametagtool/
│   ├── FileNameTagTool.java          # 主类，程序入口
│   ├── FileNameTagToolTest.java      # 测试类
│   ├── component/
│   │   ├── BadgeToggleButton.java    # 带徽标的按钮组件
│   │   └── WrapLayout.java           # 自动换行布局管理器
│   ├── model/
│   │   ├── Action.java               # 操作类型枚举
│   │   ├── Config.java               # 配置模型
│   │   └── Parsed.java               # 命令行参数解析结果
│   └── util/
│       ├── ConfigUtil.java           # 配置工具类
│       ├── EverythingUtil.java       # Everything 搜索工具
│       ├── FileUtil.java             # 文件操作工具类
│       ├── SwingUtil.java            # Swing UI 工具类
│       └── TagUtil.java              # 标签工具类
├── pom.xml                           # Maven 配置文件
├── ico/                              # 图标资源
├── addRightMenu.reg                  # 右键菜单注册脚本
└── README.md                         # 项目说明文档
```

## 常见问题

### Q: Everything 搜索功能不工作？

A: 请确保：
1. Everything 客户端已启动
2. Everything DLL 文件在项目根目录
3. 配置文件中的 Everything 路径正确

### Q: 如何自定义标签顺序？

A: 打开标签管理窗口，在"设置"标签页中可以拖拽调整标签顺序，点击"保存"按钮保存配置。

### Q: 如何批量操作多个文件？

A: 在命令行中传入多个文件路径，或在右键菜单中选择多个文件后执行操作。

### Q: 标签支持哪些字符？

A: 标签支持任意字符，但建议使用中文、英文、数字等常见字符，避免使用特殊符号。

## 打包成 EXE

使用 `jpackage` 生成独立的可执行文件：

```bash
jpackage --input target/ --main-jar filename-tagtool-1.0.0.jar --name FileNameTagTool --type exe --main-class cn.mdkml.filenametagtool.FileNameTagTool
```

## 许可证

本项目仅供学习和个人使用。
