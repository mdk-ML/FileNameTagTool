package cn.mdkml.filenametagtool.util;

import cn.mdkml.filenametagtool.model.SearchResult;
import com.sun.jna.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything 搜索工具类
 * 依赖官方 Everything32.dll/Everything64.dll
 * 要求：Everything客户端必须在后台运行
 * Version 2 is for Everything 1.4
 */
public class EverythingUtil {

    private static final int EVERYTHING_REQUEST_FILE_NAME = 0x00000001;
    private static final int EVERYTHING_REQUEST_PATH = 0x00000002;
    private static final int EVERYTHING_REQUEST_SIZE = 0x00000010;
    private static final int EVERYTHING_REQUEST_DATE_MODIFIED = 0x00000020;
    private static final int EVERYTHING_REQUEST_FLAGS =
            EVERYTHING_REQUEST_FILE_NAME | EVERYTHING_REQUEST_PATH | EVERYTHING_REQUEST_SIZE | EVERYTHING_REQUEST_DATE_MODIFIED;

    // 定义DLL接口
    private interface EverythingDll extends Library {
        EverythingDll INSTANCE = Native.load("Everything64", EverythingDll.class);

        // 设置搜索关键词
        void Everything_SetSearchW(WString search);

        // 设置是否匹配大小写
        void Everything_SetMatchCase(boolean matchCase);

        // 设置是否匹配整个单词
        void Everything_SetMatchWholeWord(boolean matchWholeWord);

        // 设置是否匹配路径
        void Everything_SetMatchPath(boolean matchPath);

        // 设置是否启用正则表达式
        void Everything_SetRegex(boolean regex);

        // 设置偏移量（用于分页）
        void Everything_SetOffset(int offset);

        // 设置请求的属性标志
        void Everything_SetRequestFlags(int flags);

        // 执行搜索
        boolean Everything_QueryW(boolean wait);

        // 获取搜索结果总数
        int Everything_GetNumResults();

        // 获取结果总数（不受maxResults限制）
        int Everything_GetTotalResults();

        // 获取文件名（Unicode）
        WString Everything_GetResultFileNameW(int index);

        // 获取文件路径（Unicode）
        WString Everything_GetResultPathW(int index);

        // 获取文件大小（通过指针返回）
        boolean Everything_GetResultSize(int index, long[] size);

        // 获取文件修改时间（FILETIME格式，通过指针返回）
        boolean Everything_GetResultDateModified(int index, Pointer fileTime);

        // 判断是否为文件夹
        boolean Everything_IsFolderResult(int index);

        // 判断是否为文件
        boolean Everything_IsFileResult(int index);

        // 重置搜索状态
        void Everything_Reset();

        // 获取最后错误代码
        int Everything_GetLastError();
    }

    // 单例模式
    private static final EverythingUtil INSTANCE = new EverythingUtil();

    /** DLL 是否加载成功 */
    private final boolean loaded;

    private EverythingUtil() {
        boolean success;
        try {
            EverythingDll.INSTANCE.Everything_GetLastError();
            success = true;
        } catch (Throwable e) {
            throw new RuntimeException("加载 Everything DLL 失败，请确保 Everything32.dll/Everything64.dll 在项目根目录", e);
        }
        this.loaded = success;
    }

    public static EverythingUtil getInstance() {
        return INSTANCE;
    }

