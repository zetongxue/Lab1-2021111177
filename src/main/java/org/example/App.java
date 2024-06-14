package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;



public class App extends Application {
    private static final Logger logger = LogManager.getLogger(App.class);
    // 绘图参数
    private static final int GRAPH_BORDER = 30;          // 边界
    private static final int GRAPH_RADIUS = 350;        // 分布的大圆的半径
    private static final Position GRAPH_CENTRAL = new Position(GRAPH_BORDER + GRAPH_RADIUS, GRAPH_BORDER + GRAPH_RADIUS);
    private static final int NODE_RADIUS = 30;          // 节点的半径
    private static final int ARROW_LENGTH = 10;         // 箭头的长度

    // 必要的变量
    private String filePath = null;                     // 读取的文件路径
    private boolean isLoaded = false;                   // 判断当前是否已经读取了一个文件
    private boolean isGraphDrew = false;                // 判断当前是否已经绘制了图
    private HashMap<String, Integer> word2id = null;    // word -> int的映射
    private String[] id2word = null;                    // int -> word的映射
    private AdjacencyList graph = null;                 // 邻接表表示的图
    private Thread myThread;                            // 随机游走线程
    private final Random random = new Random();         // 随机数生成器
    private Pane pneGraph = null;                       // 图
    private int minPathLength = 0;                      // 最小路径长度
    private int[][] minPath = null;                     // 路径
    private int[][] minMap = null;                      // 邻接矩阵

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static HashMap<String, Integer> encodeWords(String[] wordList) {
        // 将单词列表进行编码，构建词典
        AtomicInteger count = new AtomicInteger();
        HashMap<String, Integer> wordDict = new HashMap<>();
        for (String word : wordList) {
            wordDict.computeIfAbsent(word, k -> count.getAndIncrement());
        }
        return wordDict;
    }

    private static String[] getReversedWordDict(HashMap<String, Integer> wordDict) {
        // 创建一个数组，将哈希表的映射反过来
        String[] reversedWordDict = new String[wordDict.size()];
        String key;
        int value;
        for (Map.Entry<String, Integer> entry : wordDict.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            reversedWordDict[value] = key;
        }
        return reversedWordDict;
    }

    private static AdjacencyList buildGraph(String[] wordList, HashMap<String, Integer> wordDict) {
        // 根据单词列表和词典构建图
        int n = wordDict.size();
        int m = wordList.length;
        AdjacencyList graph = new AdjacencyList(n);
        for (int i = 0; i < m - 1; i++) {
            graph.addEdge(wordDict.get(wordList[i]), wordDict.get(wordList[i + 1]));
        }
        return graph;
    }

