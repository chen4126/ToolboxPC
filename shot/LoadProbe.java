package com.toolbox.pc;

import java.util.List;

/** 探针：原样打印 todo.txt 每行，确认测试数据是否完整落盘 */
public class LoadProbe {
    public static void main(String[] a) {
        List<String> lines = Store.readLines("todo.txt");
        System.out.println("lines=" + lines.size());
        for (String s : lines) System.out.println("[" + s.replace("\t", "\\t") + "]");
    }
}