    /**
     * 基础搜索方法
     *
     * @param query          搜索关键词（支持Everything原生语法）
     * @param matchCase      是否区分大小写
     * @param matchWholeWord 是否匹配整个单词
     * @param matchPath      是否匹配路径
     * @param useRegex       是否使用正则表达式
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query,
                                     boolean matchCase, boolean matchWholeWord,
                                     boolean matchPath, boolean useRegex) {
        List<SearchResult> results = new ArrayList<>();

        if (!loaded) {
            return results;
        }

        try {
            // 重置之前的搜索状态
            EverythingDll.INSTANCE.Everything_Reset();

            // 设置搜索参数
            EverythingDll.INSTANCE.Everything_SetSearchW(new WString(query));
            EverythingDll.INSTANCE.Everything_SetMatchCase(matchCase);
            EverythingDll.INSTANCE.Everything_SetMatchWholeWord(matchWholeWord);
            EverythingDll.INSTANCE.Everything_SetMatchPath(matchPath);
            EverythingDll.INSTANCE.Everything_SetRegex(useRegex);
//            EverythingDll.INSTANCE.Everything_SetRequestFlags(EVERYTHING_REQUEST_FLAGS);

            // 执行搜索（等待完成）
            boolean success = EverythingDll.INSTANCE.Everything_QueryW(true);
            if (!success) {
                int errorCode = EverythingDll.INSTANCE.Everything_GetLastError();
                throw new RuntimeException("Everything 搜索失败，错误代码: " + errorCode);
            }

            // 获取结果数量
            int numResults = EverythingDll.INSTANCE.Everything_GetNumResults();

            // 遍历结果
            for (int i = 0; i < numResults; i++) {
                String path = EverythingDll.INSTANCE.Everything_GetResultPathW(i).toString();
                String name = EverythingDll.INSTANCE.Everything_GetResultFileNameW(i).toString();
                String fullPath = path + File.separator + name;

//                long[] sizeArr = new long[1];
//                EverythingDll.INSTANCE.Everything_GetResultSize(i, sizeArr);
//                long size = sizeArr[0];
//
//                Memory ftMem = new Memory(8);
//                EverythingDll.INSTANCE.Everything_GetResultDateModified(i, ftMem);
//                long fileTime = ftMem.getLong(0);
//                long lastModified = (fileTime / 10000) - 11644473600000L;

                boolean isDirectory = EverythingDll.INSTANCE.Everything_IsFolderResult(i);

                results.add(new SearchResult(fullPath, name, isDirectory));
            }

        } finally {
            // 确保重置搜索状态
            EverythingDll.INSTANCE.Everything_Reset();
        }

        return results;
    }

    // ==================== 常用快捷方法 ====================

    /**
     * 简单搜索（默认参数）
     */
    public List<SearchResult> search(String query) {
        return search(query, false, false, false, false);
    }

    /**
     * 带路径的搜索
     *
     * @param query 搜索关键词（支持Everything原生语法）
     * @param path  搜索路径（如 "C:\projects"）
     */
    public List<SearchResult> search(String query, String path) {
        String fullQuery = (path != null ? path + " " : "") + query;
        return search(fullQuery, false, false, false, false);
    }

    /**
     * 搜索指定类型的文件
     *
     * @param extension 文件扩展名（如 "java", "pdf", "txt"）
     * @param path      搜索路径（如 "C:\projects"）
     */
    public List<SearchResult> searchByExtension(String extension, String path) {
        String query = (path != null ? path + " " : "") + "*" + extension;
        return search(query);
    }

    /**
     * 搜索文件夹
     */
    public List<SearchResult> searchFolders(String query) {
        return search("folder:" + query);
    }

    /**
     * 搜索文件（排除文件夹）
     */
    public List<SearchResult> searchFiles(String query) {
        return search("file:" + query);
    }

    /**
     * 正则表达式搜索
     */
    public List<SearchResult> searchRegex(String regex) {
        return search(regex, false, false, false, true);
    }

    /**
     * 检查Everything是否正在运行
     */
    public boolean isEverythingRunning() {
        if (!loaded) {
            return false;
        }
        try {
            EverythingDll.INSTANCE.Everything_GetLastError();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // 测试方法
    public static void main(String[] args) {
        EverythingUtil searcher = EverythingUtil.getInstance();

        if (!searcher.isEverythingRunning()) {
            System.err.println("错误：Everything客户端未运行，请先启动Everything");
            return;
        }

//        System.out.println("=== 测试1：简单搜索 *.java 文件 ===");
//        List<SearchResult> results0 = searcher.search("*.java");
//        for (SearchResult result : results0) {
//            System.out.println(result);
//        }

        List<SearchResult> results1 = searcher.search("【 】", "C:\\Users\\MU\\Desktop\\【测试】FileNameTagTool");
        for (SearchResult result : results1) {
            System.out.println(result);
            System.out.println("======");
            System.out.println(result.getFileName());
        }
//        System.out.println("\n=== 测试2：搜索指定路径下的 PDF 文件 ===");
//        List<SearchResult> results2 = searcher.searchByExtension("pdf", "C:\\Users\\Public\\Documents");
//        for (SearchResult result : results2) {
//            System.out.println(result);
//        }
//
//        System.out.println("\n=== 测试3：搜索文件夹 ===");
//        List<SearchResult> results3 = searcher.searchFolders("Downloads");
//        for (SearchResult result : results3) {
//            System.out.println(result);
//        }
    }

    public static void launchEverythingUI(String query, String everythingPath) {
        try {
            new ProcessBuilder(everythingPath, "-search", query).start();
        } catch (Exception e) {
            throw new RuntimeException("启动 Everything 失败：" + e.getMessage(), e);
        }
    }
}