    @Override
    public void start(Stage stage) {
        // GUI
        stage.setTitle("App");
        // 按钮
        Label lblCurrentFunction = new Label("当前功能：加载文件");
        Button btnLoadFile = new Button("加载文件");
        Button btnShowGraph = new Button("展示有向图");
        Button btnQueryBridgeWords = new Button("查询桥接词");
        Button btnGenerateNewWord = new Button("根据桥接词生成新文本");
        Button btnShortestPath = new Button("计算两个词之间的最短路径");
        Button btnRandomWalk = new Button("随机游走");

        // 创建右侧不同功能对应的窗体
        // 加载文件
        GridPane pneLoadFile = new GridPane();
        pneLoadFile.setPadding(new Insets(10));
        FileChooser fileChooserTxt = new FileChooser();
        fileChooserTxt.setTitle("选择文件");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooserTxt.getExtensionFilters().add(txtFilter);
        Button btnFileChooser = new Button("选择文件");
        pneLoadFile.add(btnFileChooser, 0, 0);
        Label lblLoadFile = new Label("当前未加载文件");
        pneLoadFile.add(lblLoadFile, 0, 1);

        // 展示有向图
        GridPane pneShowGraph = new GridPane();
        pneShowGraph.setPadding(new Insets(10));
        Button btnSaveImage = new Button("保存图片");
        Button btnRestoreImage = new Button("还原图片");
        pneGraph = new Pane();
        btnFileChooser.setOnAction(e -> {
            loadFile(stage, fileChooserTxt);
            if (this.isLoaded) {
                lblLoadFile.setText("当前已加载文件：" + this.filePath);
                pneGraph.getChildren().clear();
            }
        });
        FileChooser fileChooserImage = new FileChooser();
        fileChooserImage.setTitle("保存图片");
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooserImage.setInitialFileName("graph.png");
        fileChooserImage.getExtensionFilters().add(imageFilter);
        btnSaveImage.setOnAction(e -> saveImage(stage, pneGraph, fileChooserImage));
        btnRestoreImage.setOnAction(e -> showDirectedGraph(this.graph, this.pneGraph, null));
        pneShowGraph.add(btnSaveImage, 0, 0);
        pneShowGraph.add(btnRestoreImage, 1, 0);
        pneShowGraph.add(pneGraph, 0, 1);
        GridPane.setColumnSpan(pneGraph, 2);

        // 查询桥接词
        GridPane pneQueryBridgeWords = getPneQueryBridgeWords();
        // 根据桥接词生成新文本
        GridPane pneGenerateNewWord = getPneGenerateNewWord();
        // 计算两个词之间的最短路径
        GridPane pneShortestPath = getPneShortestPath();
        // 随机游走
        GridPane pneRandomWalk = getPneRandomWalk(stage);

        // 创建主布局
        GridPane root = new GridPane();
        root.setPadding(new Insets(10));
        root.setVgap(10);
        root.setHgap(10);
        VBox buttonsVBox = new VBox(10); // 设置垂直间距
        buttonsVBox.getChildren().addAll(lblCurrentFunction, btnLoadFile, btnShowGraph, btnQueryBridgeWords, btnGenerateNewWord, btnShortestPath, btnRandomWalk);
        root.add(buttonsVBox, 0, 0);
        VBox right = new VBox(10);
        right.getChildren().add(pneLoadFile);
        root.add(right, 1, 0);

        // 将按钮绑定窗体变换事件
        btnLoadFile.setOnAction(e -> changePane(right, pneLoadFile, false, lblCurrentFunction, btnLoadFile.getText()));
        btnShowGraph.setOnAction(e -> {
            changePane(right, pneShowGraph, true, lblCurrentFunction, btnShowGraph.getText());
            if (!this.isGraphDrew) {
                showDirectedGraph(this.graph, this.pneGraph, null);
            }
        });
        btnQueryBridgeWords.setOnAction(e -> changePane(right, pneQueryBridgeWords, true, lblCurrentFunction, btnQueryBridgeWords.getText()));
        btnGenerateNewWord.setOnAction(e -> changePane(right, pneGenerateNewWord, true, lblCurrentFunction, btnGenerateNewWord.getText()));
        btnShortestPath.setOnAction(e -> changePane(right, pneShortestPath, true, lblCurrentFunction, btnShortestPath.getText()));
        btnRandomWalk.setOnAction(e -> changePane(right, pneRandomWalk, true, lblCurrentFunction, btnRandomWalk.getText()));

        stage.setScene(new Scene(root, 1000, 800));
        stage.setTitle("WordLadder");
        stage.setMaximized(true);
        stage.show();
    }
    /* 以下几个函数与界面的搭建有关，最后一个是界面的切换 */
    private GridPane getPneRandomWalk(Stage stage) {
        GridPane pneRandomWalk = new GridPane();
        pneRandomWalk.setPadding(new Insets(10));
        Button btnRandomWalkExecute = new Button("执行");
        Button btnRandomWalkInterrupt = new Button("中断");
        Button btnRandomWalkSaveResult = new Button("保存结果");
        TextField txtRandomWalkResult = new TextField();
        txtRandomWalkResult.setPrefSize(800, 20);
        FileChooser fileSaveTxt = new FileChooser();
        fileSaveTxt.setTitle("保存文件");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileSaveTxt.getExtensionFilters().add(txtFilter);
        fileSaveTxt.setInitialFileName("random walk");
        btnRandomWalkExecute.setOnAction(e -> {
            String result = randomWalk(txtRandomWalkResult);
            txtRandomWalkResult.setText(result);
        });
        btnRandomWalkInterrupt.setOnAction(e -> myThread.interrupt());
        btnRandomWalkSaveResult.setOnAction(e -> saveFile(txtRandomWalkResult.getText(), stage, fileSaveTxt));
        pneRandomWalk.add(btnRandomWalkExecute, 0, 0);
        pneRandomWalk.add(btnRandomWalkInterrupt, 1, 0);
        pneRandomWalk.add(btnRandomWalkSaveResult, 2, 0);
        pneRandomWalk.add(txtRandomWalkResult, 0, 1);
        GridPane.setColumnSpan(txtRandomWalkResult, 3);
        return pneRandomWalk;
    }

