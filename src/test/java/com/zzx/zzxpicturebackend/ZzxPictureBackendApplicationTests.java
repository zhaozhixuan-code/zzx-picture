package com.zzx.zzxpicturebackend;

import java.util.ArrayList;
import java.util.List;

// @SpringBootTest
class ZzxPictureBackendApplicationTests {

    static List<List<Integer>> allPath;
    static int[][] g;
    static List<Integer> arr;
    static int n;
    public static void main(String[] args) {
         int[][] graph = {{1,3}, {2}, {3}, {}};
        List<List<Integer>> lists = allPathsSourceTarget(graph);
        System.out.println(lists);

    }
    public static List<List<Integer>> allPathsSourceTarget(int[][] graph) {
        // 一共有 n 个节点
        n = graph.length;
        g = new int[n][n];
        allPath = new ArrayList<>();
        arr = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < graph[i].length; j++) {
                g[i][graph[i][j]] = 1;
            }
        }

        for (int j = 0; j < g[0].length; j++) {
            arr.clear();
            arr.add(0);
            if (g[0][j] == 1) {
                arr.add(j);
                dfs(j);
            }
        }
        return allPath;
    }
    public static void dfs(int index) {
        if (index == n - 1) {
            allPath.add(new ArrayList<>(arr));
            return;
        }

        for (int i = 0; i < n; i++) {
            if (g[index][i] == 1) {
                arr.add(i);
                dfs(i);
                arr.remove(arr.size() - 1);
            }
        }

    }
}