    private GridPane getPneGenerateNewWord() {
        GridPane pneGenerateNewWord = new GridPane();
        pneGenerateNewWord.setPadding(new Insets(10));
        Label lblGenerateNewWordWordInput = new Label("输入文本：");
        TextField txtGenerateNewWordWordInput = new TextField();
        txtGenerateNewWordWordInput.setPrefSize(800, 20);
        Button btnGenerateNewWordExecute = new Button("生成");
        TextField txtGenerateNewWordResult = new TextField();
        txtGenerateNewWordResult.setPrefSize(800, 20);
        btnGenerateNewWordExecute.setOnAction(e -> {
            String wordInput = txtGenerateNewWordWordInput.getText().toLowerCase();
            String wordResult = generateNewText(wordInput);
            txtGenerateNewWordResult.setText(wordResult);
        });
        pneGenerateNewWord.add(lblGenerateNewWordWordInput, 0, 0);
        pneGenerateNewWord.add(txtGenerateNewWordWordInput, 1, 0);
        pneGenerateNewWord.add(btnGenerateNewWordExecute, 0, 1);
        pneGenerateNewWord.add(txtGenerateNewWordResult, 1, 1);
        return pneGenerateNewWord;
    }

    private GridPane getPneShortestPath() {
        GridPane pneShortestPath = new GridPane();
        pneShortestPath.setPadding(new Insets(10));
        Label lblShortestPathWord1 = new Label("词1：");
        Label lblShortestPathWord2 = new Label("词2：");
        TextField txtShortestPathWord1 = new TextField();
        txtShortestPathWord1.setPrefSize(800, 20);
        TextField txtShortestPathWord2 = new TextField();
        txtShortestPathWord2.setPrefSize(800, 20);
        Button btnShortestPathExecute = new Button("计算");
        TextField txtShortestPathResult = new TextField();
        txtShortestPathResult.setPrefSize(800, 20);
        Label lblShortestPathLength = new Label("最短路径长度：");
        TextField txtShortestPathLength = new TextField();
        txtShortestPathLength.setPrefSize(800, 20);
        btnShortestPathExecute.setOnAction(e -> {
            String word1 = txtShortestPathWord1.getText().toLowerCase();
            String word2 = txtShortestPathWord2.getText().toLowerCase();
            String result = calcShortestPath(word1, word2);
            txtShortestPathResult.setText(result);
            if (this.minPathLength != 0 && result != null){
                txtShortestPathLength.setText(String.valueOf(this.minPathLength));
            } else {
                txtShortestPathLength.setText("");
            }
        });
        pneShortestPath.add(lblShortestPathWord1, 0, 0);
        pneShortestPath.add(txtShortestPathWord1, 1, 0);
        pneShortestPath.add(lblShortestPathWord2, 0, 1);
        pneShortestPath.add(txtShortestPathWord2, 1, 1);
        pneShortestPath.add(btnShortestPathExecute, 0, 2);
        pneShortestPath.add(txtShortestPathResult, 1, 2);
        pneShortestPath.add(lblShortestPathLength, 0, 3);
        pneShortestPath.add(txtShortestPathLength, 1, 3);
        return pneShortestPath;
    }

    private GridPane getPneQueryBridgeWords() {
        GridPane pneQueryBridgeWords = new GridPane();
        pneQueryBridgeWords.setPadding(new Insets(10));
        Label lblQueryBridgeWordsWord1 = new Label("词1：");
        Label lblQueryBridgeWordsWord2 = new Label("词2：");
        TextField txtQueryBridgeWordsWord1 = new TextField();
        TextField txtQueryBridgeWordsWord2 = new TextField();
        Button btnQueryBridgeWordsExecute = new Button("查询");
        TextField txtQueryBridgeWordsResult = new TextField();
        txtQueryBridgeWordsResult.setPrefSize(800, 20);
        btnQueryBridgeWordsExecute.setOnAction(e -> {
            String word1 = txtQueryBridgeWordsWord1.getText().toLowerCase();
            String word2 = txtQueryBridgeWordsWord2.getText().toLowerCase();
            String result = queryBridgeWords(word1, word2);
            txtQueryBridgeWordsResult.setText(result);
        });
        pneQueryBridgeWords.add(lblQueryBridgeWordsWord1, 0, 0);
        pneQueryBridgeWords.add(txtQueryBridgeWordsWord1, 1, 0);
        pneQueryBridgeWords.add(lblQueryBridgeWordsWord2, 0, 1);
        pneQueryBridgeWords.add(txtQueryBridgeWordsWord2, 1, 1);
        pneQueryBridgeWords.add(btnQueryBridgeWordsExecute, 0, 2);
        pneQueryBridgeWords.add(txtQueryBridgeWordsResult, 1, 2);
        return pneQueryBridgeWords;
    }

    private void changePane(VBox right, Pane targetPane, boolean check, Label lblShow, String wordShow) {
        // 使用按钮切换功能时，页面的变化
        if (check && !isLoaded) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("请先加载文件！");
            alert.showAndWait();
            return;
        }
        right.getChildren().clear();
        right.getChildren().add(targetPane);
        lblShow.setText("当前功能：" + wordShow);
    }

    /* 随机游走算法，涉及线程 */
    private String randomWalk(TextField result) {
        /* 随机游走（启动线程） */
        myThread = new Thread(new RandomWalkThread(this.word2id, this.id2word, this.graph.graph, result));
        myThread.start();
        return null;
    }

    /* 计算最短路径，第一个是主函数，剩下的是相关计算 */
    private String calcShortestPath(String word1, String word2) {
        /* 计算最短路径 */
        int id1;
        int id2;
        try{
            id1 = word2id.get(word1);
            id2 = word2id.get(word2);
        } catch (Exception e){
            return null;
        }
        if (this.minMap == null){
            calcShortestPathMatrix();
        }
        int max = Integer.MAX_VALUE;        // 别Integer.max 两个相加越界为负
        int min = this.minMap[id1][id2];
        if (min == max) {
            return null;
        } else {
            this.minPathLength = min;
            List<Integer> pathIndexList = getPathIndex(this.minPath, word2id.get(word1), word2id.get(word2));
            showDirectedGraph(this.graph, this.pneGraph, pathIndexList);
            return getPathString(pathIndexList);
        }
    }
    private void calcShortestPathMatrix(){
        /* 最短路径矩阵只需要计算一次 */
        int size = this.graph.size;
        this.minPath = newMatrix(size, -1);
        this.minMap = adj2Matrix();
        int temp;
        for (int k = 0; k < size; k++)
        {
            for (int i = 0; i < size; i++)
            {
                for (int j = 0; j < size; j++)
                {
                    temp = safeAdd(this.minMap[i][k], this.minMap[k][j]);
                    if (this.minMap[i][j] > temp) {
                        this.minMap[i][j] = temp;
                        minPath[i][j] = k;
                    }
                }
            }
        }
    }

    private void getPathIndexRecursive(int[][] path, int u, int v, List<Integer> pathIndexList){
        int mid = path[u][v];
        if (mid == -1){
            pathIndexList.add(v);
        } else {
            getPathIndexRecursive(path, u, mid, pathIndexList);
            getPathIndexRecursive(path, mid, v, pathIndexList);
        }
    }

    private List<Integer> getPathIndex(int[][] path, int u, int v){
        /* 获取最短路径上的点 */
        List<Integer> pathIndexList = new ArrayList<>();
        pathIndexList.add(u);
        int mid = path[u][v];
        if (mid == -1){
            pathIndexList.add(v);
        } else {
            getPathIndexRecursive(path, u, mid, pathIndexList);
            getPathIndexRecursive(path, mid, v, pathIndexList);
        }
        return pathIndexList;
    }

    private int[][] adj2Matrix() {
        /* 将邻接表转换为邻接矩阵 */
        int[][] matrix = newMatrix(this.graph.size, Integer.MAX_VALUE);
        for (int i = 0; i < this.graph.size; i++) {
            LinkedList<Edge> head = this.graph.graph.get(i);
            for (Edge edge : head) {
                matrix[i][edge.destination] = edge.weight;
            }
        }
        return matrix;
    }

    private int[][] newMatrix(int size, int initValue){
        /* 根据指定值初始化一个矩阵 */
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                matrix[i][j] = initValue;
            }
        }
        return matrix;
    }

    private String getPathString(List<Integer> pathIndexList) {
        /* 得到最短路径的文字显示 */
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pathIndexList.size() - 1; i++) {
            result.append(id2word[pathIndexList.get(i)]).append("->");
        }
        result.append(id2word[pathIndexList.get(pathIndexList.size() - 1)]);
        return result.toString();
    }

    private int safeAdd(int a, int b){
        /* 安全加法，当一个数与最大值加时，返回最大值 */
        if (a == Integer.MAX_VALUE || b == Integer.MAX_VALUE){
            return Integer.MAX_VALUE;
        } else {
            return a + b;
        }
    }

    /* 生成新文本函数 */
    private String generateNewText(String inputText) {
        /* 生成新文本 */
        String[] words = inputText.split("\\s");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length - 1; i++) {
            if (!(word2id.containsKey(words[i]) && word2id.containsKey(words[i + 1]))) {
                result.append(words[i]).append(" ");
                continue;
            }
            LinkedList<Edge> head = graph.graph.get(word2id.get(words[i]));
            List<Integer> resultId = new ArrayList<>();
            for (Edge edge : head) {
                int r = graph.findEdge(edge.destination, word2id.get(words[i + 1]));
                if (r != 0)
                    resultId.add(edge.destination);
            }
            result.append(words[i]).append(" ");
            if (!resultId.isEmpty()) {
                int pickId = this.random.nextInt(resultId.size());
                result.append(id2word[resultId.get(pickId)]).append(" ");
            }
        }
        result.append(words[words.length - 1]);
        return result.toString();
    }

    /* 查询桥接词 */
    public String queryBridgeWords(String word1, String word2) {
        /* 查询桥接词 */
        // 获取两个单词的id
        int id1 = 0;
        int id2 = 0;
        int errorNo = 0;
        try {
            id1 = word2id.get(word1);
        } catch (Exception e){
            errorNo = 1;
        }
        try {
            id2 = word2id.get(word2);
        } catch (Exception e){
            if (errorNo == 1){
                errorNo = 3;
            } else {
                errorNo = 2;
            }
        }
        String errorInfo = null;
        switch (errorNo){
            case 1:
                errorInfo = "No \"" + word1 + "\" in the graph!";
                break;
            case 2:
                errorInfo = "No \"" + word2 + "\" in the graph!";
                break;
            case 3:
                errorInfo = "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
                break;
            case 0:
            default:
                break;
        }
        if (errorNo != 0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText(errorInfo);
            alert.showAndWait();
            return null;
        }
        // 获取桥接词id
        List<Integer> resultId = new ArrayList<>();
        for (Edge edge : graph.graph.get(id1)) {
            int r = graph.findEdge(edge.destination, id2);
            if (r != 0)
                resultId.add(edge.destination);
        }
        StringBuilder resultText = new StringBuilder("The bridge words from \"" + word1 + "\" to \""+ word2 + "\" ");
        // 根据桥接词内容生成返回文本
        if (resultId.isEmpty()) {
            // 如果没有，则报错，返回null
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("No bridge words from \"" + word1 + "\" to \"" + word2 + "\" !");
            alert.showAndWait();
            return null;
        } else if (resultId.size() == 1) {
            // 有1个单词
            resultText.append("is: ").append(id2word[resultId.get(0)]).append(".");
        } else {
            // 有多个单词
            resultText.append("are: ");
            for (int re : resultId.subList(0, resultId.size() - 1)) {
                resultText.append(id2word[re]).append(", ");
            }
            resultText.append("and ").append(id2word[resultId.get(resultId.size() - 1)]).append(".");
        }
        return resultText.toString();
    }

    /* 以下7个函数与绘图有关，第一个是绘图函数，剩下的是相关的计算函数 */
    private void showDirectedGraph(AdjacencyList graph, Pane pane, List<Integer> marked) {
        /* 实现展示有向图的功能 */
        // 把当前pane中的所有子节点清空
        pane.getChildren().clear();
        // 先计算全部节点的位置
        Position[] positions = calcNodePositions(graph.size);
        Position start;
        Position end;
        // 先绘制边
        for (int i = 0; i < graph.size; i++) {
            for (Edge edge : graph.graph.get(i)) {
                start = positions[i];
                end = positions[edge.destination];
                Position[] arrowPosition = calcArrowPosition(start, end);
                Line mainLine = new Line(arrowPosition[0].x(), arrowPosition[0].y(), arrowPosition[1].x(), arrowPosition[1].y());
                Line arrowLineL = new Line(arrowPosition[1].x(), arrowPosition[1].y(), arrowPosition[2].x(), arrowPosition[2].y());
                Line arrowLineR = new Line(arrowPosition[1].x(), arrowPosition[1].y(), arrowPosition[3].x(), arrowPosition[3].y());
                Label lblWeight = new Label(String.valueOf(edge.weight));
                lblWeight.setTranslateX(arrowPosition[0].x() + (arrowPosition[1].x() - arrowPosition[0].x()) * 0.3);
                lblWeight.setTranslateY(arrowPosition[0].y() + (arrowPosition[1].y() - arrowPosition[0].y()) * 0.3);
                if (isEdgeInList(marked, i, edge.destination)){
                    mainLine.setStroke(Color.RED);
                    arrowLineL.setStroke(Color.RED);
                    arrowLineR.setStroke(Color.RED);
                } else {
                    mainLine.setStroke(Color.BLACK);
                    arrowLineL.setStroke(Color.BLACK);
                    arrowLineR.setStroke(Color.BLACK);
                }
                pane.getChildren().addAll(mainLine, arrowLineL, arrowLineR, lblWeight);
            }
        }
        String word;
        // 绘制全部节点
        for (int i = 0; i < graph.size; i++) {
            Position pos = calcNodePosition(i);
            Circle circle = new Circle(pos.x(), pos.y(), NODE_RADIUS);
            if (isNodeInList(marked, i)) {
                circle.setFill(Color.YELLOW);
            } else {
                circle.setFill(Color.WHITE);
            }
            word = id2word[i];
            circle.setStroke(Color.BLACK);
            Tooltip tooltip = new Tooltip(word);
            Tooltip.install(circle, tooltip);
            pane.getChildren().add(circle);

            Label label = new Label(word);
            label.setTranslateX(pos.x() - (double) NODE_RADIUS / 2);
            label.setTranslateY(pos.y() - (double) NODE_RADIUS / 4);
            pane.getChildren().add(label);
        }
        this.isGraphDrew = true;
    }

    private Position calcNodePosition(int id) {
        // 在显示有向图时，通过id来计算一个节点应当显示在的位置
        double angle = id * 2 * Math.PI / graph.size;
        return new Position(GRAPH_CENTRAL.x() + (int) (GRAPH_RADIUS * Math.cos(angle)), GRAPH_CENTRAL.y() + (int) (GRAPH_RADIUS * Math.sin(angle)));
    }

    private Position[] calcNodePositions(int maxVal) {
        // 计算全部节点的位置
        Position[] positions = new Position[maxVal];
        for (int i = 0; i < maxVal; i++) {
            positions[i] = calcNodePosition(i);
        }
        return positions;
    }

    private Position[] calcArrowPosition(Position start, Position end) {
        /* 在绘图时计算箭头线的坐标 */
        // return: start, end, arrowL, arrowR [start-end, end-arrowL, end-arrowR]，
        double angle = Math.atan2((end.y() - start.y()), (end.x() - start.x()));
        double distance = calcNodeDistance(start, end);
        Position newStart = new Position(
                start.x() + (int) (NODE_RADIUS / distance * (end.x() - start.x())),
                start.y() + (int) (NODE_RADIUS / distance * (end.y() - start.y()))
        );
        Position newEnd = new Position(
                end.x() + (int) (NODE_RADIUS / distance * (start.x() - end.x())),
                end.y() + (int) (NODE_RADIUS / distance * (start.y() - end.y()))
        );
        Position arrowL = new Position(
                newEnd.x() - (int) (ARROW_LENGTH * Math.cos(angle + Math.PI / 6)),
                newEnd.y() - (int) (ARROW_LENGTH * Math.sin(angle + Math.PI / 6))
        );
        Position arrowR = new Position(
                newEnd.x() - (int) (ARROW_LENGTH * Math.cos(angle - Math.PI / 6)),
                newEnd.y() - (int) (ARROW_LENGTH * Math.sin(angle - Math.PI / 6))
        );
        return new Position[]{newStart, newEnd, arrowL, arrowR};
    }

    private double calcNodeDistance(Position start, Position end) {
        // 计算图上两个节点之间的距离
        return Math.sqrt(Math.pow((end.x() - start.x()), 2) + Math.pow((end.y() - start.y()), 2));
    }

    private boolean isEdgeInList(List<Integer> integers, int start, int end){
        // 判断一个边是否在list中
        if (integers == null) return false;
        for (int i = 0; i < integers.size() - 1; i++){
            if (integers.get(i) == start && integers.get(i + 1) == end){
                return true;
            }
        }
        return false;
    }

    private boolean isNodeInList(List<Integer> integers, int node){
        // 判断一个节点是否在list中
        if (integers == null) return false;
        return integers.contains(node);
    }

    /* 保存图片 */
    private void saveImage(Stage stage, Pane pane, FileChooser fileChooser) {
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;
        WritableImage image = new WritableImage((int) pane.getWidth(), (int) pane.getHeight());
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        pane.snapshot(params, image);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }

    /* 加载文件的全过程 */
    private void loadFile(Stage stage, FileChooser fileChooser) {
        // 加载文件，并计算得到必要数据
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) return;
        this.filePath = selectedFile.getAbsolutePath();
        String[] wordList = readFile(this.filePath);
        this.word2id = encodeWords(wordList);
        this.id2word = getReversedWordDict(this.word2id);
        this.graph = buildGraph(wordList, this.word2id);
        this.isLoaded = true;
        this.isGraphDrew = false;
    }

    private String[] readFile(String fileName) {
        // 读取文件，返回单词列表
        StringBuilder sb = new StringBuilder();
        try (FileReader fileReader = new FileReader(fileName);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // 使用正则表达式替换非字母字符为空格，并替换为小写
                String cleanedLine = line.replaceAll("[^a-zA-Z]+", " ").toLowerCase();
                sb.append(cleanedLine).append(" ");
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        return sb.toString().split("\\s+");
    }

    private void saveFile(String string, Stage stage, FileChooser fileChooser){
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
            writer.write(string);
        } catch (IOException e) {
            logger.error("保存文件时出现错误: {}", e.getMessage());
        }
    }
}

record Position(int x, int y) {
}


